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
package org.eclipse.jkube.kit.resource.helm;

import com.marcnuri.helm.Helm;
import io.fabric8.kubeapitest.junit.EnableKubeAPIServer;
import io.fabric8.kubeapitest.junit.KubeConfig;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.common.util.AsyncUtil;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

@DisplayName("HelmService.test")
@EnableKubeAPIServer
class HelmServiceTestIT {

  @KubeConfig
  static String kubeConfigYaml;
  @TempDir
  private Path tempDir;
  private KubernetesClient kubernetesClient;
  private ByteArrayOutputStream logOutput;
  private HelmService helmService;
  private HelmConfig helmConfig;

  @BeforeEach
  void setUp() throws Exception {
    kubernetesClient = new KubernetesClientBuilder().withConfig(Config.fromKubeconfig(kubeConfigYaml)).build();
    kubernetesClient.apps().deployments().withTimeout(1, TimeUnit.SECONDS).delete();
    kubernetesClient.pods().withTimeout(1, TimeUnit.SECONDS).delete();
    kubernetesClient.configMaps().withTimeout(1, TimeUnit.SECONDS).delete();
    kubernetesClient.secrets().withTimeout(1, TimeUnit.SECONDS).delete();
    logOutput = new ByteArrayOutputStream();
    Helm.create().withDir(tempDir).withName("test-project").call();
    Path helmChartOutputDir = tempDir.resolve("output").resolve("jkube").resolve("helm");
    Files.createDirectories(helmChartOutputDir.resolve("kubernetes"));
    FileUtils.copyDirectory(tempDir.resolve("test-project").toFile(), helmChartOutputDir.resolve("kubernetes").toFile());
    helmService = new HelmService(JKubeConfiguration.builder()
      .project(JavaProject.builder()
        .buildDirectory(tempDir.resolve("target").toFile())
        .build())
      .clusterConfiguration(ClusterConfiguration.from(kubernetesClient.getConfiguration()).build())
      .build(), ResourceServiceConfig.builder()
      .build(), new KitLogger.PrintStreamLogger(new PrintStream(logOutput)));
    helmConfig = HelmConfig.builder()
      .chart("test-project")
      .version("0.1.0")
      .types(Collections.singletonList(HelmConfig.HelmType.KUBERNETES))
      .outputDir(helmChartOutputDir.toString())
      .releaseName("test-project")
      .disableOpenAPIValidation(true)
      .build();
  }

  @AfterEach
  void stopKubernetesServer() {
    kubernetesClient.close();
  }

  @Nested
  class WithChartInstalled {

    @BeforeEach
    void installChart() {
      helmService.install(helmConfig);
    }

    @Test
    @DisplayName("when valid chart but test timeout too low, then throw exception")
    void tooLowTimeout_thenThrowException() {
      // Given
      helmConfig.setTestTimeout(1);
      // Then
      assertThatIllegalStateException()
        .isThrownBy(() -> helmService.test(helmConfig))
        .withMessageContaining("timed out waiting for the condition");
    }

    @Test
    @DisplayName("when valid chart release provided, then log test details after test completion")
    void validChartRelease_thenLogChartTestDetails() {
      // Given
      CompletableFuture<Boolean> helmTest = AsyncUtil.async(() -> {
        helmService.test(helmConfig);
        return true;
      });
      kubernetesClient.pods().withName("test-project-test-connection")
        .waitUntilCondition(Objects::nonNull, 500, TimeUnit.MILLISECONDS);
      // When
      kubernetesClient.pods().withName("test-project-test-connection").editStatus(p -> new PodBuilder(p)
        .editOrNewStatus()
        .withPhase("Succeeded")
        .endStatus()
        .build());
      // Then
      assertThat(helmTest).succeedsWithin(5, TimeUnit.SECONDS);
      assertThat(logOutput)
        .asString()
          .containsSubsequence(
            "[INFO] Testing Helm Chart test-project 0.1.0",
            "[INFO] [[W]]NAME: test-project",
            "[INFO] [[W]]NAMESPACE: ",
            "[INFO] [[W]]STATUS: deployed",
            "[INFO] [[W]]REVISION: 1",
            "[INFO] [[W]]Phase: Succeeded");
    }
  }

  @Test
  @DisplayName("when unknown chart release provided, then throw exception")
  void invalidChartRelease_thenLogChartTestDetails() {
    // Given
    helmConfig.setReleaseName("i-was-never-created");
    // When
    assertThatIllegalStateException()
      .isThrownBy(() -> helmService.test(helmConfig))
      .withMessageContaining("not found");
  }

}
