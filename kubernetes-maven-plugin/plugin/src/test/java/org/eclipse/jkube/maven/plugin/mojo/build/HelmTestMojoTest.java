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
package org.eclipse.jkube.maven.plugin.mojo.build;

import com.marcnuri.helm.Helm;
import io.fabric8.kubeapitest.junit.EnableKubeAPIServer;
import io.fabric8.kubeapitest.junit.KubeConfig;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.common.util.AsyncUtil;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@EnableKubeAPIServer
class HelmTestMojoTest {

  @KubeConfig
  static String kubeConfigYaml;
  @TempDir
  private Path projectDir;
  private KubernetesClient kubernetesClient;
  private PrintStream originalPrintStream;
  private ByteArrayOutputStream outputStream;
  private HelmTestMojo helmTestMojo;

  @BeforeEach
  void setUp() throws Exception {
    kubernetesClient = new KubernetesClientBuilder().withConfig(Config.fromKubeconfig(kubeConfigYaml)).build();
    kubernetesClient.apps().deployments().withTimeout(1, TimeUnit.SECONDS).delete();
    kubernetesClient.pods().withTimeout(1, TimeUnit.SECONDS).delete();
    kubernetesClient.configMaps().withTimeout(1, TimeUnit.SECONDS).delete();
    kubernetesClient.secrets().withTimeout(1, TimeUnit.SECONDS).delete();
    originalPrintStream = System.out;
    outputStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outputStream));
    Helm.create().withDir(projectDir).withName("empty-project").call();
    Path helmChartOutputDir = projectDir.resolve("target").resolve("jkube").resolve("helm");
    Files.createDirectories(helmChartOutputDir.resolve("kubernetes"));
    FileUtils.copyDirectory(projectDir.resolve("empty-project").toFile(), helmChartOutputDir.resolve("kubernetes").toFile());
    System.setProperty("jkube.kubernetesTemplate", projectDir.toFile().getAbsolutePath());
    helmTestMojo = new HelmTestMojo();
    helmTestMojo.helm = HelmConfig.builder()
      .outputDir(helmChartOutputDir.toString())
      .disableOpenAPIValidation(true)
      .build();
    helmTestMojo.log = new KitLogger.SilentLogger();
    helmTestMojo.access = ClusterConfiguration.from(kubernetesClient.getConfiguration()).build();
    helmTestMojo.interpolateTemplateParameters = true;
    helmTestMojo.settings = new Settings();
    helmTestMojo.project = new MavenProject();
    helmTestMojo.project.setVersion("0.1.0");
    helmTestMojo.project.getBuild()
      .setOutputDirectory(projectDir.resolve("target").resolve("classes").toFile().getAbsolutePath());
    helmTestMojo.project.getBuild().setDirectory(projectDir.resolve("target").toFile().getAbsolutePath());
    helmTestMojo.project.setFile(projectDir.resolve("target").toFile());
  }

  @AfterEach
  void tearDown() {
    kubernetesClient.close();
    System.setOut(originalPrintStream);
    System.clearProperty("jkube.kubernetesTemplate");
    helmTestMojo = null;
  }

  @Test
  @DisplayName("Helm release installed on Kubernetes cluster, then test helm release")
  void execute_whenReleasePresent_shouldTestChartFromKubernetesCluster() throws MojoFailureException {
    // Given
    helmTestMojo.init();
    helmTestMojo.jkubeServiceHub.getHelmService().install(helmTestMojo.helm);
    // When
    CompletableFuture<Boolean> helmTest = AsyncUtil.async(() -> {
      helmTestMojo.execute();
      return true;
    });

    kubernetesClient.pods().withName("empty-project-test-connection")
      .waitUntilCondition(Objects::nonNull, 5, TimeUnit.SECONDS);
    kubernetesClient.pods().withName("empty-project-test-connection").editStatus(p -> new PodBuilder(p)
      .editOrNewStatus()
      .withPhase("Succeeded")
      .endStatus()
      .build());
    // Then
    assertThat(helmTest).succeedsWithin(5, TimeUnit.SECONDS);
    assertThat(outputStream.toString())
      .contains("[INFO] Testing Helm Chart empty-project 0.1.0")
      .contains("[INFO] [[W]]NAME: empty-project")
      .contains("[INFO] [[W]]STATUS: deployed")
      .contains("[INFO] [[W]]REVISION: 1")
      .contains("[INFO] [[W]]Phase: Succeeded");
  }

  @Test
  @DisplayName("Helm Release not installed on Kubernetes cluster, then throw exception")
  void execute_whenReleaseNotPresent_thenThrowException() {
    assertThatIllegalStateException()
      .isThrownBy(() -> helmTestMojo.execute())
      .withMessageContaining(" not found");
  }

  @Test
  void execute_whenSkipTrue_shouldDoNothing() throws Exception {
    // Given
    helmTestMojo.skip = true;
    helmTestMojo.mojoExecution = new MojoExecution(new org.apache.maven.plugin.descriptor.MojoDescriptor());
    helmTestMojo.mojoExecution.getMojoDescriptor().setPluginDescriptor(new org.apache.maven.plugin.descriptor.PluginDescriptor());
    helmTestMojo.mojoExecution.getMojoDescriptor().setGoal("helm-test");
    helmTestMojo.mojoExecution.getMojoDescriptor().getPluginDescriptor().setGoalPrefix("k8s");

    // When
    helmTestMojo.execute();

    // Then
    assertThat(outputStream.toString()).contains("`k8s:helm-test` goal is skipped.");
  }
}
