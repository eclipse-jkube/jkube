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


import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import lombok.Getter;
import org.eclipse.jkube.kit.common.util.LazyBuilder;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.openshift.api.model.DeploymentConfig;

/**
 * @author roland
 * @since 08/04/16
 */
public class HandlerHub {

    private final PodTemplateHandler podTemplateHandler;
    @Getter
    private final List<ControllerHandlerLazyBuilder<? extends HasMetadata>> controllerHandlers;
    private final LazyBuilder<NamespaceHandler> namespaceHandler;
    private final LazyBuilder<ProjectHandler> projectHandler;
    private final LazyBuilder<ServiceHandler> serviceHandler;

    public HandlerHub(GroupArtifactVersion groupArtifactVersion, Properties configuration) {
        ProbeHandler probeHandler = new ProbeHandler();
        ContainerHandler containerHandler = new ContainerHandler(configuration, groupArtifactVersion, probeHandler);
        podTemplateHandler = new PodTemplateHandler(containerHandler);
        controllerHandlers = Arrays.asList(
            new ControllerHandlerLazyBuilder<>(Deployment.class, () -> new DeploymentHandler(podTemplateHandler)),
            new ControllerHandlerLazyBuilder<>(DeploymentConfig.class, () ->
                new DeploymentConfigHandler(podTemplateHandler)),
            new ControllerHandlerLazyBuilder<>(ReplicaSet.class,() -> new ReplicaSetHandler(podTemplateHandler)),
            new ControllerHandlerLazyBuilder<>(ReplicationController.class,() ->
                new ReplicationControllerHandler(podTemplateHandler)),
            new ControllerHandlerLazyBuilder<>(StatefulSet.class,() -> new StatefulSetHandler(podTemplateHandler)),
            new ControllerHandlerLazyBuilder<>(DaemonSet.class,() -> new DaemonSetHandler(podTemplateHandler)),
            new ControllerHandlerLazyBuilder<>(Job.class,() -> new JobHandler(podTemplateHandler)),
            new ControllerHandlerLazyBuilder<>(CronJob.class, () -> new CronJobHandler(podTemplateHandler))
        );
        namespaceHandler = new LazyBuilder<>(NamespaceHandler::new);
        projectHandler = new LazyBuilder<>(ProjectHandler::new);
        serviceHandler = new LazyBuilder<>(ServiceHandler::new);
    }

    public NamespaceHandler getNamespaceHandler() { return namespaceHandler.get(); }

    public ProjectHandler getProjectHandler() { return projectHandler.get(); }

    public ServiceHandler getServiceHandler() {
        return serviceHandler.get();
    }


    @SuppressWarnings("unchecked")
    public <T extends HasMetadata> ControllerHandler<T> getHandlerFor(T item) {
        if (item == null) {
            return null;
        }
        return (ControllerHandler<T>) getHandlerFor(item.getClass());
    }

    @SuppressWarnings("unchecked")
    public <T extends HasMetadata> ControllerHandler<T> getHandlerFor(Class<T> controllerType) {
        return (ControllerHandler<T>) controllerHandlers.stream()
            .filter(handler -> handler.getControllerHandlerType().isAssignableFrom(controllerType))
            .findAny().map(LazyBuilder::get).orElse(null);
    }
}
