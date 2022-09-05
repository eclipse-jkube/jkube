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
package org.eclipse.jkube.quarkus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Stream;

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

class QuarkusModeTest {

  private File target;
  private Properties projectProperties;
  private JavaProject project;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    target = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    projectProperties = new Properties();
    project = JavaProject.builder()
      .outputDirectory(target)
      .buildDirectory(target)
      .properties(projectProperties)
      .build();
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("from")
  class From {

    @Test
    @DisplayName("with no settings, should return FAST_JAR")
    void withNoSettingsAndNoFiles_shouldReturnFastJar() {
      // When
      final QuarkusMode result = QuarkusMode.from(project);
      // Then
      assertThat(result).isEqualTo(QuarkusMode.FAST_JAR);
    }

    @Test
    @DisplayName("with no settings and native binary artifact, should return NATIVE")
    void withNoSettingsAndNativeBinary_shouldReturnNative() throws IOException {
      // Given
      Files.createFile(target.toPath().resolve("-runner"));
      // When
      final QuarkusMode result = QuarkusMode.from(project);
      // Then
      assertThat(result).isEqualTo(QuarkusMode.NATIVE);
    }

    @Test
    @DisplayName("with custom suffix and native binary artifact, should return NATIVE")
    void withCustomSuffixAndNativeBinary_shouldReturnNative() throws IOException {
      // Given
      projectProperties.put("quarkus.package.runner-suffix", "-coyote");
      Files.createFile(target.toPath().resolve("-coyote"));
      // When
      final QuarkusMode result = QuarkusMode.from(project);
      // Then
      assertThat(result).isEqualTo(QuarkusMode.NATIVE);
    }

    @Test
    @DisplayName("with no settings and runner jar artifact, should return UBER_JAR")
    void withNoSettingsAndRunnerJar_shouldReturnUberJar() throws IOException {
      // Given
      Files.createFile(target.toPath().resolve("-runner.jar"));
      // When
      final QuarkusMode result = QuarkusMode.from(project);
      // Then
      assertThat(result).isEqualTo(QuarkusMode.UBER_JAR);
    }

    @Test
    @DisplayName("with custom suffix and runner jar artifact, should return UBER_JAR")
    void withCustomSuffixAndRunnerJar_shouldReturnUberJar() throws IOException {
      // Given
      projectProperties.put("quarkus.package.runner-suffix", "-coyote");
      Files.createFile(target.toPath().resolve("-coyote.jar"));
      // When
      final QuarkusMode result = QuarkusMode.from(project);
      // Then
      assertThat(result).isEqualTo(QuarkusMode.UBER_JAR);
    }

    @Test
    @DisplayName("with no settings and runner jar artifact and lib directory, should return LEGACY_JAR")
    void withNoSettingsAndRunnerJarAndLib_shouldReturnLegacyJar() throws IOException {
      // Given
      Files.createFile(target.toPath().resolve("-runner.jar"));
      Files.createDirectory(target.toPath().resolve("lib"));
      // When
      final QuarkusMode result = QuarkusMode.from(project);
      // Then
      assertThat(result).isEqualTo(QuarkusMode.LEGACY_JAR);
    }

    @DisplayName("with custom quarkus.package.type=")
    @ParameterizedTest(name = "''{0}'' should return ''{1}''")
    @MethodSource("packageTypes")
    void withPackageType(String packageType, QuarkusMode expectedMode) {
      // Given
      projectProperties.put("quarkus.package.type", packageType);
      // When
      final QuarkusMode result = QuarkusMode.from(project);
      // Then
      assertThat(result)
        .isEqualTo(expectedMode);
    }

    Stream<Arguments> packageTypes() {
      return Stream.of(
        Arguments.of("native", QuarkusMode.NATIVE),
        Arguments.of("legacy-jar", QuarkusMode.LEGACY_JAR),
        Arguments.of("fast-jar", QuarkusMode.FAST_JAR),
        Arguments.of("uber-jar", QuarkusMode.UBER_JAR)
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
    void mode(QuarkusMode mode, boolean isFatJar) {
      // When
      final boolean result = mode.isFatJar();
      // Then
      assertThat(result)
        .isEqualTo(isFatJar);
    }

    Stream<Arguments> modes() {
      return Stream.of(
        Arguments.of(QuarkusMode.NATIVE, false),
        Arguments.of(QuarkusMode.LEGACY_JAR, false),
        Arguments.of(QuarkusMode.FAST_JAR, false),
        Arguments.of(QuarkusMode.UBER_JAR, true)
      );
    }
  }
}
