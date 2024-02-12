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
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.common.JKubeException;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HelmLintMojoTest {

  @TempDir
  private Path projectDir;
  private PrintStream originalPrintStream;
  private ByteArrayOutputStream outputStream;
  private HelmLintMojo helmLintMojo;


  @BeforeEach
  void setUp() throws Exception {
    originalPrintStream = System.out;
    outputStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outputStream));
    helmLintMojo = new HelmLintMojo();
    helmLintMojo.helm = HelmConfig.builder().chartExtension("tgz").build();
    helmLintMojo.interpolateTemplateParameters = true;
    helmLintMojo.settings = new Settings();
    helmLintMojo.project = new MavenProject();
    helmLintMojo.project.setVersion("0.1.0");
    helmLintMojo.project.getBuild()
      .setOutputDirectory(projectDir.resolve("target").resolve("classes").toFile().getAbsolutePath());
    helmLintMojo.project.getBuild().setDirectory(projectDir.resolve("target").toFile().getAbsolutePath());
    helmLintMojo.project.setFile(projectDir.resolve("target").toFile());
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalPrintStream);
    helmLintMojo = null;
  }

  @Test
  void execute_withMissingHelmPackage_shouldThrowException() {
    assertThatThrownBy(helmLintMojo::execute)
      .isInstanceOf(JKubeException.class)
      .hasMessage("Linting failed");
    assertThat(outputStream.toString())
      .contains("Linting empty-project 0.1.0\n")
      .contains("Using packaged file:")
      .contains("[[W]]Error unable to open tarball:");
  }

  @Test
  void execute_withHelmPackage_shouldSucceed() throws Exception {
    Helm.create().withDir(projectDir).withName("empty-project").call()
      .packageIt().withDestination(projectDir.resolve("target").resolve("jkube").resolve("helm").resolve("empty-project").resolve("kubernetes")).call();
    helmLintMojo.execute();
    assertThat(outputStream.toString())
      .contains("Linting empty-project 0.1.0\n")
      .contains("Using packaged file:")
      .contains("[[W]][INFO] Chart.yaml: icon is recommended")
      .contains("Linting successful");
  }
}
