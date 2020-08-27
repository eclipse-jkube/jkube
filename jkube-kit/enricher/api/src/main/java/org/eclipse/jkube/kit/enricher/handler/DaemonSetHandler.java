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

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetSpec;
import io.fabric8.kubernetes.api.model.apps.DaemonSetSpecBuilder;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import java.util.List;

/**
 * Created by matthew on 26/10/16.
 */
public class DaemonSetHandler {

    private final PodTemplateHandler podTemplateHandler;

    DaemonSetHandler(PodTemplateHandler podTemplateHandler) {
        this.podTemplateHandler = podTemplateHandler;
    }

    public DaemonSet getDaemonSet(ResourceConfig config,
                                  List<ImageConfiguration> images) {
        return new DaemonSetBuilder()
                .withMetadata(createDaemonSetMetaData(config))
                .withSpec(createDaemonSetSpec(config, images))
                .build();
    }

    // ===========================================================

    private ObjectMeta createDaemonSetMetaData(ResourceConfig config) {
        return new ObjectMetaBuilder()
                .withName(KubernetesHelper.validateKubernetesId(config.getControllerName(), "controller name"))
                .build();
    }

    private DaemonSetSpec createDaemonSetSpec(ResourceConfig config, List<ImageConfiguration> images) {
        return new DaemonSetSpecBuilder()
                .withTemplate(podTemplateHandler.getPodTemplate(config,images))
                .build();
    }

}
