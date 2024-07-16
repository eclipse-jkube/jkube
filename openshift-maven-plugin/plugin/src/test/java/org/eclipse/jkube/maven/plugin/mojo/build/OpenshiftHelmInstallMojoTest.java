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
class OpenshiftHelmInstallMojoTest {
  @TempDir
  private Path projectDir;
  private PrintStream originalPrintStream;
  private ByteArrayOutputStream outputStream;
  private OpenshiftHelmInstallMojo openShiftHelmInstallMojo;
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
    Files.write(helmChartOutputDir.resolve("openshift").resolve("Chart.yaml"),
      ("\ndependencies:\n" +
        "  - name: the-dependency\n" +
        "    version: 0.1.0\n" +
        "    repository: file://../../../../the-dependency\n").getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.APPEND);
    System.setProperty("jkube.kubernetesTemplate", projectDir.toFile().getAbsolutePath());
    openShiftHelmInstallMojo = new OpenshiftHelmInstallMojo();
    openShiftHelmInstallMojo.helm = HelmConfig.builder().chartExtension("tgz")
      .outputDir(helmChartOutputDir.toString())
      .installDependencyUpdate(true)
      .disableOpenAPIValidation(true)
      .build();
    openShiftHelmInstallMojo.interpolateTemplateParameters = true;
    openShiftHelmInstallMojo.access = ClusterConfiguration.from(kubernetesClient.getConfiguration()).build();
    openShiftHelmInstallMojo.settings = new Settings();
    openShiftHelmInstallMojo.project = new MavenProject();
    openShiftHelmInstallMojo.project.setVersion("0.1.0");
    openShiftHelmInstallMojo.project.getBuild()
      .setOutputDirectory(projectDir.resolve("target").resolve("classes").toFile().getAbsolutePath());
    openShiftHelmInstallMojo.project.getBuild().setDirectory(projectDir.resolve("target").toFile().getAbsolutePath());
    openShiftHelmInstallMojo.project.setFile(projectDir.resolve("target").toFile());
    // Remove after https://github.com/fabric8io/kubernetes-client/issues/6062 is fixed
    prepareMockWebServerExpectationsForAggregatedDiscoveryEndpoints(server);
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalPrintStream);
    System.clearProperty("jkube.kubernetesTemplate");
    openShiftHelmInstallMojo = null;
  }

  @Test
  void execute_withInstallDependencyUpdateDisabled_shouldThrowException() {
    // Given
    openShiftHelmInstallMojo.helm = openShiftHelmInstallMojo.helm.toBuilder()
      .installDependencyUpdate(false)
      .build();
    // When + Then
    assertThatIllegalStateException()
      .isThrownBy(openShiftHelmInstallMojo::execute)
      .withMessage("An error occurred while checking for chart dependencies. " +
        "You may need to run `helm dependency build` to fetch missing dependencies: found in Chart.yaml, but missing in charts/ directory: the-dependency");
  }

  @Test
  void execute_withHelmDependencyPresent_shouldSucceed() throws Exception {
    // Given
    Helm.create().withName("the-dependency").withDir(projectDir).call();
    // When
    openShiftHelmInstallMojo.execute();
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
      .isThrownBy(() -> openShiftHelmInstallMojo.execute())
      .withMessageContaining("the-dependency not found");
  }
}
