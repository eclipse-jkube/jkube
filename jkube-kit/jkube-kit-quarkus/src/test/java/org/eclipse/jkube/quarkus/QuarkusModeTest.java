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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.kit.common.JavaProject;

import mockit.Expectations;
import mockit.Mocked;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class QuarkusModeTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mocked
  private JavaProject project;

  private File target;
  private List<String> compileClassPathElements;
  private Properties projectProperties;

  @Before
  public void setUp() throws IOException {
    target = temporaryFolder.newFolder("target");
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
  public void from_withNoSettingsAndNoFiles_shouldReturnFastJar() {
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result).isEqualTo(QuarkusMode.FAST_JAR);
  }

  @Test
  public void from_withNoSettingsAndNativeBinary_shouldReturnNative() throws IOException {
    // Given
    new File(target, "-runner").createNewFile();
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result).isEqualTo(QuarkusMode.NATIVE);
  }

  @Test
  public void from_withCustomSuffixAndNativeBinary_shouldReturnNative() throws IOException {
    // Given
    projectProperties.put("quarkus.package.runner-suffix", "-coyote");
    new File(target, "-coyote").createNewFile();
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result).isEqualTo(QuarkusMode.NATIVE);
  }

  @Test
  public void from_withNoSettingsAndRunnerJar_shouldReturnUberJar() throws IOException {
    // Given
    new File(target, "-runner.jar").createNewFile();
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result).isEqualTo(QuarkusMode.UBER_JAR);
  }

  @Test
  public void from_withCustomSuffixAndRunnerJar_shouldReturnUberJar() throws IOException {
    // Given
    projectProperties.put("quarkus.package.runner-suffix", "-coyote");
    new File(target, "-coyote.jar").createNewFile();
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result).isEqualTo(QuarkusMode.UBER_JAR);
  }

  @Test
  public void from_withNoSettingsAndRunnerJarAndLib_shouldReturnLegacyJar() throws IOException {
    // Given
    new File(target, "-runner.jar").createNewFile();
    FileUtils.forceMkdir(new File(target, "lib"));
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result).isEqualTo(QuarkusMode.LEGACY_JAR);
  }

  @Test
  public void from_withNativePackaging_shouldReturnNative() {
    // Given
    projectProperties.put("quarkus.package.type", "native");
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result)
        .isEqualTo(QuarkusMode.NATIVE)
        .hasFieldOrPropertyWithValue("fatJar", false);
  }

  @Test
  public void from_withLegacyJarPackaging_shouldReturnLegacyJar() {
    // Given
    projectProperties.put("quarkus.package.type", "legacy-jar");
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result)
        .isEqualTo(QuarkusMode.LEGACY_JAR)
        .hasFieldOrPropertyWithValue("fatJar", false);
  }

  @Test
  public void from_withFastJarPackaging_shouldReturnFastJar() {
    // Given
    projectProperties.put("quarkus.package.type", "fast-jar");
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result)
        .isEqualTo(QuarkusMode.FAST_JAR)
        .hasFieldOrPropertyWithValue("fatJar", false);
  }

  @Test
  public void from_withUberJarPackaging_shouldReturnUberJar() {
    // Given
    projectProperties.put("quarkus.package.type", "uber-jar");
    // When
    final QuarkusMode result = QuarkusMode.from(project);
    // Then
    assertThat(result)
        .isEqualTo(QuarkusMode.UBER_JAR)
        .hasFieldOrPropertyWithValue("fatJar", true);
  }
}
