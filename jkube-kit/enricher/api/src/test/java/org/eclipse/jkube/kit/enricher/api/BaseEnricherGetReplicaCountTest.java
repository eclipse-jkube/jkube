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
package org.eclipse.jkube.kit.enricher.api;

import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.enricher.api.BaseEnricher.getReplicaCount;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class BaseEnricherGetReplicaCountTest {

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("with no list builder")
  class NoListBuilder {

    @Test
    @DisplayName("no resource config, should return default value")
    void nullResourceConfig() {
      // When
      int result = getReplicaCount(null, null, 42);
      // Then
      assertThat(result).isEqualTo(42);
    }

    @DisplayName("with resource config")
    @ParameterizedTest(name = "and {0} replicas should return ''{1}'' replicas")
    @MethodSource("getReplicaCountsData")
    void resourceConfig(Integer replicas, int expectedReplicas) {
      // Given
      final ResourceConfig resourceConfig = ResourceConfig.builder().replicas(replicas).build();
      // When
      int result = getReplicaCount(null, resourceConfig, 42);
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
    // When
    int result = getReplicaCount(new KubernetesListBuilder().addToItems(new ConfigMapBuilder()), new ResourceConfig(), 1337);
    // Then
    assertThat(result).isEqualTo(1337);
  }

  @Test
  void withDeploymentConfigInListBuilderAndEmptyResourceConfig_shouldReturnDeploymentConfig() {
    // Given
    final KubernetesListBuilder klb = new KubernetesListBuilder()
            .addToItems(new DeploymentConfigBuilder().withNewSpec().withReplicas(1).endSpec());
    final ResourceConfig resourceConfig = ResourceConfig.builder().replicas(313373).build();
    // When
    final int result = getReplicaCount(klb, resourceConfig, 1337);
    // Then
    assertThat(result).isEqualTo(1);
  }

  @Test
  void withDeploymentAndDeploymentConfigInListBuilderAndEmptyResourceConfig_shouldReturnValueInFirstItem() {
    // Given
    final KubernetesListBuilder klb = new KubernetesListBuilder()
            .addToItems(new DeploymentBuilder().withNewSpec().withReplicas(2).endSpec())
            .addToItems(new DeploymentConfigBuilder().withNewSpec().withReplicas(1).endSpec());
    final ResourceConfig resourceConfig = ResourceConfig.builder().replicas(313373).build();
    // When
    int result = getReplicaCount(klb, resourceConfig, 1337);
    // Then
    assertThat(result).isEqualTo(2);
  }
}
