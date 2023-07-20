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
package org.eclipse.jkube.kit.common.archive;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.AssemblyFileSet;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.eclipse.jkube.kit.common.archive.AssemblyFileSetUtils.calculateFilePermissions;
import static org.eclipse.jkube.kit.common.archive.AssemblyFileSetUtils.isSelfPath;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unused")
class AssemblyFileSetUtilsTest {

  @TempDir
  File temp;

  @Test
  void calculateFilePermissionsFileWithNoFileMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().build();
    final File sourceFile = File.createTempFile("source-file", "txt", temp);
    final File aFile = File.createTempFile("just-a-file", "txt", temp);
    // When
    final List<AssemblyFileEntry> result = calculateFilePermissions(sourceFile, aFile, afs);
    // Then
    assertThat(result).hasSize(1).contains(new AssemblyFileEntry(sourceFile, aFile, "0644"));
  }

  @Test
  void calculateFilePermissionsFileWithFileMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().fileMode("0777").build();
    final File sourceFile = File.createTempFile("source-file", "txt", temp);
    final File aFile = File.createTempFile("just-a-file", "txt", temp);
    // When
    final List<AssemblyFileEntry> result = calculateFilePermissions(sourceFile, aFile, afs);
    // Then
    assertThat(result).hasSize(1).contains(new AssemblyFileEntry(sourceFile, aFile, "0777"));
  }

  @Test
  void calculateFilePermissionsDirectoryWithNoDirectoryMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().build();
    final File sourceDirectory = temp.toPath().resolve("source-directory").toFile();
    final File aDirectory = Files.createDirectories(temp.toPath().resolve("just-a-directory")).toFile();
    // When
    final List<AssemblyFileEntry> result = calculateFilePermissions(sourceDirectory, aDirectory, afs);
    // Then
    assertThat(result).hasSize(1).contains(new AssemblyFileEntry(sourceDirectory, aDirectory, "040755"));
  }

  @Test
  void calculateFilePermissionsDirectoryWithDirectoryMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().directoryMode("040777").build();
    final File sourceDirectory = new File(temp, "source-directory");
    final File sourceSubdirectory = new File(sourceDirectory, "subdirectory");
    FileUtils.forceMkdir(sourceSubdirectory);
    final File sourceFile = new File(sourceDirectory, "file.txt");
    assertThat(sourceFile.createNewFile()).isTrue();
    final File aDirectory = new File(temp, "just-a-directory");
    final File aSubdirectory = new File(aDirectory, "subdirectory");
    FileUtils.forceMkdir(aSubdirectory);
    final File aFile = new File(aDirectory, "file.txt");
    assertThat(aFile.createNewFile()).isTrue();
    // When
    final List<AssemblyFileEntry> result = calculateFilePermissions(sourceDirectory, aDirectory, afs);
    // Then
    assertThat(result).hasSize(3).containsExactlyInAnyOrder(
            new AssemblyFileEntry(sourceDirectory, aDirectory, "040777"),
            new AssemblyFileEntry(sourceSubdirectory, aSubdirectory, "040777"),
            new AssemblyFileEntry(sourceFile, aFile, "0644")
    );
  }

  @Test
  void calculateFilePermissionsDirectoryAndNestedDirectoryWithDirectoryAndFileMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().directoryMode("040775").fileMode("0755").build();
    final File sourceDirectory = new File(temp, "source-directory");
    final File sourceSubdirectory = new File(sourceDirectory, "subdirectory");
    FileUtils.forceMkdir(sourceSubdirectory);
    final File aDirectory = new File(temp, "just-a-directory");
    final File aSubdirectory = new File(aDirectory, "subdirectory");
    FileUtils.forceMkdir(aSubdirectory);
    // When
    final List<AssemblyFileEntry> result = calculateFilePermissions(sourceDirectory, aDirectory, afs);
    // Then
    assertThat(result).hasSize(2).containsExactlyInAnyOrder(
            new AssemblyFileEntry(sourceDirectory, aDirectory, "040775"),
            new AssemblyFileEntry(sourceSubdirectory, aSubdirectory, "040775")
    );
  }

  @Test
  void calculateFilePermissionsDirectoryAndNestedDirectoryAndFileWithDirectoryAndFileMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().directoryMode("040755").fileMode("0755").build();
    final File sourceDirectory = new File(temp, "source-directory");
    final File sourceSubdirectory = new File(sourceDirectory, "subdirectory");
    FileUtils.forceMkdir(sourceSubdirectory);
    final File sourceFile = new File(sourceDirectory, "file.txt");
    assertThat(sourceFile.createNewFile()).isTrue();
    final File aDirectory = new File(temp, "just-a-directory");
    final File aSubdirectory = new File(aDirectory, "subdirectory");
    FileUtils.forceMkdir(aSubdirectory);
    final File aFile = new File(aDirectory, "file.txt");
    assertThat(aFile.createNewFile()).isTrue();
    // When
    final List<AssemblyFileEntry> result = calculateFilePermissions(sourceDirectory, aDirectory, afs);
    // Then
    assertThat(result).hasSize(3).containsExactlyInAnyOrder(
            new AssemblyFileEntry(sourceDirectory, aDirectory, "040755"),
            new AssemblyFileEntry(sourceSubdirectory, aSubdirectory, "040755"),
            new AssemblyFileEntry(sourceFile, aFile, "0755")
    );
  }

  @DisplayName("Self Path Tests")
  @ParameterizedTest(name = "selfPath with a ''{0}'' value should be true")
  @MethodSource("selfPathTestData")
  void selfPath(String testDesc, String path) {
    // When
    boolean result = isSelfPath(path);
    // Then
    assertThat(result).isTrue();
  }

  public static Stream<Arguments> selfPathTestData() {
    return Stream.of(
            Arguments.of("null", null),
            Arguments.of("blank string", "   "),
            Arguments.of("empty string", ""),
            Arguments.of(".", ".")
    );
  }

}
