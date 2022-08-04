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
import org.eclipse.jkube.kit.common.service.MigrateService;
import org.eclipse.jkube.kit.common.util.LazyBuilder;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ResourceService;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.kubernetes.DockerBuildService;
import org.eclipse.jkube.kit.config.service.kubernetes.JibBuildService;
import org.eclipse.jkube.kit.config.service.kubernetes.KubernetesUndeployService;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftBuildService;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftUndeployService;

import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.resource.helm.HelmService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.jkube.kit.config.resource.RuntimeMode.KUBERNETES;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unused"})
public class JKubeServiceHubTest {

  private JKubeServiceHub.JKubeServiceHubBuilder jKubeServiceHubBuilder;
  private OpenShiftClient openShiftClient;

  @Before
  public void setUp() throws Exception {
    final ClusterAccess clusterAccess = mock(ClusterAccess.class, RETURNS_DEEP_STUBS);
    openShiftClient = mock(OpenShiftClient.class);
    when(clusterAccess.createDefaultClient()).thenReturn(openShiftClient);
    when(openShiftClient.adapt(OpenShiftClient.class)).thenReturn(openShiftClient);
    jKubeServiceHubBuilder = JKubeServiceHub.builder()
        .platformMode(KUBERNETES)
        .configuration(mock(JKubeConfiguration.class, RETURNS_DEEP_STUBS))
        .clusterAccess(clusterAccess)
        .log(new KitLogger.SilentLogger())
        .dockerServiceHub(mock(DockerServiceHub.class, RETURNS_DEEP_STUBS))
        .offline(false)
        .buildServiceConfig(mock(BuildServiceConfig.class, RETURNS_DEEP_STUBS));
  }

  @Test
  public void buildWithMissingPlatformModeThrowsException() {
    // Given
    jKubeServiceHubBuilder.platformMode(null);
    // When
    final NullPointerException result = assertThrows(NullPointerException.class, jKubeServiceHubBuilder::build);
    // Then
    assertThat(result).hasMessageContaining("platformMode is a required parameter");
  }

  @Test
  public void buildWithMissingConfigurationThrowsException() {
    // Given
    jKubeServiceHubBuilder.configuration(null);
    // When
    final NullPointerException result = assertThrows(NullPointerException.class, jKubeServiceHubBuilder::build);
    // Then
    assertThat(result).hasMessageContaining("JKubeConfiguration is required");
  }

  @Test
  public void buildWithMissingKitLoggerThrowsException() {
    // Given
    jKubeServiceHubBuilder.log(null);
    // When
    final NullPointerException result = assertThrows(NullPointerException.class, jKubeServiceHubBuilder::build);
    // Then
    assertThat(result).hasMessageContaining("log is a required parameter");
  }

  @Test
  public void testBasicInit() {
    // When
    try (final JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()
    ) {
      // Then
      assertThat(jKubeServiceHub)
          .isNotNull()
          .hasFieldOrPropertyWithValue("runtimeMode", KUBERNETES)
          .extracting(JKubeServiceHub::getClient)
          .isNotNull();
    }
  }

  @Test
  public void getBuildServiceInKubernetes() {
    // Given
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When
      BuildService buildService = jKubeServiceHub.getBuildService();
      // Then
      assertThat(buildService)
          .isNotNull()
          .isInstanceOf(DockerBuildService.class);
    }
  }

  @Test
  public void getBuildServiceInOpenShift() {
    // Given
    jKubeServiceHubBuilder.platformMode(RuntimeMode.OPENSHIFT);
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When
      BuildService buildService = jKubeServiceHub.getBuildService();
      // Then
      assertThat(buildService)
          .isNotNull()
          .isInstanceOf(OpenshiftBuildService.class);
    }
  }

  @Test
  public void getJibBuildServiceInKubernetes() {
    // Given
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      when(jKubeServiceHub.getBuildServiceConfig().getJKubeBuildStrategy()).thenReturn(JKubeBuildStrategy.jib);
      // When
      BuildService buildService = jKubeServiceHub.getBuildService();
      // Then
      assertThat(buildService)
          .isNotNull()
          .isInstanceOf(JibBuildService.class);
    }
  }

  @Test
  public void getUndeployServiceInKubernetes() {
    // Given
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When
      final UndeployService result = jKubeServiceHub.getUndeployService();
      // Then
      assertThat(result)
          .isNotNull()
          .isInstanceOf(KubernetesUndeployService.class);
    }
  }

  @Test
  public void getUndeployServiceInOpenShiftWithInvalidClient() {
    // Given
    jKubeServiceHubBuilder.platformMode(RuntimeMode.OPENSHIFT);
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      when(openShiftClient.isSupported()).thenReturn(true);
      // When
      final UndeployService result = jKubeServiceHub.getUndeployService();
      // Then
      assertThat(result)
          .isNotNull()
          .isInstanceOf(KubernetesUndeployService.class);
    }
  }

  @Test
  public void getUndeployServiceInOpenShiftWithValidClient() {
    // Given
    jKubeServiceHubBuilder.platformMode(RuntimeMode.OPENSHIFT);
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      when(openShiftClient.isSupported()).thenReturn(true);
      // When
      final UndeployService result = jKubeServiceHub.getUndeployService();
      // Then
      assertThat(result)
          .isNotNull()
          .isInstanceOf(OpenshiftUndeployService.class);
    }
  }

  @Test
  public void getPortForwardService() {
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When
      final PortForwardService portForwardService = jKubeServiceHub.getPortForwardService();
      // Then
      assertThat(portForwardService).isNotNull();
    }
  }

  @Test
  public void getDebugService() {
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When
      final DebugService debugService = jKubeServiceHub.getDebugService();
      // Then
      assertThat(debugService).isNotNull();
    }
  }

  @Test
  public void getHelmService() {
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When
      final HelmService helmService = jKubeServiceHub.getHelmService();
      // Then
      assertThat(helmService).isNotNull();
    }
  }

  @Test
  public void getMigrateService() {
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When
      final MigrateService migrateService = jKubeServiceHub.getMigrateService();
      // Then
      assertThat(migrateService).isNotNull();
    }
  }

  @Test
  public void getResourceService() {
    // Given
    jKubeServiceHubBuilder.resourceService(new LazyBuilder<>(() -> mock(ResourceService.class)));
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When
      final ResourceService resourceService = jKubeServiceHub.getResourceService();
      // Then
      assertThat(resourceService).isNotNull();
    }
  }

  @Test
  public void clusterAccessIsNotInitializedIfProvided() {
    // Given
    try (final JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build();
         MockedConstruction<ClusterAccess> clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class)) {
      assertThat(clusterAccessMockedConstruction.constructed()).asList().isEmpty();
      // When
      jKubeServiceHub.getClusterAccess();
      // Then
      assertThat(clusterAccessMockedConstruction.constructed()).asList().isEmpty();
    }
  }

  @Test
  public void clusterAccessIsInitializedLazily() {
    // Given
    jKubeServiceHubBuilder.clusterAccess(null);
    try (final JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build();
         MockedConstruction<ClusterAccess> clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class)) {
      assertThat(clusterAccessMockedConstruction.constructed()).asList().isEmpty();
      // When
      jKubeServiceHub.getClusterAccess();
      // Then
      assertThat(clusterAccessMockedConstruction.constructed()).asList().hasSize(1);
    }
  }

  @Test
  public void getClientWithOfflineConnectionIsNotAllowed() {
    // Given
    jKubeServiceHubBuilder.offline(true);
    try (final JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When
      final IllegalArgumentException result = assertThrows(IllegalArgumentException.class,
          jKubeServiceHub::getClient);
      // Then
      assertThat(result).hasMessage("Connection to Cluster required. Please check if offline mode is set to false");
    }
  }

  @Test
  public void getApplyServiceWithOfflineThrowsException() {
    // Given
    jKubeServiceHubBuilder.offline(true);
    try (final JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When
      final IllegalArgumentException result = assertThrows(IllegalArgumentException.class,
          jKubeServiceHub::getApplyService);
      // Then
      assertThat(result).hasMessage("Connection to Cluster required. Please check if offline mode is set to false");
    }
  }

  @Test
  public void closeClosesInitializedClient() {
    try (final JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      jKubeServiceHub.getClient();
    }
    // Then
    verify(openShiftClient, times(1)).close();
  }
}
