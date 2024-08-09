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
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.jkube.kit.common.util.KubernetesMockServerUtil.prepareMockWebServerExpectationsForAggregatedDiscoveryEndpoints;

@EnableKubernetesMockClient(crud = true)
class OpenshiftHelmUninstallMojoTest {
  @TempDir
  private Path projectDir;
  private PrintStream originalPrintStream;
  private ByteArrayOutputStream outputStream;
  private OpenshiftHelmUninstallMojo openShiftHelmUninstallMojo;
  private KubernetesClient kubernetesClient;
  private KubernetesMockServer server;

  @BeforeEach
  void setUp() throws Exception {
    originalPrintStream = System.out;
    outputStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outputStream));
    Helm.create().withDir(projectDir).withName("empty-project").call();
    Path helmChartOutputDir = projectDir.resolve("target").resolve("jkube").resolve("helm");
    Files.createDirectories(helmChartOutputDir.resolve("openshift"));
    FileUtils.copyDirectory(projectDir.resolve("empty-project").toFile(), helmChartOutputDir.resolve("openshift").toFile());
    System.setProperty("jkube.kubernetesTemplate", projectDir.toFile().getAbsolutePath());
    openShiftHelmUninstallMojo = new OpenshiftHelmUninstallMojo();
    openShiftHelmUninstallMojo.helm = HelmConfig.builder().chartExtension("tgz")
      .outputDir(helmChartOutputDir.toString())
      .disableOpenAPIValidation(true)
      .build();
    openShiftHelmUninstallMojo.interpolateTemplateParameters = true;
    openShiftHelmUninstallMojo.access = ClusterConfiguration.from(kubernetesClient.getConfiguration()).build();
    openShiftHelmUninstallMojo.settings = new Settings();
    openShiftHelmUninstallMojo.project = new MavenProject();
    openShiftHelmUninstallMojo.project.setVersion("0.1.0");
    openShiftHelmUninstallMojo.project.getBuild()
      .setOutputDirectory(projectDir.resolve("target").resolve("classes").toFile().getAbsolutePath());
    openShiftHelmUninstallMojo.project.getBuild().setDirectory(projectDir.resolve("target").toFile().getAbsolutePath());
    openShiftHelmUninstallMojo.project.setFile(projectDir.resolve("target").toFile());
    // Remove after https://github.com/fabric8io/kubernetes-client/issues/6062 is fixed
    prepareMockWebServerExpectationsForAggregatedDiscoveryEndpoints(server);
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalPrintStream);
    System.clearProperty("jkube.kubernetesTemplate");
    openShiftHelmUninstallMojo = null;
  }

  @Test
  @DisplayName("Helm release installed on Kuberentes cluster, then uninstall helm release")
  void execute_whenReleasePresent_shouldUninstallChartFromKubernetesCluster() throws MojoExecutionException, MojoFailureException, IOException {
    // Given
    openShiftHelmUninstallMojo.init();
    openShiftHelmUninstallMojo.jkubeServiceHub.getHelmService().install(openShiftHelmUninstallMojo.helm);
    // Should be removed once https://github.com/fabric8io/kubernetes-client/issues/6220 gets fixed
    Secret secret = kubernetesClient.secrets().withName("sh.helm.release.v1.empty-project.v1").get();
    server.expect().get().withPath("/api/v1/namespaces/test/secrets?labelSelector=name%3Dempty-project%2Cowner%3Dhelm")
      .andReturn(200, new SecretListBuilder()
        .addToItems(secret)
        .build())
      .once();
    // When
    openShiftHelmUninstallMojo.execute();
    // Then
    assertThat(outputStream.toString())
      .contains("release \"empty-project\" uninstalled");
  }

  @Test
  @DisplayName("Helm Release not installed on Kubernetes cluster, then throw exception")
  void execute_whenReleaseNotPresent_thenThrowException() {
    assertThatIllegalStateException()
      .isThrownBy(() -> openShiftHelmUninstallMojo.execute())
      .withMessageContaining(" not found");
  }
}
