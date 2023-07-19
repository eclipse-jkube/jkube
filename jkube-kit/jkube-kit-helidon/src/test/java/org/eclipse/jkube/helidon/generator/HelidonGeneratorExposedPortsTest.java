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
package org.eclipse.jkube.helidon.generator;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class HelidonGeneratorExposedPortsTest {
  private GeneratorContext ctx;

  private File target;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    target = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    ctx = GeneratorContext.builder()
      .logger(new KitLogger.SilentLogger())
      .project(JavaProject.builder()
        .version("1.33.7-SNAPSHOT")
        .baseDirectory(target)
        .buildDirectory(target.getAbsoluteFile())
        .properties(new Properties())
        .outputDirectory(target)
        .build())
      .config(new ProcessorConfig())
      .strategy(JKubeBuildStrategy.s2i)
      .build();
  }

  @Test
  void withDefaults_shouldAddDefaults() throws IOException {
    // Given
    whenStandardJarInTarget();
    // When
    final List<ImageConfiguration> result = new HelidonGenerator(ctx).customize(new ArrayList<>(), true);
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
    withNativeExtensionDependencyInTarget();
    // When
    final List<ImageConfiguration> result = new HelidonGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertThat(result).singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getPorts)
        .asList()
        .containsExactly("8080");
  }

  @Test
  void withApplicationYaml_shouldAddConfigured() throws IOException {
    // Given
    whenStandardJarInTarget();
    ctx = ctx.toBuilder().project(ctx.getProject().toBuilder()
        .compileClassPathElement(HelidonGeneratorExposedPortsTest.class.getResource("/custom-port-configuration").getPath())
        .build())
      .build();
    // When
    final List<ImageConfiguration> result = new HelidonGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertThat(result).singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getPorts)
        .asList()
        .containsExactly("1337", "8778", "9779");
  }

  private void whenStandardJarInTarget() throws IOException {
    Files.createFile(target.toPath().resolve("sample.jar"));
    final Path lib = target.toPath().resolve("libs");
    Files.createDirectory(lib);
    Files.createFile(lib.resolve("dependency.jar"));
  }

  private void withNativeExtensionDependencyInTarget() {
    ctx = ctx.toBuilder().project(ctx.getProject().toBuilder()
        .dependency(Dependency.builder()
          .groupId("io.helidon.integrations.graal")
          .artifactId("helidon-graal-native-image-extension")
          .build()).build()).build();
  }

  private void withNativeBinaryInTarget() throws IOException {
    Files.createFile(target.toPath().resolve("sample-runner"));
  }
}
