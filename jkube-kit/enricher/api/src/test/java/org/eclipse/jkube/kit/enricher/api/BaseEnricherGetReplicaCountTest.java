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
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.enricher.api.BaseEnricher.getReplicaCount;

public class BaseEnricherGetReplicaCountTest {

  @Test
  public void withNoListBuilderAndNoResourceConfigShouldReturnDefault() {
    // When
    final int result = getReplicaCount(null, null, 1337);
    // Then
    assertThat(result).isEqualTo(1337);
  }

  @Test
  public void withNoListBuilderAndEmptyResourceConfigShouldReturnDefault() {
    // When
    final int result = getReplicaCount(null, new ResourceConfig(), 1337);
    // Then
    assertThat(result).isEqualTo(1337);
  }

  @Test
  public void withNoListBuilderAndConfiguredResourceConfigShouldReturnResourceConfig() {
    // Given
    final ResourceConfig resourceConfig = ResourceConfig.builder().replicas(313373).build();
    // When
    final int result = getReplicaCount(null, resourceConfig, 1337);
    // Then
    assertThat(result).isEqualTo(313373);
  }

  @Test
  public void withEmptyListBuilderAndEmptyResourceConfigShouldReturnDefault() {
    // When
    final int result = getReplicaCount(
        new KubernetesListBuilder().addToItems(new ConfigMapBuilder()), new ResourceConfig(), 1337);
    // Then
    assertThat(result).isEqualTo(1337);
  }

  @Test
  public void withDeploymentConfigInListBuilderAndEmptyResourceConfigShouldReturnDeploymentConfig() {
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
  public void withDeploymentConfigAndDeploymentInListBuilderAndEmptyResourceConfigShouldReturnValueInFirstItem() {
    // Given
    final KubernetesListBuilder klb = new KubernetesListBuilder()
        .addToItems(new DeploymentBuilder().withNewSpec().withReplicas(2).endSpec())
        .addToItems(new DeploymentConfigBuilder().withNewSpec().withReplicas(1).endSpec());
    final ResourceConfig resourceConfig = ResourceConfig.builder().replicas(313373).build();
    // When
    final int result = getReplicaCount(klb, resourceConfig, 1337);
    // Then
    assertThat(result).isEqualTo(2);
  }
}