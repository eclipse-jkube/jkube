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
package org.eclipse.jkube.kit.common.archive;

import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.common.archive.AssemblyFileUtils.getAssemblyFileOutputDirectory;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class AssemblyFileUtilsTest {

  @TempDir
  File temporaryFolder;

  @Test
  void getAssemblyFileOutputDirectoryRequired() {
    // Given
    final AssemblyFile af = AssemblyFile.builder().build();
    final File outputDirectoryForRelativePaths = new File(temporaryFolder, "output");
    final Assembly layer = new Assembly();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder().build();
    // When
    final NullPointerException result = assertThrows(NullPointerException.class, () -> {
      getAssemblyFileOutputDirectory(af, outputDirectoryForRelativePaths, layer, ac);
      fail("Should fail as output directory should not be null");
    });
    // Then
    assertThat(result.getMessage()).isEqualTo("Assembly Configuration output dir is required");
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  void getAssemblyFileOutputDirectoryWithAbsoluteDirectoryShouldReturnSame() {
    // Given
    final AssemblyFile af = AssemblyFile.builder().outputDirectory(new File("/")).build();
    final File outputDirectoryForRelativePaths = new File(temporaryFolder, "output");
    final Assembly layer = new Assembly();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder().build();
    // When
    final File result = getAssemblyFileOutputDirectory(af, outputDirectoryForRelativePaths, layer, ac);
    // Then
    assertThat(result.getAbsolutePath()).isEqualTo("/");
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void getAssemblyFileOutputDirectoryWithAbsoluteDirectoryShouldReturnSameWindows() {
    // Given
    final AssemblyFile af = AssemblyFile.builder().outputDirectory(new File("C:\\")).build();
    final File outputDirectoryForRelativePaths = new File(temporaryFolder, "output");
    final Assembly layer = new Assembly();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder().build();
    // When
    final File result = getAssemblyFileOutputDirectory(af, outputDirectoryForRelativePaths, layer, ac);
    // Then
    assertThat(result.getAbsolutePath()).isEqualTo("C:\\");
  }

  @Test
  void getAssemblyFileOutputDirectoryWithRelativeDirectoryShouldReturnComputedPath() {
    // Given
    final AssemblyFile af = AssemblyFile.builder().outputDirectory(new File("target")).build();
    final File outputDirectoryForRelativePaths = new File(temporaryFolder, "output");
    final Assembly layer = new Assembly();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder().targetDir("/project").build();
    // When
    final File result = getAssemblyFileOutputDirectory(af, outputDirectoryForRelativePaths, layer, ac);
    // Then
    final String expectedPath = outputDirectoryForRelativePaths.toPath().resolve("project").resolve("target")
        .toAbsolutePath().toString();
    assertThat(result.getAbsolutePath()).isEqualTo(expectedPath);
  }

  @Test
  void getAssemblyFileOutputDirectoryWithRelativeDirectoryAndAssemblyIdShouldReturnComputedPath() {
    // Given
    final AssemblyFile af = AssemblyFile.builder().outputDirectory(new File("target")).build();
    final File outputDirectoryForRelativePaths = new File(temporaryFolder, "output");
    final Assembly layer = Assembly.builder().id("layer-1").build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder().targetDir("/project").build();
    // When
    final File result = getAssemblyFileOutputDirectory(af, outputDirectoryForRelativePaths, layer, ac);
    // Then
    final String expectedPath = outputDirectoryForRelativePaths.toPath()
        .resolve("layer-1").resolve("project").resolve("target").toAbsolutePath().toString();
    assertThat(result.getAbsolutePath()).isEqualTo(expectedPath);
  }

  @Test
  void getAssemblyFileOutputDirectory_withOutputDirectory_shouldReturnNormalizedDir() {
    // Given
    final AssemblyFile af = AssemblyFile.builder().outputDirectory(new File("build/./camel-quarkus-demo-1.0.0-runner")).build();
    final File outputDirectoryForRelativePaths = new File(temporaryFolder, "output");
    final Assembly layer = Assembly.builder().id("layer-1").build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder().targetDir("/project").build();

    // When
    final File result = getAssemblyFileOutputDirectory(af, outputDirectoryForRelativePaths, layer, ac);

    // Then
    assertThat(result.getAbsolutePath()).isEqualTo(
            new File(outputDirectoryForRelativePaths, "layer-1/project/build/camel-quarkus-demo-1.0.0-runner").getAbsolutePath());
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  void resolveSourceFileAbsoluteFileShouldReturnSame() {
    // Given
    final File baseDirectory = new File(temporaryFolder, "base");
    final AssemblyFile af = AssemblyFile.builder().source(new File("/")).build();
    // When
    final File result = AssemblyFileUtils.resolveSourceFile(baseDirectory, af);
    // Then
    assertThat(result.getAbsolutePath()).isEqualTo("/");
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void resolveSourceFileAbsoluteFileShouldReturnSameWindows() {
    // Given
    final File baseDirectory = new File(temporaryFolder, "base");
    final AssemblyFile af = AssemblyFile.builder().source(new File("C:\\")).build();
    // When
    final File result = AssemblyFileUtils.resolveSourceFile(baseDirectory, af);
    // Then
    assertThat(result.getAbsolutePath()).isEqualTo("C:\\");
  }

  @Test
  void resolveSourceFileRelativeSourceShouldReturnComputedPath() {
    // Given
    final File baseDirectory = new File(temporaryFolder, "base");
    final AssemblyFile af = AssemblyFile.builder().source(new File("some-file.txt")).build();
    // When
    final File result = AssemblyFileUtils.resolveSourceFile(baseDirectory, af);
    // Then
    final String expectedPath = baseDirectory.toPath().resolve("some-file.txt")
        .toAbsolutePath().toString();
    assertThat(result.getAbsolutePath()).isEqualTo(expectedPath);
  }
}