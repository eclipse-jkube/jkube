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
package io.jkube.maven.enricher.handler;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import io.fabric8.openshift.api.model.DeploymentConfigSpecBuilder;
import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.config.resource.ResourceConfig;
import io.jkube.kit.common.util.KubernetesHelper;

import java.util.List;

public class DeploymentConfigHandler {
    private final PodTemplateHandler podTemplateHandler;
    DeploymentConfigHandler(PodTemplateHandler podTemplateHandler) {
        this.podTemplateHandler = podTemplateHandler;
    }

    public DeploymentConfig getDeploymentConfig(ResourceConfig config,
                                                List<ImageConfiguration> images, Long openshiftDeployTimeoutSeconds, Boolean imageChangeTrigger, Boolean enableAutomaticTrigger, Boolean isOpenshiftBuildStrategy, List<String> generatedContainers) {

        DeploymentConfig deploymentConfig = new DeploymentConfigBuilder()
                .withMetadata(createDeploymentConfigMetaData(config))
                .withSpec(createDeploymentConfigSpec(config, images, openshiftDeployTimeoutSeconds, imageChangeTrigger, enableAutomaticTrigger, isOpenshiftBuildStrategy, generatedContainers))
                .build();

        return deploymentConfig;
    }

    // ===========================================================

    private ObjectMeta createDeploymentConfigMetaData(ResourceConfig config) {
        return new ObjectMetaBuilder()
                .withName(KubernetesHelper.validateKubernetesId(config.getControllerName(), "controller name"))
                .build();
    }

    private DeploymentConfigSpec createDeploymentConfigSpec(ResourceConfig config, List<ImageConfiguration> images, Long openshiftDeployTimeoutSeconds, Boolean imageChangeTrigger, Boolean enableAutomaticTrigger, Boolean isOpenshiftBuildStrategy, List<String> generatedContainers) {
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

    private void validateContainer(Container container) {
        if (container.getImage() == null) {
            throw new IllegalArgumentException("Container " + container.getName() + " has no Docker image configured. " +
                    "Please check your Docker image configuration (including the generators which are supposed to run)");
        }
    }
}