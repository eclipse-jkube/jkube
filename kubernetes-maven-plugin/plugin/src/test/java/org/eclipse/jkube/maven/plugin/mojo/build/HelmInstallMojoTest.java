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
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.eclipse.jkube.kit.common.util.KubernetesMockServerUtil.prepareMockWebServerExpectationsForAggregatedDiscoveryEndpoints;

@EnableKubernetesMockClient(crud = true)
class HelmInstallMojoTest {
  @TempDir
  private Path projectDir;
  private PrintStream originalPrintStream;
  private ByteArrayOutputStream outputStream;
  private HelmInstallMojo helmInstallMojo;
  private KubernetesClient kubernetesClient;
  private KubernetesMockServer server;

  @BeforeEach
  void setUp() throws Exception {
    originalPrintStream = System.out;
    // Remove after https://github.com/fabric8io/kubernetes-client/issues/6062 is fixed
    prepareMockWebServerExpectationsForAggregatedDiscoveryEndpoints(server);
    outputStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outputStream));
    Helm.create().withDir(projectDir).withName("empty-project").call();
    Path helmChartOutputDir = projectDir.resolve("target").resolve("jkube").resolve("helm");
    Files.createDirectories(helmChartOutputDir.resolve("kubernetes"));
    FileUtils.copyDirectory(projectDir.resolve("empty-project").toFile(), helmChartOutputDir.resolve("kubernetes").toFile());
    Files.write(helmChartOutputDir.resolve("kubernetes").resolve("Chart.yaml"),
      ("\ndependencies:\n" +
        "  - name: the-dependency\n" +
        "    version: 0.1.0\n" +
        "    repository: file://../../../../the-dependency\n").getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.APPEND);
    System.setProperty("jkube.kubernetesTemplate", projectDir.toFile().getAbsolutePath());
    helmInstallMojo = new HelmInstallMojo();
    helmInstallMojo.helm = HelmConfig.builder().chartExtension("tgz")
      .outputDir(helmChartOutputDir.toString())
      .installDependencyUpdate(true)
      .disableOpenAPIValidation(true)
      .build();
    helmInstallMojo.access = ClusterConfiguration.from(kubernetesClient.getConfiguration()).build();
    helmInstallMojo.interpolateTemplateParameters = true;
    helmInstallMojo.settings = new Settings();
    helmInstallMojo.project = new MavenProject();
    helmInstallMojo.project.setVersion("0.1.0");
    helmInstallMojo.project.getBuild()
      .setOutputDirectory(projectDir.resolve("target").resolve("classes").toFile().getAbsolutePath());
    helmInstallMojo.project.getBuild().setDirectory(projectDir.resolve("target").toFile().getAbsolutePath());
    helmInstallMojo.project.setFile(projectDir.resolve("target").toFile());
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalPrintStream);
    System.clearProperty("jkube.kubernetesTemplate");
    helmInstallMojo = null;
  }

  @Test
  void execute_withInstallDependencyUpdateDisabled_shouldThrowException() {
    // Given
    helmInstallMojo.helm = helmInstallMojo.helm.toBuilder()
      .installDependencyUpdate(false)
      .build();
    // When + Then
    assertThatIllegalStateException()
      .isThrownBy(helmInstallMojo::execute)
      .withMessage("An error occurred while checking for chart dependencies. " +
        "You may need to run `helm dependency build` to fetch missing dependencies: found in Chart.yaml, but missing in charts/ directory: the-dependency");
  }

  @Test
  void execute_withHelmDependencyPresent_shouldSucceed() throws Exception {
    // Given
    Helm.create().withName("the-dependency").withDir(projectDir).call();
    // When
    helmInstallMojo.execute();
    // Then
    assertThat(outputStream.toString())
      .contains("NAME : empty-project")
      .contains("NAMESPACE : ")
      .contains("STATUS : deployed")
      .contains("REVISION : 1")
      .contains("LAST DEPLOYED : ")
      .contains("Saving 1 charts")
      .contains("Deleting outdated charts");
  }

  @Test
  void execute_withHelmDependencyAbsent_shouldThrowException() {
    // When + Then
    assertThatIllegalStateException()
      .isThrownBy(() -> helmInstallMojo.execute())
      .withMessageContaining("the-dependency not found");
  }
}
