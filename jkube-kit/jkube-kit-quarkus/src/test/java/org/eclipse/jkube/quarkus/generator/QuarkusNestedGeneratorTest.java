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
package org.eclipse.jkube.quarkus.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Stream;

import org.eclipse.jkube.generator.api.GeneratorConfig;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class QuarkusNestedGeneratorTest {

  private File target;
  private Properties projectProperties;
  private GeneratorContext context;
  private GeneratorConfig config;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    target = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    projectProperties = new Properties();
    context = GeneratorContext.builder()
      .project(JavaProject.builder()
        .outputDirectory(target)
        .buildDirectory(target)
        .properties(projectProperties)
        .build())
      .build();
    config = new GeneratorConfig(projectProperties, "quarkus", context.getConfig());
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("from")
  class From {

    @Test
    @DisplayName("with no settings, should return FastJarGenerator")
    void withNoSettingsAndNoFiles_shouldReturnFastJar() {
      // When
      final QuarkusNestedGenerator result = QuarkusNestedGenerator.from(context, config);
      // Then
      assertThat(result).isInstanceOf(FastJarGenerator.class);
    }

    @Test
    @DisplayName("with no settings and native binary artifact, should return NativeGenerator")
    void withNoSettingsAndNativeBinary_shouldReturnNative() throws IOException {
      // Given
      Files.createFile(target.toPath().resolve("-runner"));
      // When
      final QuarkusNestedGenerator result = QuarkusNestedGenerator.from(context, config);
      // Then
      assertThat(result).isInstanceOf(NativeGenerator.class);
    }

    @Test
    @DisplayName("with custom suffix and native binary artifact, should return NativeGenerator")
    void withCustomSuffixAndNativeBinary_shouldReturnNative() throws IOException {
      // Given
      projectProperties.put("quarkus.package.runner-suffix", "-coyote");
      Files.createFile(target.toPath().resolve("-coyote"));
      // When
      final QuarkusNestedGenerator result = QuarkusNestedGenerator.from(context, config);
      // Then
      assertThat(result).isInstanceOf(NativeGenerator.class);
    }

    @Test
    @DisplayName("with no settings and runner jar artifact, should return UberJarGenerator")
    void withNoSettingsAndRunnerJar_shouldReturnUberJar() throws IOException {
      // Given
      Files.createFile(target.toPath().resolve("-runner.jar"));
      // When
      final QuarkusNestedGenerator result = QuarkusNestedGenerator.from(context, config);
      // Then
      assertThat(result).isInstanceOf(UberJarGenerator.class);
    }

    @Test
    @DisplayName("with custom suffix and runner jar artifact, should return UberJarGenerator")
    void withCustomSuffixAndRunnerJar_shouldReturnUberJar() throws IOException {
      // Given
      projectProperties.put("quarkus.package.runner-suffix", "-coyote");
      Files.createFile(target.toPath().resolve("-coyote.jar"));
      // When
      final QuarkusNestedGenerator result = QuarkusNestedGenerator.from(context, config);
      // Then
      assertThat(result).isInstanceOf(UberJarGenerator.class);
    }

    @Test
    @DisplayName("with no settings and runner jar artifact and lib directory, should return LegacyJarGenerator")
    void withNoSettingsAndRunnerJarAndLib_shouldReturnLegacyJar() throws IOException {
      // Given
      Files.createFile(target.toPath().resolve("-runner.jar"));
      Files.createDirectory(target.toPath().resolve("lib"));
      // When
      final QuarkusNestedGenerator result = QuarkusNestedGenerator.from(context, config);
      // Then
      assertThat(result).isInstanceOf(LegacyJarGenerator.class);
    }

    @DisplayName("with custom quarkus.package.type=")
    @ParameterizedTest(name = "''{0}'' should return ''{1}''")
    @MethodSource("packageTypes")
    void withPackageType(String packageType, Class<? extends QuarkusNestedGenerator> expectedNestedGeneratorClass) {
      // Given
      projectProperties.put("quarkus.package.type", packageType);
      // When
      final QuarkusNestedGenerator result = QuarkusNestedGenerator.from(context, config);
      // Then
      assertThat(result)
        .isInstanceOf(expectedNestedGeneratorClass);
    }

    Stream<Arguments> packageTypes() {
      return Stream.of(
        Arguments.of("native", NativeGenerator.class),
        Arguments.of("legacy-jar", LegacyJarGenerator.class),
        Arguments.of("fast-jar", FastJarGenerator.class),
        Arguments.of("uber-jar", UberJarGenerator.class),
        Arguments.of("unrecognized", FastJarGenerator.class)
      );
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("isFatJar")
  class IsFatJar {

    @DisplayName("for mode")
    @ParameterizedTest(name = "''{0}'' should return ''{1}''")
    @MethodSource("modes")
    void mode(String packageType, boolean isFatJar) {
      // Given
      projectProperties.put("quarkus.package.type", packageType);
      // When
      final boolean result = QuarkusNestedGenerator.from(context, config).isFatJar();
      // Then
      assertThat(result)
        .isEqualTo(isFatJar);
    }

    Stream<Arguments> modes() {
      return Stream.of(
        Arguments.of("native", false),
        Arguments.of("legacy-jar", false),
        Arguments.of("fast-jar", false),
        Arguments.of("uber-jar", true)
      );
    }
  }
}
