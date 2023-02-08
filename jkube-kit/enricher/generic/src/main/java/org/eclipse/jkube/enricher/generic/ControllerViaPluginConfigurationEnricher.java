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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.eclipse.jkube.kit.enricher.handler.ControllerHandler;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentFluent;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetFluent;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class ControllerViaPluginConfigurationEnricher extends BaseEnricher {
    protected static final String[] POD_CONTROLLER_KINDS =
            { "ReplicationController", "ReplicaSet", "Deployment", "DeploymentConfig", "StatefulSet", "DaemonSet", "Job" };

    private final ControllerHandler<Deployment> deployHandler;
    private final ControllerHandler<StatefulSet> statefulSetHandler;

    @AllArgsConstructor
    private enum Config implements Configs.Config {
        NAME("name", null),
        /**
         * @deprecated in favor of <code>jkube.imagePullPolicy</code> property
         */
        @Deprecated
        PULL_POLICY("pullPolicy", JKUBE_DEFAULT_IMAGE_PULL_POLICY),
        REPLICA_COUNT("replicaCount", "1");

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    public ControllerViaPluginConfigurationEnricher(EnricherContext context) {
        super(context, "jkube-controller-from-configuration");
        deployHandler = context.getHandlerHub().getHandlerFor(Deployment.class);
        statefulSetHandler = context.getHandlerHub().getHandlerFor(StatefulSet.class);
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        final String name = getConfig(Config.NAME, JKubeProjectUtil.createDefaultResourceName(getContext().getGav().getSanitizedArtifactId()));
        final ControllerResourceConfig controllerResourceConfig = ControllerResourceConfig.builder()
                .controllerName(name)
                .imagePullPolicy(getImagePullPolicy(Config.PULL_POLICY))
                .replicas(getReplicaCount(builder, Configs.asInt(getConfig(Config.REPLICA_COUNT))))
                .initContainers(Optional.ofNullable(getControllerResourceConfig().getInitContainers()).orElse(Collections.emptyList()))
                .build();

        final List<ImageConfiguration> images = getImages();
        // Check if at least a replica set is added. If not add a default one
        if (KubernetesResourceUtil.checkForKind(builder, POD_CONTROLLER_KINDS)) {
            // At least one image must be present, otherwise the resulting config will be invalid
            if (KubernetesResourceUtil.checkForKind(builder, "StatefulSet")) {
                final StatefulSetSpec spec = statefulSetHandler.get(controllerResourceConfig, images).getSpec();
                if (spec != null) {
                    builder.accept(new TypedVisitor<StatefulSetBuilder>() {
                        @Override
                        public void visit(StatefulSetBuilder statefulSetBuilder) {
                            if (statefulSetBuilder.buildMetadata() == null || statefulSetBuilder.buildMetadata().getName() == null) {
                                statefulSetBuilder.editOrNewMetadata().withName(name).endMetadata();
                            }
                            statefulSetBuilder.editOrNewSpec().editOrNewTemplate().editOrNewSpec().endSpec().endTemplate().endSpec();
                            mergeStatefulSetSpec(statefulSetBuilder, spec);
                        }
                    });

                    if (spec.getTemplate() != null && spec.getTemplate().getSpec() != null) {
                        final PodSpec podSpec = spec.getTemplate().getSpec();
                        builder.accept(new TypedVisitor<PodSpecBuilder>() {
                            @Override
                            public void visit(PodSpecBuilder builder) {
                                String defaultApplicationContainerName = KubernetesResourceUtil.mergePodSpec(builder, podSpec, name, getValueFromConfig(SIDECAR, false));
                                if(defaultApplicationContainerName != null) {
                                    setProcessingInstruction(NEED_IMAGECHANGE_TRIGGERS, Collections.singletonList(defaultApplicationContainerName));
                                }
                            }
                        });
                    }
                }
            } else {
                final DeploymentSpec spec = deployHandler.get(controllerResourceConfig, images).getSpec();
                if (spec != null) {
                    builder.accept(new TypedVisitor<DeploymentBuilder>() {
                        @Override
                        public void visit(DeploymentBuilder deploymentBuilder) {
                            if (deploymentBuilder.buildMetadata() == null || deploymentBuilder.buildMetadata().getName() == null) {
                                deploymentBuilder.editOrNewMetadata().withName(name).endMetadata();
                            }
                            deploymentBuilder.editOrNewSpec().editOrNewTemplate().editOrNewSpec().endSpec().endTemplate().endSpec();
                            mergeDeploymentSpec(deploymentBuilder, spec);
                        }
                    });

                    if (spec.getTemplate() != null && spec.getTemplate().getSpec() != null) {
                        final PodSpec podSpec = spec.getTemplate().getSpec();
                        builder.accept(new TypedVisitor<PodSpecBuilder>() {
                            @Override
                            public void visit(PodSpecBuilder builder) {
                                String defaultApplicationContainerName = KubernetesResourceUtil.mergePodSpec(builder, podSpec, name, getValueFromConfig(SIDECAR, false));
                                if(defaultApplicationContainerName != null) {
                                    setProcessingInstruction(NEED_IMAGECHANGE_TRIGGERS, Collections.singletonList(defaultApplicationContainerName));
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    private void mergeDeploymentSpec(DeploymentBuilder builder, DeploymentSpec spec) {
        DeploymentFluent.SpecNested<DeploymentBuilder> specBuilder = builder.editSpec();
        KubernetesResourceUtil.mergeSimpleFields(specBuilder, spec);
        specBuilder.endSpec();
    }

    private void mergeStatefulSetSpec(StatefulSetBuilder builder, StatefulSetSpec spec) {
        StatefulSetFluent.SpecNested<StatefulSetBuilder> specBuilder = builder.editSpec();
        KubernetesResourceUtil.mergeSimpleFields(specBuilder, spec);
        specBuilder.endSpec();
    }

}
