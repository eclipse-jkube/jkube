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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.common.JavaProject;

import mockit.Expectations;
import mockit.Mocked;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class QuarkusModeTest {

  @Mocked
  private JavaProject project;

  private File target;
  private List<String> compileClassPathElements;
  private Properties projectProperties;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    target = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    compileClassPathElements = new ArrayList<>();
    projectProperties = new Properties();
    // @formatter:off
    new Expectations() {{
      project.getCompileClassPathElements(); result = compileClassPathElements;
      project.getOutputDirectory(); result = target;
      project.getBuildDirectory(); result = target; minTimes = 0;
      project.getProperties(); result = projectProperties;
    }};
    // @formatter:on
  }

  @Test
  void from_withNoSettingsAndNoFiles_shouldReturnFastJar() {
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result).isEqualTo(QuarkusMode.FAST_JAR);
  }

  @Test
  void from_withNoSettingsAndNativeBinary_shouldReturnNative() throws IOException {
    // Given
    new File(target, "-runner").createNewFile();
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result).isEqualTo(QuarkusMode.NATIVE);
  }

  @Test
  void from_withCustomSuffixAndNativeBinary_shouldReturnNative() throws IOException {
    // Given
    projectProperties.put("quarkus.package.runner-suffix", "-coyote");
    new File(target, "-coyote").createNewFile();
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result).isEqualTo(QuarkusMode.NATIVE);
  }

  @Test
  void from_withNoSettingsAndRunnerJar_shouldReturnUberJar() throws IOException {
    // Given
    new File(target, "-runner.jar").createNewFile();
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result).isEqualTo(QuarkusMode.UBER_JAR);
  }

  @Test
  void from_withCustomSuffixAndRunnerJar_shouldReturnUberJar() throws IOException {
    // Given
    projectProperties.put("quarkus.package.runner-suffix", "-coyote");
    new File(target, "-coyote.jar").createNewFile();
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result).isEqualTo(QuarkusMode.UBER_JAR);
  }

  @Test
  void from_withNoSettingsAndRunnerJarAndLib_shouldReturnLegacyJar() throws IOException {
    // Given
    new File(target, "-runner.jar").createNewFile();
    FileUtils.forceMkdir(new File(target, "lib"));
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result).isEqualTo(QuarkusMode.LEGACY_JAR);
  }

  @ParameterizedTest(name = "QuarkusMode from project with ''{0}'' packaging should return ''{1}''")
  @MethodSource("packageTypes")
  void from_withPackageType(String packageType, QuarkusMode expectedMode, boolean isFatJar) {
    // Given
    projectProperties.put("quarkus.package.type", packageType);
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result)
            .isEqualTo(expectedMode)
            .hasFieldOrPropertyWithValue("fatJar", isFatJar);
  }

  public static Stream<Arguments> packageTypes() {
    return Stream.of(
            Arguments.of("native", QuarkusMode.NATIVE, false),
            Arguments.of("legacy-jar", QuarkusMode.LEGACY_JAR, false),
            Arguments.of("fast-jar", QuarkusMode.FAST_JAR, false),
            Arguments.of("uber-jar", QuarkusMode.UBER_JAR, true)
    );
  }
}
