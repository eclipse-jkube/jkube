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
package org.eclipse.jkube.kit.enricher.api.util;

import java.util.List;

import org.eclipse.jkube.kit.common.KitLogger;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;

/**
 * @author roland
 * @since 07/02/17
 */
public class InitContainerHandler {

    public static final String INIT_CONTAINER_ANNOTATION = "pod.alpha.kubernetes.io/init-containers";

    KitLogger log;

    public InitContainerHandler(KitLogger log) {
        this.log = log;
    }

    public boolean hasInitContainer(PodTemplateSpecBuilder builder, String name) {
        return getInitContainer(builder, name) != null;
    }

    public Container getInitContainer(PodTemplateSpecBuilder builder, String name) {
        if (builder.hasSpec()) {
            List<Container> initContainerList = builder.buildSpec().getInitContainers();
            for(Container initContainer : initContainerList) {
                if(initContainer.getName().equals(name)) {
                    return initContainer;
                }
            }
        }
        return null;
    }

    public void removeInitContainer(PodTemplateSpecBuilder builder, String initContainerName) {
        Container initContainer = getInitContainer(builder, initContainerName);
        if (initContainer != null) {
            List<Container> initContainers = builder.buildSpec().getInitContainers();
            initContainers.remove(initContainer);
            builder.editSpec().withInitContainers(initContainers).endSpec();
        }
    }

    public void appendInitContainer(PodTemplateSpecBuilder builder, Container initContainer) {
        String name = initContainer.getName();
        Container existing = getInitContainer(builder, name);
        if (existing != null) {
            if (existing.equals(initContainer)) {
                log.warn("Trying to add init-container %s a second time. Ignoring ....", name);
                return;
            } else {
                throw new IllegalArgumentException(
                    String.format("PodSpec %s already contains a different init container with name %s but can not add a second one with the same name. " +
                                  "Please choose a different name for the init container",
                                  builder.build().getMetadata().getName(), name));
            }
        }

        ensureSpec(builder);
        builder.editSpec().addToInitContainers(initContainer).endSpec();
    }

    private void ensureSpec(PodTemplateSpecBuilder obj) {
        if (obj.buildSpec() == null) {
            obj.withNewSpec().endSpec();
        }
    }
}
