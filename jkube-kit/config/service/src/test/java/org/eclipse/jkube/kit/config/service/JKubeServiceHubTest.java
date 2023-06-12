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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.jkube.kit.config.resource.RuntimeMode.KUBERNETES;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unused"})
class JKubeServiceHubTest {

  private JKubeServiceHub.JKubeServiceHubBuilder jKubeServiceHubBuilder;
  private OpenShiftClient openShiftClient;

  @BeforeEach
  void setUp() {
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
  void buildWithMissingPlatformModeThrowsException() {
    // Given
    jKubeServiceHubBuilder.platformMode(null);
    // When + Then
    assertThatNullPointerException()
        .isThrownBy(jKubeServiceHubBuilder::build)
        .withMessageContaining("platformMode is a required parameter");
  }

  @Test
  void buildWithMissingConfigurationThrowsException() {
    // Given
    jKubeServiceHubBuilder.configuration(null);
    // When + Then
    assertThatNullPointerException()
        .isThrownBy(jKubeServiceHubBuilder::build)
        .withMessageContaining("JKubeConfiguration is required");
  }

  @Test
  void buildWithMissingKitLoggerThrowsException() {
    // Given
    jKubeServiceHubBuilder.log(null);
    // When + Then
    assertThatNullPointerException()
        .isThrownBy(jKubeServiceHubBuilder::build)
        .withMessageContaining("log is a required parameter");
  }

  @Test
  void basicInit() {
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
  void getBuildServiceInKubernetes() {
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
  void getBuildServiceInOpenShift() {
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
  void getJibBuildServiceInKubernetes() {
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
  void getUndeployServiceInKubernetes() {
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
  void getUndeployServiceInOpenShiftWithInvalidClient() {
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
  void getUndeployServiceInOpenShiftWithValidClient() {
    // Given
    jKubeServiceHubBuilder.platformMode(RuntimeMode.OPENSHIFT);
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      when(openShiftClient.hasApiGroup("openshift.io", false)).thenReturn(true);
      // When
      final UndeployService result = jKubeServiceHub.getUndeployService();
      // Then
      assertThat(result)
          .isNotNull()
          .isInstanceOf(OpenshiftUndeployService.class);
    }
  }

  @Test
  void getPortForwardService() {
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When
      final PortForwardService portForwardService = jKubeServiceHub.getPortForwardService();
      // Then
      assertThat(portForwardService).isNotNull();
    }
  }

  @Test
  void getDebugService() {
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When
      final DebugService debugService = jKubeServiceHub.getDebugService();
      // Then
      assertThat(debugService).isNotNull();
    }
  }

  @Test
  void getHelmService() {
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When
      final HelmService helmService = jKubeServiceHub.getHelmService();
      // Then
      assertThat(helmService).isNotNull();
    }
  }

  @Test
  void getMigrateService() {
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When
      final MigrateService migrateService = jKubeServiceHub.getMigrateService();
      // Then
      assertThat(migrateService).isNotNull();
    }
  }

  @Test
  void getResourceService() {
    // Given
    jKubeServiceHubBuilder.resourceService(new LazyBuilder<>(hub -> mock(ResourceService.class)));
    try (JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When
      final ResourceService resourceService = jKubeServiceHub.getResourceService();
      // Then
      assertThat(resourceService).isNotNull();
    }
  }

  @Test
  void clusterAccessIsNotInitializedIfProvided() {
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
  void clusterAccessIsInitializedLazily() {
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
  void getClientWithOfflineConnectionIsNotAllowed() {
    // Given
    jKubeServiceHubBuilder.offline(true);
    try (final JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When + Then
      assertThatIllegalArgumentException()
          .isThrownBy(jKubeServiceHub::getClient)
          .withMessage("Connection to Cluster required. Please check if offline mode is set to false");
    }
  }

  @Test
  void getApplyServiceWithOfflineThrowsException() {
    // Given
    jKubeServiceHubBuilder.offline(true);
    try (final JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      // When + Then
      assertThatIllegalArgumentException()
          .isThrownBy(jKubeServiceHub::getApplyService)
          .withMessage("Connection to Cluster required. Please check if offline mode is set to false");
    }
  }

  @Test
  void closeClosesInitializedClient() {
    try (final JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      jKubeServiceHub.getClient();
    }
    // Then
    verify(openShiftClient, times(1)).close();
  }
}
