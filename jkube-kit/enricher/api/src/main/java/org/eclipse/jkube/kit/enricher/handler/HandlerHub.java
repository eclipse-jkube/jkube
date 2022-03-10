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
package org.eclipse.jkube.kit.enricher.handler;


import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.openshift.api.model.DeploymentConfig;
import org.eclipse.jkube.kit.common.util.LazyBuilder;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author roland
 * @since 08/04/16
 */
public class HandlerHub {

    private final PodTemplateHandler podTemplateHandler;
    private final LazyBuilder<DeploymentHandler> deploymentHandler;
    private final LazyBuilder<DeploymentConfigHandler> deploymentConfigHandler;
    private final LazyBuilder<ReplicaSetHandler> replicaSetHandler;
    private final LazyBuilder<ReplicationControllerHandler> replicationControllerHandler;
    private final LazyBuilder<StatefulSetHandler> statefulSetHandler;
    private final LazyBuilder<DaemonSetHandler> daemonSetHandler;
    private final LazyBuilder<JobHandler> jobHandler;
    private final LazyBuilder<NamespaceHandler> namespaceHandler;
    private final LazyBuilder<ProjectHandler> projectHandler;
    private final LazyBuilder<ServiceHandler> serviceHandler;

    public HandlerHub(GroupArtifactVersion groupArtifactVersion, Properties configuration) {
        ProbeHandler probeHandler = new ProbeHandler();
        ContainerHandler containerHandler = new ContainerHandler(configuration, groupArtifactVersion, probeHandler);
        podTemplateHandler = new PodTemplateHandler(containerHandler);
        deploymentHandler = new LazyBuilder<>(() -> new DeploymentHandler(podTemplateHandler));
        deploymentConfigHandler = new LazyBuilder<>(() -> new DeploymentConfigHandler(podTemplateHandler));
        replicaSetHandler = new LazyBuilder<>(() -> new ReplicaSetHandler(podTemplateHandler));
        replicationControllerHandler = new LazyBuilder<>(() -> new ReplicationControllerHandler(podTemplateHandler));
        statefulSetHandler = new LazyBuilder<>(() -> new StatefulSetHandler(podTemplateHandler));
        daemonSetHandler = new LazyBuilder<>(() -> new DaemonSetHandler(podTemplateHandler));
        jobHandler = new LazyBuilder<>(() -> new JobHandler(podTemplateHandler));
        namespaceHandler = new LazyBuilder<>(NamespaceHandler::new);
        projectHandler = new LazyBuilder<>(ProjectHandler::new);
        serviceHandler = new LazyBuilder<>(ServiceHandler::new);
    }

    public List<? extends ControllerHandler<?>> getControllerHandlers() {
        return Arrays.asList(
            getDaemonSetHandler(),
            getDeploymentConfigHandler(),
            getDeploymentHandler(),
            getJobHandler(),
            getReplicaSetHandler(),
            getReplicationControllerHandler(),
            getStatefulSetHandler()
        );
    }
    public DeploymentHandler getDeploymentHandler() {
        return deploymentHandler.get();
    }

    public DeploymentConfigHandler getDeploymentConfigHandler() {
        return deploymentConfigHandler.get();
    }

    public ReplicaSetHandler getReplicaSetHandler() {
        return replicaSetHandler.get();
    }

    public ReplicationControllerHandler getReplicationControllerHandler() {
        return replicationControllerHandler.get();
    }

    public StatefulSetHandler getStatefulSetHandler() {
        return statefulSetHandler.get();
    }

    public DaemonSetHandler getDaemonSetHandler() {
        return daemonSetHandler.get();
    }

    public JobHandler getJobHandler() {
        return jobHandler.get();
    }

    public NamespaceHandler getNamespaceHandler() { return namespaceHandler.get(); }

    public ProjectHandler getProjectHandler() { return projectHandler.get(); }

    public ServiceHandler getServiceHandler() {
        return serviceHandler.get();
    }

    public <T extends HasMetadata> ControllerHandler getHandlerFor(T item) {
        if (item instanceof Deployment) {
            return getDeploymentHandler();
        } else if (item instanceof DeploymentConfig) {
            return getDeploymentConfigHandler();
        } else if (item instanceof ReplicationController) {
            return getReplicationControllerHandler();
        } else if (item instanceof ReplicaSet) {
            return getReplicaSetHandler();
        } else if (item instanceof StatefulSet) {
            return getStatefulSetHandler();
        } else if (item instanceof DaemonSet) {
            return getDaemonSetHandler();
        } else if (item instanceof Job) {
            return getJobHandler();
        }
        return null;
    }
}
