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
package io.jkube.maven.enricher.api.util;

import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.jkube.kit.common.KitLogger;

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
