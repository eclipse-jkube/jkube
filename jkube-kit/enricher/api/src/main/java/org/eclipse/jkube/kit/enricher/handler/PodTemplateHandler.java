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
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;
import org.eclipse.jkube.kit.config.resource.VolumeType;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.createNewInitContainersFromConfig;

/**
 * @author roland
 */
public class PodTemplateHandler {

    private final ContainerHandler containerHandler;

    public PodTemplateHandler(ContainerHandler containerHandler) {
        this.containerHandler = containerHandler;
    }

    public PodTemplateSpec getPodTemplate(ControllerResourceConfig config, String restartPolicy, List<ImageConfiguration> images)  {
        return new PodTemplateSpecBuilder()
            .withMetadata(createPodMetaData())
            .withSpec(createPodSpec(config, restartPolicy, images))
            .build();
    }

    private ObjectMeta createPodMetaData() {
        return new ObjectMetaBuilder().build();
    }

    private PodSpec createPodSpec(ControllerResourceConfig config, String restartPolicy, List<ImageConfiguration> images) {

        return new PodSpecBuilder()
            .withRestartPolicy(restartPolicy)
            .withContainers(containerHandler.getContainers(config,images))
            .withInitContainers(createNewInitContainersFromConfig(config.getInitContainers()))
            .withVolumes(getVolumes(config))
            .build();
    }

    private List<Volume> getVolumes(ControllerResourceConfig config) {
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
