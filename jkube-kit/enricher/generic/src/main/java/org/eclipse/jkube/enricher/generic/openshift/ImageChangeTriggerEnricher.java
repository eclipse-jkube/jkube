/*
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
package org.eclipse.jkube.enricher.generic.openshift;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.openshift.api.model.DeploymentConfigSpecBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ImageChangeTriggerEnricher extends BaseEnricher {
    private static final String ENRICHER_NAME = "jkube-openshift-imageChangeTrigger";
    private final Boolean enableAutomaticTrigger;
    private final Boolean enableImageChangeTrigger;
    private final Boolean trimImageInContainerSpecFlag;


    @AllArgsConstructor
    private enum Config implements Configs.Config {
        CONTAINERS("containers", "");

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    public ImageChangeTriggerEnricher(EnricherContext context) {
        super(context, ENRICHER_NAME);
        this.enableAutomaticTrigger = getValueFromConfig(OPENSHIFT_ENABLE_AUTOMATIC_TRIGGER, true);
        this.enableImageChangeTrigger = getValueFromConfig(IMAGE_CHANGE_TRIGGERS, true);
        this.trimImageInContainerSpecFlag = getValueFromConfig(OPENSHIFT_TRIM_IMAGE_IN_CONTAINER_SPEC, false);
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        if(platformMode.equals(PlatformMode.kubernetes))
            return;

        builder.accept(new TypedVisitor<DeploymentConfigSpecBuilder>() {
            @Override
            public void visit(DeploymentConfigSpecBuilder builder) {
                Map<String, String> containerToImageMap = new HashMap<>();
                PodTemplateSpec template = builder.buildTemplate();
                if (template != null) {
                    PodSpec podSpec = template.getSpec();
                    Objects.requireNonNull(podSpec, "No PodSpec for PodTemplate:" + template);
                    List<Container> containers = podSpec.getContainers();
                    for(Container container : containers) {
                        if(container.getName() != null && container.getImage() != null) {
                            containerToImageMap.put(container.getName(), container.getImage());
                        }
                    }
                }
                // add a new image change trigger for the build stream
                if (containerToImageMap.size() != 0) {
                    if(enableImageChangeTrigger && isOpenShiftMode()) {
                        for (Map.Entry<String, String> entry : containerToImageMap.entrySet()) {
                            String containerName = entry.getKey();

                            if(!isImageChangeTriggerNeeded(containerName))
                                continue;

                            ImageName image = new ImageName(entry.getValue());
                            String tag = image.getTag() != null ? image.getTag() : "latest";
                            builder.addNewTrigger()
                                    .withType("ImageChange")
                                    .withNewImageChangeParams()
                                    .withAutomatic(enableAutomaticTrigger)
                                    .withNewFrom()
                                    .withKind("ImageStreamTag")
                                    .withName(image.getSimpleName() + ":" + tag)
                                    .endFrom()
                                    .withContainerNames(containerName)
                                    .endImageChangeParams()
                                    .endTrigger();
                        }
                        if(trimImageInContainerSpecFlag) {
                            builder.editTemplate().editSpec().withContainers(trimImagesInContainers(template)).endSpec().endTemplate();
                        }
                    }

                }
            }
        });
    }

    private Boolean isImageChangeTriggerNeeded(String containerName) {
        String containersFromConfig = Configs.asString(getConfig(Config.CONTAINERS));
        Boolean enrichAll = getValueFromConfig(ENRICH_ALL_WITH_IMAGE_TRIGGERS, false);
        JKubeBuildStrategy buildStrategy = getContext().getConfiguration().getJKubeBuildStrategy();

        if (Boolean.TRUE.equals(enrichAll)) {
            return true;
        }

        if (buildStrategy != null && buildStrategy.equals(JKubeBuildStrategy.jib)) {
            return false;
        }

        return getProcessingInstructionViaKey(FABRIC8_GENERATED_CONTAINERS).contains(containerName) ||
            getProcessingInstructionViaKey(NEED_IMAGECHANGE_TRIGGERS).contains(containerName) ||
            Arrays.asList(containersFromConfig.split(",")).contains(containerName);
    }

    private List<Container> trimImagesInContainers(PodTemplateSpec template) {
        List<Container> containers = template.getSpec().getContainers();
        containers.forEach(container -> container.setImage(""));
        return containers;
    }
}
