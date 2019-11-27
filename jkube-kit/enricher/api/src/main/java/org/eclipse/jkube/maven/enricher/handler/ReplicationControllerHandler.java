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
package org.eclipse.jkube.maven.enricher.handler;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpecBuilder;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import java.util.List;

/**
 * @author roland
 * @since 08/04/16
 */
public class ReplicationControllerHandler {

    private final PodTemplateHandler podTemplateHandler;

    ReplicationControllerHandler(PodTemplateHandler podTemplateHandler) {
        this.podTemplateHandler = podTemplateHandler;
    }

    public ReplicationController getReplicationController(ResourceConfig config,
                                                          List<ImageConfiguration> images) {
        return new ReplicationControllerBuilder()
            .withMetadata(createRcMetaData(config))
            .withSpec(createRcSpec(config, images))
            .build();
    }

    // ===========================================================
    // TODO: "replica set" config used

    private ObjectMeta createRcMetaData(ResourceConfig config) {
        return new ObjectMetaBuilder()
            .withName(KubernetesHelper.validateKubernetesId(config.getControllerName(), "replication controller name"))
            .build();
    }

    private ReplicationControllerSpec createRcSpec(ResourceConfig config, List<ImageConfiguration> images) {
        return new ReplicationControllerSpecBuilder()
            .withReplicas(config.getReplicas())
            .withTemplate(podTemplateHandler.getPodTemplate(config,images))
            .build();
    }
}
