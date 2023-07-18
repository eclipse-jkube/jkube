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
package org.eclipse.jkube.quarkus.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class QuarkusGeneratorExposedPortsTest {

  private GeneratorContext ctx;

  private File target;
  private Properties projectProperties;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    target = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    projectProperties = new Properties();
    ctx = GeneratorContext.builder()
      .logger(new KitLogger.SilentLogger())
      .project(JavaProject.builder()
        .version("1.33.7-SNAPSHOT")
        .baseDirectory(target)
        .buildDirectory(target.getAbsoluteFile())
        .properties(projectProperties)
        .outputDirectory(target)
        .build())
      .config(new ProcessorConfig())
      .strategy(JKubeBuildStrategy.s2i)
      .build();
  }

  @Test
  void withDefaults_shouldAddDefaults() throws IOException {
    // Given
    withFastJarInTarget();
    // When
    final List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), false);
    // Then
    assertThat(result).singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getPorts)
        .asList()
        .containsExactly("8080", "8778", "9779");
  }

  @Test
  void withDefaultsInNative_shouldAddDefaultsForNative() throws IOException {
    // Given
    withNativeBinaryInTarget();
    // When
    final List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), false);
    // Then
    assertThat(result).singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getPorts)
        .asList()
        .containsExactly("8080");
  }

  @Test
  void withApplicationProperties_shouldAddConfigured() throws IOException {
    // Given
    withFastJarInTarget();
    ctx = ctx.toBuilder().project(ctx.getProject().toBuilder()
        .compileClassPathElement(QuarkusGeneratorExposedPortsTest.class.getResource("/generator-extract-ports").getPath())
        .build())
      .build();
    // When
    final List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), false);
    // Then
    assertThat(result).singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getPorts)
        .asList()
        .containsExactly("1337", "8778", "9779");
  }

  @Test
  void withApplicationPropertiesAndProfile_shouldAddConfiguredProfile() throws IOException {
    // Given
    withFastJarInTarget();
    projectProperties.put("quarkus.profile", "dev");
    ctx = ctx.toBuilder().project(ctx.getProject().toBuilder()
        .compileClassPathElement(QuarkusGeneratorExposedPortsTest.class.getResource("/generator-extract-ports").getPath())
        .build())
      .build();
    // When
    final List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), false);
    // Then
    assertThat(result).singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getPorts)
        .asList()
        .containsExactly("1337", "8778", "9779");
  }

  private void withNativeBinaryInTarget() throws IOException {
    Files.createFile(target.toPath().resolve("sample-runner"));
  }

  private void withFastJarInTarget() throws IOException {
    final Path quarkusApp = target.toPath().resolve("quarkus-app");
    Files.createDirectory(quarkusApp);
    Files.createDirectory(quarkusApp.resolve("app"));
    Files.createDirectory(quarkusApp.resolve("lib"));
    Files.createDirectory(quarkusApp.resolve("quarkus"));
    Files.createFile(quarkusApp.resolve("quarkus-run.jar"));
  }
}
