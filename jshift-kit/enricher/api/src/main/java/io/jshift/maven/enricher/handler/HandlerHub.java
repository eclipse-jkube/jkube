/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jshift.maven.enricher.handler;


import io.jshift.kit.config.resource.GroupArtifactVersion;

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
