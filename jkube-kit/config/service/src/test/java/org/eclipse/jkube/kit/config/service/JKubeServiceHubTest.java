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
package org.eclipse.jkube.kit.config.service;

import io.fabric8.kubernetes.api.model.APIGroupBuilder;
import io.fabric8.kubernetes.api.model.APIGroupListBuilder;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.build.service.docker.DockerServiceHub;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.common.service.MigrateService;
import org.eclipse.jkube.kit.common.util.LazyBuilder;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ResourceService;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.kubernetes.DockerBuildService;
import org.eclipse.jkube.kit.config.service.kubernetes.JibImageBuildService;
import org.eclipse.jkube.kit.config.service.kubernetes.KubernetesUndeployService;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftBuildService;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftUndeployService;

import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.resource.helm.HelmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.jkube.kit.config.resource.RuntimeMode.KUBERNETES;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unused"})
@EnableKubernetesMockClient(crud = true)
class JKubeServiceHubTest {
  @TempDir
  Path temporaryFolder;

  private JKubeServiceHub.JKubeServiceHubBuilder jKubeServiceHubBuilder;
  KubernetesMockServer kubernetesMockServer;
  OpenShiftClient openShiftClient;

  @BeforeEach
  void setUp() {
    jKubeServiceHubBuilder = JKubeServiceHub.builder()
        .platformMode(KUBERNETES)
        .configuration(JKubeConfiguration.builder()
          .clusterConfiguration(ClusterConfiguration.from(openShiftClient.getConfiguration()).build())
          .project(JavaProject.builder()
            .baseDirectory(temporaryFolder.toFile())
            .build())
          .build())
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
          .isInstanceOf(JibImageBuildService.class);
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
      kubernetesMockServer.setUnsupported("openshift.io");
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
      kubernetesMockServer.expect().get().withPath("/apis")
        .andReturn(HTTP_OK, new APIGroupListBuilder().addToGroups(new APIGroupBuilder().withName("apps.openshift.io").build()).build())
        .once();
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
    KubernetesClient kubernetesClient;
    try (final JKubeServiceHub jKubeServiceHub = jKubeServiceHubBuilder.build()) {
      kubernetesClient = jKubeServiceHub.getClient();
    }
    // Then
    assertThat(kubernetesClient)
      .isNotNull()
      .extracting(Client::getHttpClient)
      .extracting(HttpClient::isClosed)
      .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
      .isTrue();
  }
}
