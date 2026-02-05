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
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@EnableKubeAPIServer
class HelmUninstallMojoTest {

  @KubeConfig
  static String kubeConfigYaml;
  @TempDir
  private Path projectDir;
  private KubernetesClient kubernetesClient;
  private PrintStream originalPrintStream;
  private ByteArrayOutputStream outputStream;
  private HelmUninstallMojo helmUninstallMojo;

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
    helmUninstallMojo = new HelmUninstallMojo();
    helmUninstallMojo.helm = HelmConfig.builder().chartExtension("tgz")
      .outputDir(helmChartOutputDir.toString())
      .disableOpenAPIValidation(true)
      .build();
    helmUninstallMojo.access = ClusterConfiguration.from(kubernetesClient.getConfiguration()).build();
    helmUninstallMojo.interpolateTemplateParameters = true;
    helmUninstallMojo.settings = new Settings();
    helmUninstallMojo.project = new MavenProject();
    helmUninstallMojo.project.setVersion("0.1.0");
    helmUninstallMojo.project.getBuild()
      .setOutputDirectory(projectDir.resolve("target").resolve("classes").toFile().getAbsolutePath());
    helmUninstallMojo.project.getBuild().setDirectory(projectDir.resolve("target").toFile().getAbsolutePath());
    helmUninstallMojo.project.setFile(projectDir.resolve("target").toFile());
    helmUninstallMojo.log = new KitLogger.SilentLogger();
  }

  @AfterEach
  void tearDown() {
    kubernetesClient.close();
    System.setOut(originalPrintStream);
    System.clearProperty("jkube.kubernetesTemplate");
    helmUninstallMojo = null;
  }

  @Test
  @DisplayName("Helm release installed on Kuberentes cluster, then uninstall helm release")
  void execute_whenReleasePresent_shouldUninstallChartFromKubernetesCluster() throws MojoExecutionException, MojoFailureException {
    // Given
    helmUninstallMojo.init();
    helmUninstallMojo.jkubeServiceHub.getHelmService().install(helmUninstallMojo.helm);
    // When
    helmUninstallMojo.execute();
    // Then
    assertThat(outputStream.toString())
      .contains("release \"empty-project\" uninstalled");
  }

  @Test
  @DisplayName("Helm Release not installed on Kubernetes cluster, then throw exception")
  void execute_whenReleaseNotPresent_thenThrowException() {
    assertThatIllegalStateException()
      .isThrownBy(() -> helmUninstallMojo.execute())
      .withMessageContaining(" not found");
  }

  @Test
  void execute_whenSkipTrue_shouldDoNothing() throws Exception {
    // Given
    helmUninstallMojo.skip = true;
    helmUninstallMojo.mojoExecution = new MojoExecution(new org.apache.maven.plugin.descriptor.MojoDescriptor());
    helmUninstallMojo.mojoExecution.getMojoDescriptor().setPluginDescriptor(new org.apache.maven.plugin.descriptor.PluginDescriptor());
    helmUninstallMojo.mojoExecution.getMojoDescriptor().setGoal("helm-uninstall");
    helmUninstallMojo.mojoExecution.getMojoDescriptor().getPluginDescriptor().setGoalPrefix("k8s");
    // When
    helmUninstallMojo.execute();
    // Then
    assertThat(outputStream.toString()).contains("`k8s:helm-uninstall` goal is skipped.");
  }
}
