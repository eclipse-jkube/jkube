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


import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;

import java.util.Properties;

/**
 * @author roland
 * @since 08/04/16
 */
public class HandlerHub {

    private final PodTemplateHandler podTemplateHandler;

    public HandlerHub(GroupArtifactVersion groupArtifactVersion, Properties configuration) {
        ProbeHandler probeHandler = new ProbeHandler();
        ContainerHandler containerHandler = new ContainerHandler(configuration, groupArtifactVersion, probeHandler);
        podTemplateHandler = new PodTemplateHandler(containerHandler);
    }

    public ServiceHandler getServiceHandler() {
        return new ServiceHandler();
    }

    public DeploymentHandler getDeploymentHandler() {
        return new DeploymentHandler(podTemplateHandler);
    }

    public DeploymentConfigHandler getDeploymentConfigHandler() {
        return new DeploymentConfigHandler(podTemplateHandler);
    }

    public ReplicaSetHandler getReplicaSetHandler() {
        return new ReplicaSetHandler(podTemplateHandler);
    }

    public ReplicationControllerHandler getReplicationControllerHandler() {
        return new ReplicationControllerHandler(podTemplateHandler);
    }

    public StatefulSetHandler getStatefulSetHandler() {
        return new StatefulSetHandler(podTemplateHandler);
    }

    public DaemonSetHandler getDaemonSetHandler() {
        return new DaemonSetHandler(podTemplateHandler);
    }

    public JobHandler getJobHandler() {
        return new JobHandler(podTemplateHandler);
    }

    public ProjectHandler getProjectHandler() { return new ProjectHandler(); }

    public NamespaceHandler getNamespaceHandler() { return new NamespaceHandler(); }
}
