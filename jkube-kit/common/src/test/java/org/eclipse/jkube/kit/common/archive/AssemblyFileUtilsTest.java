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

import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.eclipse.jkube.kit.common.util.EnvUtil.isWindows;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

public class AssemblyFileUtilsTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void getAssemblyFileOutputDirectoryRequired() throws IOException {
      // Given
      final AssemblyFile af = AssemblyFile.builder().build();
      final File outputDirectoryForRelativePaths = temporaryFolder.newFolder("output");
      final AssemblyConfiguration ac = AssemblyConfiguration.builder().build();

      // When
      try {
          AssemblyFileUtils.getAssemblyFileOutputDirectory(af, outputDirectoryForRelativePaths, ac);
          fail("Should fail as output directory should not be null");
      } catch(NullPointerException ex) {
          assertEquals("Assembly Configuration output dir is required", ex.getMessage());
      }
  }

  @Test
  public void getAssemblyFileOutputDirectoryWithAbsoluteDirectoryShouldReturnSame() throws IOException {
    // Given
    assumeFalse(isWindows());
    final AssemblyFile af = AssemblyFile.builder().outputDirectory(new File("/")).build();
    final File outputDirectoryForRelativePaths = temporaryFolder.newFolder("output");
    final AssemblyConfiguration ac = AssemblyConfiguration.builder().build();
    // When
    final File result = AssemblyFileUtils.getAssemblyFileOutputDirectory(af, outputDirectoryForRelativePaths, ac);
    // Then
    assertEquals("/", result.getAbsolutePath());
  }

  @Test
  public void getAssemblyFileOutputDirectoryWithAbsoluteDirectoryShouldReturnSameWindows() throws IOException {
    // Given
    assumeTrue(isWindows());
    final AssemblyFile af = AssemblyFile.builder().outputDirectory(new File("C:\\")).build();
    final File outputDirectoryForRelativePaths = temporaryFolder.newFolder("output");
    final AssemblyConfiguration ac = AssemblyConfiguration.builder().build();
    // When
    final File result = AssemblyFileUtils.getAssemblyFileOutputDirectory(af, outputDirectoryForRelativePaths, ac);
    // Then
    assertEquals("C:\\", result.getAbsolutePath());
  }

  @Test
  public void getAssemblyFileOutputDirectoryWithRelativeDirectoryShouldReturnComputedPath() throws IOException {
    // Given
    final AssemblyFile af = AssemblyFile.builder().outputDirectory(new File("target")).build();
    final File outputDirectoryForRelativePaths = temporaryFolder.newFolder("output");
    final AssemblyConfiguration ac = AssemblyConfiguration.builder().targetDir("/project").build();
    // When
    final File result = AssemblyFileUtils.getAssemblyFileOutputDirectory(af, outputDirectoryForRelativePaths, ac);
    // Then
    final String expectedPath = outputDirectoryForRelativePaths.toPath().resolve("project").resolve("target")
        .toAbsolutePath().toString();
    assertEquals(expectedPath, result.getAbsolutePath());
  }

  @Test
  public void resolveSourceFileAbsoluteFileShouldReturnSame() throws IOException {
    // Given
    assumeFalse(isWindows());
    final File baseDirectory = temporaryFolder.newFolder("base");
    final AssemblyFile af = AssemblyFile.builder().source(new File("/")).build();
    // When
    final File result = AssemblyFileUtils.resolveSourceFile(baseDirectory, af);
    // Then
    assertEquals("/", result.getAbsolutePath());
  }

  @Test
  public void resolveSourceFileAbsoluteFileShouldReturnSameWindows() throws IOException {
    // Given
    assumeTrue(isWindows());
    final File baseDirectory = temporaryFolder.newFolder("base");
    final AssemblyFile af = AssemblyFile.builder().source(new File("C:\\")).build();
    // When
    final File result = AssemblyFileUtils.resolveSourceFile(baseDirectory, af);
    // Then
    assertEquals("C:\\", result.getAbsolutePath());
  }

  @Test
  public void resolveSourceFileRelativeSourceShouldReturnComputedPath() throws IOException {
    // Given
    final File baseDirectory = temporaryFolder.newFolder("base");
    final AssemblyFile af = AssemblyFile.builder().source(new File("some-file.txt")).build();
    // When
    final File result = AssemblyFileUtils.resolveSourceFile(baseDirectory, af);
    // Then
    final String expectedPath = baseDirectory.toPath().resolve("some-file.txt")
        .toAbsolutePath().toString();
    assertEquals(expectedPath, result.getAbsolutePath());
  }
}