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
package org.eclipse.jkube.kit.config.service;

import org.eclipse.jkube.kit.build.service.docker.DockerServiceHub;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.kubernetes.DockerBuildService;
import org.eclipse.jkube.kit.config.service.kubernetes.JibBuildService;
import org.eclipse.jkube.kit.config.service.kubernetes.KubernetesUndeployService;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftBuildService;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftUndeployService;

import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unused"})
public class JKubeServiceHubTest {

  private JKubeServiceHub.JKubeServiceHubBuilder jKubeServiceHubBuilder;

  @Before
  public void setUp() throws Exception {
    final ClusterAccess clusterAccess = mock(ClusterAccess.class, RETURNS_DEEP_STUBS);
    when(clusterAccess.createDefaultClient()).thenReturn(mock(OpenShiftClient.class, RETURNS_DEEP_STUBS));
    jKubeServiceHubBuilder = JKubeServiceHub.builder()
        .platformMode(RuntimeMode.KUBERNETES)
        .configuration(mock(JKubeConfiguration.class, RETURNS_DEEP_STUBS))
        .clusterAccess(clusterAccess)
        .log(mock(KitLogger.class))
        .dockerServiceHub(mock(DockerServiceHub.class, RETURNS_DEEP_STUBS))
        .offline(false)
        .buildServiceConfig(mock(BuildServiceConfig.class, RETURNS_DEEP_STUBS));
  }

  @Test(expected = NullPointerException.class)
  public void testMissingClusterAccess() {
    JKubeServiceHub.builder()
        .log(mock(KitLogger.class))
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void testMissingKitLogger() {
    JKubeServiceHub.builder()
        .clusterAccess(mock(ClusterAccess.class))
        .build();
  }

  @Test
  public void testBasicInit() {
    // When
    try (final JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()
    ) {
      // Then
      assertThat(jKubeServiceHub)
          .isNotNull()
          .hasFieldOrPropertyWithValue("runtimeMode", RuntimeMode.KUBERNETES)
          .extracting(JKubeServiceHub::getClient)
          .isNotNull();
    }
  }

  @Test
  public void testGetBuildServiceInKubernetes() {
    // Given
    JKubeServiceHub hub = jKubeServiceHubBuilder.build();
    // When
    BuildService buildService = hub.getBuildService();
    // Then
    assertThat(buildService)
        .isNotNull()
        .isInstanceOf(DockerBuildService.class);
  }

  @Test
  public void testGetBuildServiceInOpenShift() {
    // Given
    JKubeServiceHub hub = jKubeServiceHubBuilder.platformMode(RuntimeMode.OPENSHIFT)
            .build();
    // When
    BuildService buildService = hub.getBuildService();
    // Then
    assertThat(buildService)
        .isNotNull()
        .isInstanceOf(OpenshiftBuildService.class);
  }

  @Test
  public void testGetJibBuildServiceInKubernetes() {
    // Given
    JKubeServiceHub hub = jKubeServiceHubBuilder.build();
    when(hub.getBuildServiceConfig().getJKubeBuildStrategy()).thenReturn(JKubeBuildStrategy.jib);
    // When
    BuildService buildService = hub.getBuildService();
    // Then
    assertThat(buildService)
        .isNotNull()
        .isInstanceOf(JibBuildService.class);
  }

  @Test
  public void testGetUndeployServiceInKubernetes() {
    // Given
    JKubeServiceHub hub = jKubeServiceHubBuilder.build();
    // When
    final UndeployService result = hub.getUndeployService();
    // Then
    assertThat(result)
        .isNotNull()
        .isInstanceOf(KubernetesUndeployService.class);
  }

  @Test
  public void testGetUndeployServiceInOpenShiftWithInvalidClient() {
    // Given
    JKubeServiceHub hub = jKubeServiceHubBuilder.platformMode(RuntimeMode.OPENSHIFT).build();
    when(hub.getClient().isAdaptable(OpenShiftClient.class)).thenReturn(false);
    // When
    final UndeployService result = hub.getUndeployService();
    // Then
    assertThat(result)
        .isNotNull()
        .isInstanceOf(KubernetesUndeployService.class);
  }

  @Test
  public void testGetUndeployServiceInOpenShiftWithValidClient() {
    // Given
    JKubeServiceHub hub = jKubeServiceHubBuilder.platformMode(RuntimeMode.OPENSHIFT).build();
    when(hub.getClient().isAdaptable(OpenShiftClient.class)).thenReturn(true);
    // When
    final UndeployService result = hub.getUndeployService();
    // Then
    assertThat(result)
        .isNotNull()
        .isInstanceOf(OpenshiftUndeployService.class);
  }

  @Test
  public void testGetPortForwardService() {
    // When
    final PortForwardService portForwardService = jKubeServiceHubBuilder.build().getPortForwardService();
    // Then
    assertThat(portForwardService).isNotNull();
  }

  @Test
  public void testGetDebugService() {
    // When
    final DebugService debugService = jKubeServiceHubBuilder.build().getDebugService();
    // Then
    assertThat(debugService).isNotNull();
  }

  @Test
  public void testBasicInitWithOffline() {
    // Given + When
    try (final JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.platformMode(RuntimeMode.KUBERNETES).offline(true).build()) {
      // Then
      assertThat(jKubeServiceHub)
          .isNotNull()
          .hasFieldOrPropertyWithValue("client", null);
    }
  }

  @Test
  public void testAccessServiceWithNonInitializedClientThrowsException() {
    // Given + When
    try (final JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.platformMode(RuntimeMode.KUBERNETES).offline(true).build()) {
      // Then
      assertThrows(IllegalArgumentException.class, jKubeServiceHub::getApplyService);
    }
  }
}
