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
package org.eclipse.jkube.kit.enricher.api;

import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class BaseEnricherGetReplicaCountTest {

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("with no list builder")
  class NoListBuilder {

    @Test
    @DisplayName("no controller resource config, should return default value")
    void nullControllerResourceConfig() {
      // Given
      final BaseEnricher enricher = withEnricher(JKubeEnricherContext.builder());
      // When
      int result = enricher.getReplicaCount(null, 42);
      // Then
      assertThat(result).isEqualTo(42);
    }

    @DisplayName("with resource config")
    @ParameterizedTest(name = "and {0} replicas should return ''{1}'' replicas")
    @MethodSource("getReplicaCountsData")
    void resourceConfig(Integer replicas, int expectedReplicas) {
      // Given
      final ControllerResourceConfig controller = ControllerResourceConfig.builder().replicas(replicas).build();
      final BaseEnricher enricher = withEnricher(
        JKubeEnricherContext.builder().resources(ResourceConfig.builder().controller(controller).build()));
      // When
      int result = enricher.getReplicaCount(null, 42);
      // Then
      assertThat(result).isEqualTo(expectedReplicas);
    }

    Stream<Arguments> getReplicaCountsData() {
      return Stream.of(
        arguments(null, 42),
        arguments(1337, 1337)
      );
    }
  }

  @Test
  void withEmptyListBuilderAndEmptyResourceConfig_shouldReturnDefault() {
    // Given
    final BaseEnricher enricher = withEnricher(
      JKubeEnricherContext.builder().resources(ResourceConfig.builder().build()));
    // When
    int result = enricher.getReplicaCount(new KubernetesListBuilder().addToItems(new ConfigMapBuilder()), 1337);
    // Then
    assertThat(result).isEqualTo(1337);
  }

  @Test
  void withEmptyListBuilderAndResourceConfig_shouldReturnResourceConfig() {
    // Given
    final BaseEnricher enricher = withEnricher(
      JKubeEnricherContext.builder().resources(ResourceConfig.builder().replicas(42).build()));
    // When
    int result = enricher.getReplicaCount(new KubernetesListBuilder().addToItems(new ConfigMapBuilder()), 1337);
    // Then
    assertThat(result).isEqualTo(42);
  }

  @Test
  void withEmptyListBuilderAndResourceConfig_shouldReturnControllerResourceConfig() {
    // Given
    final BaseEnricher enricher = withEnricher(
      JKubeEnricherContext.builder().resources(ResourceConfig.builder()
        .controller(ControllerResourceConfig.builder().replicas(42).build()).build()));
    // When
    int result = enricher.getReplicaCount(new KubernetesListBuilder().addToItems(new ConfigMapBuilder()), 1337);
    // Then
    assertThat(result).isEqualTo(42);
  }

  @Test
  void withDeploymentConfigInListBuilderAndResourceConfig_shouldReturnDeploymentConfig() {
    // Given
    final KubernetesListBuilder klb = new KubernetesListBuilder()
            .addToItems(new DeploymentConfigBuilder().withNewSpec().withReplicas(1).endSpec());
    final ControllerResourceConfig controller = ControllerResourceConfig.builder().replicas(313373).build();
    final BaseEnricher enricher = withEnricher(
      JKubeEnricherContext.builder().resources(ResourceConfig.builder().controller(controller).build()));
    // When
    final int result = enricher.getReplicaCount(klb, 1337);
    // Then
    assertThat(result).isEqualTo(1);
  }

  @Test
  void withDeploymentAndDeploymentConfigInListBuilderAndResourceConfig_shouldReturnValueInFirstItem() {
    // Given
    final KubernetesListBuilder klb = new KubernetesListBuilder()
            .addToItems(new DeploymentBuilder().withNewSpec().withReplicas(2).endSpec())
            .addToItems(new DeploymentConfigBuilder().withNewSpec().withReplicas(1).endSpec());
    final ControllerResourceConfig controller = ControllerResourceConfig.builder().replicas(313373).build();
    final BaseEnricher enricher = withEnricher(
      JKubeEnricherContext.builder().resources(ResourceConfig.builder().controller(controller).build()));
    // When
    int result = enricher.getReplicaCount(klb,  1337);
    // Then
    assertThat(result).isEqualTo(2);
  }

  private static BaseEnricher withEnricher(JKubeEnricherContext.JKubeEnricherContextBuilder contextBuilder) {
    return new BaseEnricher(contextBuilder
      .log(new KitLogger.SilentLogger())
      .project(JavaProject.builder().build())
      .build(), "test-enricher");
  }
}
