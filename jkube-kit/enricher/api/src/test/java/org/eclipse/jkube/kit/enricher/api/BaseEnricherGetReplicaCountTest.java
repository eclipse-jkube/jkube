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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.enricher.api.BaseEnricher.getReplicaCount;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class BaseEnricherGetReplicaCountTest {

  @DisplayName("with no list builder")
  @ParameterizedTest(name = "and {0} should return ''{2}'' replicas")
  @MethodSource("getReplicaCountsData")
  void getReplicaCounts(String description, ResourceConfig resourceConfig, int expectedReplicas) {
    // Given & When
    int result = getReplicaCount(null, resourceConfig, 1337);
    // Then
    assertThat(result).isEqualTo(expectedReplicas);
  }

  static Stream<Arguments> getReplicaCountsData() {
    return Stream.of(
        arguments("no resource config", null, 1337),
        arguments("empty resource config", new ResourceConfig(), 1337),
        arguments("configured resource config", ResourceConfig.builder().replicas(313373).build(), 313373)
    );
  }

  @Test
  @DisplayName("with empty list builder and resource config, should return default (1337 replicas)")
  void withEmptyListBuilderAndEmptyResourceConfig_shouldReturnDefault() {
    // When
    int result = getReplicaCount(new KubernetesListBuilder().addToItems(new ConfigMapBuilder()), new ResourceConfig(), 1337);
    // Then
    assertThat(result).isEqualTo(1337);
  }

  @Test
  @DisplayName("with deployment config in list builder and empty resource config, should return deployment config (1 replica)")
  void withDeploymentConfigInListBuilderAndEmptyResourceConfig_shouldReturnDeploymentConfig() {
    // Given
    KubernetesListBuilder klb = new KubernetesListBuilder()
            .addToItems(new DeploymentConfigBuilder().withNewSpec().withReplicas(1).endSpec());
    ResourceConfig resourceConfig = ResourceConfig.builder().replicas(313373).build();
    // When
    int result = getReplicaCount(klb, resourceConfig, 1337);
    // Then
    assertThat(result).isEqualTo(1);
  }

  @Test
  @DisplayName("with deployment and deployment config in list builder and empty resource config, should return value from deployment (2 replicas)")
  void withDeploymentAndDeploymentConfigInListBuilderAndEmptyResourceConfig_shouldReturnValueInFirstItem() {
    // Given
    KubernetesListBuilder klb = new KubernetesListBuilder()
            .addToItems(new DeploymentBuilder().withNewSpec().withReplicas(2).endSpec())
            .addToItems(new DeploymentConfigBuilder().withNewSpec().withReplicas(1).endSpec());
    ResourceConfig resourceConfig = ResourceConfig.builder().replicas(313373).build();
    // When
    int result = getReplicaCount(klb, resourceConfig, 1337);
    // Then
    assertThat(result).isEqualTo(2);
  }
}