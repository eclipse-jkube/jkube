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
import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
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

class KubernetesHelmDependencyUpdateMojoTest {

  @TempDir
  private Path projectDir;
  private PrintStream originalPrintStream;
  private ByteArrayOutputStream outputStream;
  private HelmDependencyUpdateMojo helmDependencyUpdateMojo;


  @BeforeEach
  void setUp() throws Exception {
    originalPrintStream = System.out;
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
    helmDependencyUpdateMojo = new HelmDependencyUpdateMojo();
    helmDependencyUpdateMojo.helm = HelmConfig.builder().chartExtension("tgz").outputDir(helmChartOutputDir.toString()).build();
    helmDependencyUpdateMojo.interpolateTemplateParameters = true;
    helmDependencyUpdateMojo.settings = new Settings();
    helmDependencyUpdateMojo.project = new MavenProject();
    helmDependencyUpdateMojo.project.setVersion("0.1.0");
    helmDependencyUpdateMojo.project.getBuild()
        .setOutputDirectory(projectDir.resolve("target").resolve("classes").toFile().getAbsolutePath());
    helmDependencyUpdateMojo.project.getBuild().setDirectory(projectDir.resolve("target").toFile().getAbsolutePath());
    helmDependencyUpdateMojo.project.setFile(projectDir.resolve("target").toFile());
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalPrintStream);
    System.clearProperty("jkube.kubernetesTemplate");
    helmDependencyUpdateMojo = null;
  }

  @Test
  void execute_withInvalidHelmDependency_shouldThrowException() {
    assertThatIllegalStateException()
        .isThrownBy(helmDependencyUpdateMojo::execute)
        .withMessageContaining("the-dependency not found");
  }

  @Test
  void execute_withHelmDependencyPresent_shouldSucceed() throws Exception {
    // Given
    Helm.create().withName("the-dependency").withDir(projectDir).call();
    // When
    helmDependencyUpdateMojo.execute();
    // Then
    assertThat(outputStream.toString())
        .contains("Running Helm Dependency Upgrade empty-project 0.1.0")
        .contains("Saving 1 charts")
        .contains("Deleting outdated charts");
  }
}
