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
package org.eclipse.jkube.kit.config.access;

import io.fabric8.kubernetes.api.model.APIGroupListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@EnableKubernetesMockClient(https = false)
class ClusterAccessTest {

  private KitLogger logger;
  private KubernetesMockServer mockServer;
  private ClusterConfiguration clusterConfiguration;

  @BeforeEach
  void setUp() {
    logger = spy(new KitLogger.SilentLogger());
    clusterConfiguration = ClusterConfiguration.builder()
      .masterUrl(mockServer.url("/"))
      .build();
  }

  @Test
  void isOpenShiftOpenShiftClusterShouldReturnTrue() {
    // Given
    mockServer.expect().get().withPath("/apis").andReturn(200,
      new APIGroupListBuilder().addNewGroup().withName("project.openshift.io").withApiVersion("v1").endGroup().build()).once();
    // When
    final boolean result = new ClusterAccess(logger, clusterConfiguration).isOpenShift();
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isOpenShiftKubernetesClusterShouldReturnFalse() {
    // When
    final boolean result = new ClusterAccess(logger, clusterConfiguration).isOpenShift();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isOpenShiftThrowsExceptionShouldReturnFalse() {
    // Given
    clusterConfiguration = ClusterConfiguration.builder().masterUrl("https://unknown.example.com").build();
    // When
    final boolean result = new ClusterAccess(logger, clusterConfiguration).isOpenShift();
    // Then
    assertThat(result).isFalse();
    verify(logger, times(1))
        .warn(startsWith("Cannot access cluster for detecting mode"), eq(""), eq("An error has occurred."));
  }

  @Test
  void createDefaultClientShouldReturnKubernetesClient() {
    // When
    final KubernetesClient result = new ClusterAccess(logger, clusterConfiguration).createDefaultClient();
    // Then
    assertThat(result).isNotNull().isNotInstanceOf(OpenShiftClient.class);
  }
}
