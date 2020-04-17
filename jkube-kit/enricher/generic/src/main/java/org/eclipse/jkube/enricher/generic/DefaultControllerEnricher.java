/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.enricher.generic;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.openshift.api.model.DeploymentConfig;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.maven.enricher.api.BaseEnricher;
import org.eclipse.jkube.maven.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.maven.enricher.api.util.KubernetesResourceUtil;
import org.eclipse.jkube.maven.enricher.handler.DaemonSetHandler;
import org.eclipse.jkube.maven.enricher.handler.DeploymentConfigHandler;
import org.eclipse.jkube.maven.enricher.handler.DeploymentHandler;
import org.eclipse.jkube.maven.enricher.handler.HandlerHub;
import org.eclipse.jkube.maven.enricher.handler.JobHandler;
import org.eclipse.jkube.maven.enricher.handler.ReplicaSetHandler;
import org.eclipse.jkube.maven.enricher.handler.ReplicationControllerHandler;
import org.eclipse.jkube.maven.enricher.handler.StatefulSetHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Enrich with controller if not already present.
 *
 * By default the following objects will be added
 *
 * <ul>
 *     <li>ReplicationController</li>
 *     <li>ReplicaSet</li>
 *     <li>Deployment (for Kubernetes)</li>
 *     <li>DeploymentConfig (for OpenShift)</li>
 * </ul>
 *
 * TODO: There is a certain overlap with the ImageEnricher with adding default images etc.. This must be resolved.
 *
 * @author roland
 * @since 25/05/16
 */
public class DefaultControllerEnricher extends BaseEnricher {
    protected static final String[] POD_CONTROLLER_KINDS =
        { "ReplicationController", "ReplicaSet", "Deployment", "DeploymentConfig", "StatefulSet", "DaemonSet", "Job" };

    private final DeploymentHandler deployHandler;
    private final DeploymentConfigHandler deployConfigHandler;
    private final ReplicationControllerHandler rcHandler;
    private final ReplicaSetHandler rsHandler;
    private final StatefulSetHandler statefulSetHandler;
    private final DaemonSetHandler daemonSetHandler;
    private final JobHandler jobHandler;

    // Available configuration keys
    private enum Config implements Configs.Key {
        name,
        pullPolicy             {{ d = "IfNotPresent"; }},
        type                   {{ d = "deployment"; }},
        replicaCount           {{ d = "1"; }};

        public String def() { return d; } protected String d;
    }

    public DefaultControllerEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-controller");

        HandlerHub handlers = new HandlerHub(
            getContext().getGav(), getContext().getConfiguration().getProperties());
        rcHandler = handlers.getReplicationControllerHandler();
        rsHandler = handlers.getReplicaSetHandler();
        deployHandler = handlers.getDeploymentHandler();
        deployConfigHandler = handlers.getDeploymentConfigHandler();
        statefulSetHandler = handlers.getStatefulSetHandler();
        daemonSetHandler = handlers.getDaemonSetHandler();
        jobHandler = handlers.getJobHandler();
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        final String name = getConfig(Config.name, JKubeProjectUtil.createDefaultResourceName(getContext().getGav().getSanitizedArtifactId()));
        ResourceConfig xmlResourceConfig = getConfiguration().getResource().orElse(ResourceConfig.builder().build());
        ResourceConfig config = ResourceConfig.toBuilder(xmlResourceConfig)
                .controllerName(name)
                .imagePullPolicy(getImagePullPolicy(xmlResourceConfig, getConfig(Config.pullPolicy)))
                .replicas(getReplicaCount(builder, xmlResourceConfig, Configs.asInt(getConfig(Config.replicaCount))))
                .build();

        final List<ImageConfiguration> images = getImages().orElse(Collections.emptyList());

        // Check if at least a replica set is added. If not add a default one
        if (!KubernetesResourceUtil.checkForKind(builder, POD_CONTROLLER_KINDS)) {
            // At least one image must be present, otherwise the resulting config will be invalid
            if (!images.isEmpty()) {
                String type = getConfig(Config.type);
                if ("deployment".equalsIgnoreCase(type) || "deploymentConfig".equalsIgnoreCase(type)) {
                    if (platformMode == PlatformMode.kubernetes  || (platformMode == PlatformMode.openshift && useDeploymentForOpenShift())) {
                        log.info("Adding a default Deployment");
                        Deployment deployment = deployHandler.getDeployment(config, images);
                        builder.addToDeploymentItems(deployment);
                        setProcessingInstruction(FABRIC8_GENERATED_CONTAINERS, getContainersFromPodSpec(deployment.getSpec().getTemplate()));
                    } else {
                        log.info("Adding a default DeploymentConfig");
                        DeploymentConfig deploymentConfig = deployConfigHandler.getDeploymentConfig(config, images, getOpenshiftDeployTimeoutInSeconds(3600L), getValueFromConfig(IMAGE_CHANGE_TRIGGERS, true), getValueFromConfig(OPENSHIFT_ENABLE_AUTOMATIC_TRIGGER, true), isOpenShiftMode(), getProcessingInstructionViaKey(FABRIC8_GENERATED_CONTAINERS));
                        builder.addToDeploymentConfigItems(deploymentConfig);
                        setProcessingInstruction(FABRIC8_GENERATED_CONTAINERS, getContainersFromPodSpec(deploymentConfig.getSpec().getTemplate()));
                    }
                } else if ("statefulSet".equalsIgnoreCase(type)) {
                    log.info("Adding a default StatefulSet");
                    StatefulSet statefulSet = statefulSetHandler.getStatefulSet(config, images);
                    builder.addToStatefulSetItems(statefulSet);
                    setProcessingInstruction(FABRIC8_GENERATED_CONTAINERS, getContainersFromPodSpec(statefulSet.getSpec().getTemplate()));
                } else if ("daemonSet".equalsIgnoreCase(type)) {
                    log.info("Adding a default DaemonSet");
                    DaemonSet daemonSet = daemonSetHandler.getDaemonSet(config, images);
                    builder.addToDaemonSetItems(daemonSet);
                    setProcessingInstruction(FABRIC8_GENERATED_CONTAINERS, getContainersFromPodSpec(daemonSet.getSpec().getTemplate()));
                } else if ("replicaSet".equalsIgnoreCase(type)) {
                    log.info("Adding a default ReplicaSet");
                    ReplicaSet replicaSet = rsHandler.getReplicaSet(config, images);
                    builder.addToReplicaSetItems(replicaSet);
                    setProcessingInstruction(FABRIC8_GENERATED_CONTAINERS, getContainersFromPodSpec(replicaSet.getSpec().getTemplate()));
                } else if ("replicationController".equalsIgnoreCase(type)) {
                    log.info("Adding a default ReplicationController");
                    ReplicationController replicationController = rcHandler.getReplicationController(config, images);
                    builder.addToReplicationControllerItems(replicationController);
                    setProcessingInstruction(FABRIC8_GENERATED_CONTAINERS, getContainersFromPodSpec(replicationController.getSpec().getTemplate()));
                } else if ("job".equalsIgnoreCase(type)) {
                    log.info("Adding a default Job");
                    Job job = jobHandler.getJob(config, images);
                    builder.addToJobItems(job);
                    setProcessingInstruction(FABRIC8_GENERATED_CONTAINERS, getContainersFromPodSpec(job.getSpec().getTemplate()));
                }
            }
        }
    }

    private List<String> getContainersFromPodSpec(PodTemplateSpec spec) {
        List<String> containerNames = new ArrayList<>();
        spec.getSpec().getContainers().forEach(container -> { containerNames.add(container.getName()); });
        return containerNames;
    }

    static {
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(String.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(Double.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(Float.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(Long.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(Integer.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(Short.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(Character.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(Byte.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(double.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(float.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(long.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(int.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(short.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(char.class);
        KubernetesResourceUtil.SIMPLE_FIELD_TYPES.add(byte.class);
    }

}
