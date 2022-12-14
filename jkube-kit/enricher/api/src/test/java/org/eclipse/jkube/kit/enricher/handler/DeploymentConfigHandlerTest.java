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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DeploymentConfigHandlerTest {

  private ControllerResourceConfig.ControllerResourceConfigBuilder controllerResourceConfigBuilder;
  private DeploymentConfigHandler deploymentConfigHandler;

  @BeforeEach
  void before() {
    controllerResourceConfigBuilder = ControllerResourceConfig.builder();
    deploymentConfigHandler = new DeploymentConfigHandler(new PodTemplateHandler(new ContainerHandler(new Properties(),
        new GroupArtifactVersion("g", "a", "v"), new ProbeHandler())));
  }

  @Test
  void get_withNoImagesAndNoControllerName_shouldThrowException() {
    // Given
    final ControllerResourceConfig controllerResourceConfig = controllerResourceConfigBuilder.build();
    final List<ImageConfiguration> images = Collections.emptyList();
    // When & Then
    assertThatIllegalArgumentException()
        .isThrownBy(() -> deploymentConfigHandler.get(controllerResourceConfig, images))
        .withMessage("No controller name is specified!");
  }

  @Test
  void get_withNoImages_shouldReturnConfigWithNoContainers() {
    // Given
    final ControllerResourceConfig controllerResourceConfig = controllerResourceConfigBuilder.controllerName("controller").build();
    final List<ImageConfiguration> images = Collections.emptyList();
    // When
    final DeploymentConfig result = deploymentConfigHandler.get(controllerResourceConfig, images);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("metadata.name", "controller")
        .extracting("spec.template.spec.containers").asList().isEmpty();
  }

  @Test
  void get_withImages_shouldReturnConfigWithContainers() {
    // Given
    final ControllerResourceConfig controllerResourceConfig = controllerResourceConfigBuilder.controllerName("controller").build();
    final List<ImageConfiguration> images = Arrays.asList(
        ImageConfiguration.builder().name("busybox").build(BuildConfiguration.builder().build()).build(),
        ImageConfiguration.builder().name("jkubeio/java:latest").build(BuildConfiguration.builder().build()).build()
    );
    // When
    final DeploymentConfig result = deploymentConfigHandler.get(controllerResourceConfig, images);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("metadata.name", "controller")
        .extracting("spec.template.spec.containers").asList().hasSize(2)
        .extracting("image", "name")
        .containsExactly(new Tuple("busybox:latest", "g-a"), new Tuple("jkubeio/java:latest", "jkubeio-a"));
  }

  @Test
  void getPodTemplateSpec_withNoImages_shouldReturnPodTemplateSpecWithNoContainers() {
    // Given
    final ControllerResourceConfig controllerResourceConfig = controllerResourceConfigBuilder.controllerName("controller").build();
    final List<ImageConfiguration> images = Collections.emptyList();
    // When
    final PodTemplateSpec result = deploymentConfigHandler.getPodTemplateSpec(controllerResourceConfig, images);
    // Then
    assertThat(result).extracting("spec.containers").asList().isEmpty();
  }

  @Test
  void overrideReplicas() {
    // Given
    final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(new DeploymentConfigBuilder()
        .editOrNewSpec().withReplicas(1).endSpec()
        .build());
    // When
    deploymentConfigHandler.overrideReplicas(klb, 1337);
    // Then
    assertThat(klb.buildItems()).singleElement()
        .hasFieldOrPropertyWithValue("spec.replicas", 1337);
  }
}
