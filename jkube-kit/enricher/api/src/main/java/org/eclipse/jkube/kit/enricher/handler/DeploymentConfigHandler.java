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

import java.util.List;

import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import io.fabric8.openshift.api.model.DeploymentConfigSpecBuilder;

public class DeploymentConfigHandler {
    private final PodTemplateHandler podTemplateHandler;
    DeploymentConfigHandler(PodTemplateHandler podTemplateHandler) {
        this.podTemplateHandler = podTemplateHandler;
    }

    public DeploymentConfig getDeploymentConfig(ResourceConfig config, List<ImageConfiguration> images,
        Long openshiftDeployTimeoutSeconds) {
      return new DeploymentConfigBuilder()
          .withMetadata(createDeploymentConfigMetaData(config))
          .withSpec(createDeploymentConfigSpec(config, images, openshiftDeployTimeoutSeconds))
          .build();
    }

    // ===========================================================

    private ObjectMeta createDeploymentConfigMetaData(ResourceConfig config) {
        return new ObjectMetaBuilder()
                .withName(KubernetesHelper.validateKubernetesId(config.getControllerName(), "controller name"))
                .build();
    }

    private DeploymentConfigSpec createDeploymentConfigSpec(ResourceConfig config, List<ImageConfiguration> images, Long openshiftDeployTimeoutSeconds) {
        DeploymentConfigSpecBuilder specBuilder = new DeploymentConfigSpecBuilder();

        PodTemplateSpec podTemplateSpec = podTemplateHandler.getPodTemplate(config,images);

        specBuilder.withReplicas(config.getReplicas())
                .withTemplate(podTemplateSpec)
                .addNewTrigger().withType("ConfigChange").endTrigger();

        if (openshiftDeployTimeoutSeconds != null && openshiftDeployTimeoutSeconds > 0) {
            specBuilder.withNewStrategy().withType("Rolling").
                    withNewRollingParams().withTimeoutSeconds(openshiftDeployTimeoutSeconds).endRollingParams().endStrategy();
        }

        return specBuilder.build();
    }
}