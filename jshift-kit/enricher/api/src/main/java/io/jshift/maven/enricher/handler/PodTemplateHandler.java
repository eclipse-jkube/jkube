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

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.jshift.kit.build.service.docker.ImageConfiguration;
import io.jshift.kit.config.resource.ResourceConfig;
import io.jshift.kit.config.resource.VolumeConfig;
import io.jshift.kit.config.resource.VolumeType;

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
