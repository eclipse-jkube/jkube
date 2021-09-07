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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.Properties;

import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
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
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

@SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
public class JKubeServiceHubTest {

  @Mocked
  private KitLogger logger;

  @Mocked
  private ClusterAccess clusterAccess;

  @Mocked
  private OpenShiftClient openShiftClient;

  @Mocked
  private ServiceHub dockerServiceHub;

  @Mocked
  private JKubeConfiguration configuration;

  @Mocked
  private BuildServiceConfig buildServiceConfig;

  private JKubeServiceHub.JKubeServiceHubBuilder commonInit() {
    return JKubeServiceHub.builder()
            .configuration(configuration)
            .clusterAccess(clusterAccess)
            .log(logger)
            .dockerServiceHub(dockerServiceHub)
            .offline(false)
            .buildServiceConfig(buildServiceConfig);
  }

  @Test(expected = NullPointerException.class)
  public void testMissingClusterAccess() {
    JKubeServiceHub.builder()
            .log(logger)
            .build();
  }

  @Test(expected = NullPointerException.class)
  public void testMissingKitLogger() {
    JKubeServiceHub.builder()
            .clusterAccess(clusterAccess)
            .build();
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void testBasicInit() {
    // Given
    // @formatter:off
    new Expectations() {{
      configuration.getProject().getProperties(); result = new Properties();
    }};
    // @formatter:on
    // When
    try (final JKubeServiceHub jKubeServiceHub = JKubeServiceHub.builder()
            .platformMode(RuntimeMode.KUBERNETES)
            .configuration(configuration)
            .log(logger)
            .build()
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
    JKubeServiceHub hub = commonInit()
            .platformMode(RuntimeMode.KUBERNETES)
            .build();
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
    // @formatter:off
    new Expectations() {{
      buildServiceConfig.getJKubeBuildStrategy(); result = null;
    }};
    // @formatter:on
    JKubeServiceHub hub = commonInit()
            .platformMode(RuntimeMode.OPENSHIFT)
            .build();
    // When
    BuildService buildService = hub.getBuildService();
    // Then
    assertThat(buildService)
        .isNotNull()
        .isInstanceOf(OpenshiftBuildService.class);
  }

  @Test
  public void testGetArtifactResolverService() {
    JKubeServiceHub hub = commonInit()
            .platformMode(RuntimeMode.KUBERNETES)
            .build();

    assertThat(hub.getArtifactResolverService()).isNotNull();
  }

  @Test
  public void testGetJibBuildServiceInKubernetes() {
    // Given
    // @formatter:off
    new Expectations() {{
      buildServiceConfig.getJKubeBuildStrategy(); result = JKubeBuildStrategy.jib;
    }};
    // @formatter:on
    JKubeServiceHub hub = commonInit()
            .platformMode(RuntimeMode.KUBERNETES)
            .build();
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
    JKubeServiceHub hub = commonInit()
            .platformMode(RuntimeMode.KUBERNETES)
            .build();
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
    // @formatter:off
    new Expectations() {{
      openShiftClient.isAdaptable(OpenShiftClient.class); result = false;
    }};
    // @formatter:on
    JKubeServiceHub hub = commonInit()
            .platformMode(RuntimeMode.OPENSHIFT)
            .build();
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
    // @formatter:off
    new Expectations() {{
      openShiftClient.isAdaptable(OpenShiftClient.class); result = true;
    }};
    // @formatter:on
    JKubeServiceHub hub = commonInit()
            .platformMode(RuntimeMode.OPENSHIFT)
            .build();
    // When
    final UndeployService result = hub.getUndeployService();
    // Then
    assertThat(result)
        .isNotNull()
        .isInstanceOf(OpenshiftUndeployService.class);
  }

  @Test
  public void testGetPortForwardService() {
    // Given
    JKubeServiceHub hub = commonInit()
            .platformMode(RuntimeMode.KUBERNETES)
            .build();

    // When
    final PortForwardService portForwardService = hub.getPortForwardService();

    // Then
    assertThat(portForwardService).isNotNull();
  }

  @Test
  public void testGetDebugService() {
    // Given
    JKubeServiceHub hub = commonInit()
            .platformMode(RuntimeMode.KUBERNETES)
            .build();

    // When
    final DebugService debugService = hub.getDebugService();

    // Then
    assertThat(debugService).isNotNull();
  }

  @Test
  public void testBasicInitWithOffline() {
    // Given + When
    try (final JKubeServiceHub jKubeServiceHub = commonInit().platformMode(RuntimeMode.KUBERNETES).offline(true).build()) {
      // Then
      assertThat(jKubeServiceHub)
          .isNotNull()
          .hasFieldOrPropertyWithValue("client", null);
    }
  }

  @Test
  public void testAccessServiceWithNonInitializedClientThrowsException() {
    // Given + When
    try (final JKubeServiceHub jKubeServiceHub = commonInit().platformMode(RuntimeMode.KUBERNETES).offline(true).build()) {
      // Then
      assertThrows(IllegalArgumentException.class, jKubeServiceHub::getApplyService);
    }
  }
}