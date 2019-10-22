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
package io.jkube.maven.enricher.handler;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.config.resource.ResourceConfig;
import io.jkube.kit.config.resource.VolumeConfig;
import io.jkube.kit.config.resource.VolumeType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author roland
 * @since 08/04/16
 */
public class PodTemplateHandler {

    private final ContainerHandler containerHandler;

    public PodTemplateHandler(ContainerHandler containerHandler) {
        this.containerHandler = containerHandler;
    }

    public PodTemplateSpec getPodTemplate(ResourceConfig config, List<ImageConfiguration> images)  {
        return new PodTemplateSpecBuilder()
            .withMetadata(createPodMetaData(config))
            .withSpec(createPodSpec(config, images))
            .build();
    }

    private ObjectMeta createPodMetaData(ResourceConfig config) {
        return new ObjectMetaBuilder()
            .build();
    }

    private PodSpec createPodSpec(ResourceConfig config, List<ImageConfiguration> images) {

        return new PodSpecBuilder()
            .withServiceAccountName(config.getServiceAccount())
            .withContainers(containerHandler.getContainers(config,images))
            .withVolumes(getVolumes(config))
            .build();
    }

    private List<Volume> getVolumes(ResourceConfig config) {
        List<VolumeConfig> volumeConfigs = config.getVolumes();

        List<Volume> ret = new ArrayList<>();
        if (volumeConfigs != null) {
            for (VolumeConfig volumeConfig : volumeConfigs) {
                VolumeType type = VolumeType.typeFor(volumeConfig.getType());
                if (type != null) {
                    ret.add(type.fromConfig(volumeConfig));
                }
            }
        }
        return ret;
    }



}
