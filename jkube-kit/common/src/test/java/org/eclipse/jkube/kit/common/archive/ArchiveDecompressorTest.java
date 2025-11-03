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

import org.eclipse.jkube.kit.common.assertj.FileAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static org.apache.commons.io.FilenameUtils.separatorsToSystem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ArchiveDecompressorTest {

  @TempDir
  private File tempDir;

  @ParameterizedTest
  @CsvSource({
      "/archive/archive-decompressor/pack-v0.31.0-linux.tgz,pack",
      "/archive/archive-decompressor/pack-v0.31.0-windows.zip,pack.exe"
  })
  void extractArchive_whenArchiveWithSingleFileProvided_thenExtractToSpecifiedDir(String filePath, String expectedFileInExtractedArchiveName) throws IOException {
    // Given
    File input = new File(ArchiveDecompressorTest.class.getResource(filePath).getFile());

    // When
    ArchiveDecompressor.extractArchive(input, tempDir);

    // Then
    FileAssertions.assertThat(tempDir)
        .exists()
        .fileTree()
        .containsExactlyInAnyOrder(expectedFileInExtractedArchiveName);
  }

  @ParameterizedTest
  @CsvSource({
      "/archive/archive-decompressor/nested-archive.tgz,nested,nested/folder,nested/folder/artifact",
      "/archive/archive-decompressor/nested-archive.zip,nested,nested/folder,nested/folder/artifact.exe"
  })
  void extractArchive_whenArchiveWithNestedDir_thenExtractToSpecifiedDir(String filePath, String parentDir, String artifactParentDir, String artifact) throws IOException {
    // Given
    File input = new File(ArchiveDecompressorTest.class.getResource(filePath).getFile());

    // When
    ArchiveDecompressor.extractArchive(input, tempDir);

    // Then
    FileAssertions.assertThat(tempDir)
        .exists()
        .fileTree()
        .containsExactlyInAnyOrder(parentDir, separatorsToSystem(artifactParentDir), separatorsToSystem(artifact));
  }

  @Test
  void extractArchive_whenUnsupportedArchiveProvided_thenThrowException() {
    // Given
    File input = new File(ArchiveDecompressorTest.class.getResource("/archive/archive-decompressor/foo.xz").getFile());

    // When
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ArchiveDecompressor.extractArchive(input, tempDir))
        .withMessage("Unsupported archive file provided");
  }

  @Test
  void extractArchive_whenInvalidArchiveProvided_throwsException() throws IOException {
    try (final InputStream input = ArchiveDecompressorTest.class.getResourceAsStream("/archive/archive-decompressor/invalid-archive.txt")) {
      assertThatIllegalArgumentException()
        .isThrownBy(() -> ArchiveDecompressor.extractArchive(input, tempDir))
        .withMessage("Unsupported archive file provided");
    }
  }

  @Test
  void extractArchive_whenInvalidCompressedArchiveProvided_throwsException() throws IOException {
    try (final InputStream input = ArchiveDecompressorTest.class.getResourceAsStream("/archive/archive-decompressor/invalid-archive.txt.gz")) {
      assertThatIllegalArgumentException()
        .isThrownBy(() -> ArchiveDecompressor.extractArchive(input, tempDir))
        .withMessage("Unsupported archive file provided");
    }
  }

  @Test
  void extractArchive_whenTargetDirectoryExistsAsFile_throwsException() throws IOException {
    try (final InputStream input = ArchiveDecompressorTest.class.getResourceAsStream("/archive/archive-decompressor/nested-archive.tgz")) {
      final File targetDirectory = Files.createFile(tempDir.toPath().resolve("target-as-file")).toFile();
      assertThatIllegalArgumentException()
        .isThrownBy(() -> ArchiveDecompressor.extractArchive(input, targetDirectory))
        .withMessage("Target directory is not a directory");
    }
  }

  @ParameterizedTest
  @CsvSource({
      "/archive/archive-decompressor/pack-v0.31.0-linux.tgz",
      "/archive/archive-decompressor/pack-v0.31.0-windows.zip",
      "/archive/archive-decompressor/nested-archive.tgz",
      "/archive/archive-decompressor/nested-archive.zip"
  })
  void isArchive_whenValidArchiveFileProvided_thenReturnsTrue(String filePath) {
    // Given
    File input = new File(ArchiveDecompressorTest.class.getResource(filePath).getFile());

    // When
    boolean result = ArchiveDecompressor.isArchive(input);

    // Then
    assertThat(result).isTrue();
  }

  @ParameterizedTest
  @CsvSource({
      "/archive/archive-decompressor/foo.xz",
      "/archive/archive-decompressor/invalid-archive.txt.gz"
  })
  void isArchive_whenCompressedFileProvided_thenReturnsTrue(String filePath) {
    // Given
    File input = new File(ArchiveDecompressorTest.class.getResource(filePath).getFile());

    // When
    boolean result = ArchiveDecompressor.isArchive(input);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isArchive_whenNonArchiveFileProvided_thenReturnsFalse() {
    // Given
    File input = new File(ArchiveDecompressorTest.class.getResource("/archive/archive-decompressor/invalid-archive.txt").getFile());

    // When
    boolean result = ArchiveDecompressor.isArchive(input);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isArchive_whenNonExistentFileProvided_thenReturnsFalse() {
    // Given
    File input = new File(tempDir, "non-existent-file.txt");

    // When
    boolean result = ArchiveDecompressor.isArchive(input);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isArchive_whenExecutableFileProvided_thenReturnsFalse() throws IOException {
    // Given
    File executable = Files.createFile(tempDir.toPath().resolve("executable")).toFile();
    executable.setExecutable(true);
    Files.write(executable.toPath(), "#!/bin/sh\necho 'test'".getBytes());

    // When
    boolean result = ArchiveDecompressor.isArchive(executable);

    // Then
    assertThat(result).isFalse();
  }

  @ParameterizedTest
  @CsvSource({
      "archive.tar",
      "archive.tgz",
      "archive.tar.gz",
      "archive.tar.bz2",
      "archive.tar.xz",
      "archive.zip",
      "application.jar",
      "application.war",
      "application.ear",
      "archive.rar",
      "archive.7z",
      "file.gz",
      "file.bz2",
      "file.xz"
  })
  void isArchive_whenFileWithArchiveExtension_thenReturnsTrueWithoutReadingContent(String fileName) throws IOException {
    // Given - Create an empty file with archive extension (no actual archive content)
    File fileWithArchiveExtension = Files.createFile(tempDir.toPath().resolve(fileName)).toFile();

    // When
    boolean result = ArchiveDecompressor.isArchive(fileWithArchiveExtension);

    // Then - Should return true based on extension alone (fast path)
    assertThat(result).isTrue();
  }

  @ParameterizedTest
  @CsvSource({
      "ARCHIVE.TAR",
      "ARCHIVE.TGZ",
      "ARCHIVE.ZIP",
      "Application.JAR",
      "Application.War",
      "file.GZ"
  })
  void isArchive_whenFileWithUppercaseArchiveExtension_thenReturnsTrue(String fileName) throws IOException {
    // Given - Create file with uppercase extension
    File fileWithUppercaseExtension = Files.createFile(tempDir.toPath().resolve(fileName)).toFile();

    // When
    boolean result = ArchiveDecompressor.isArchive(fileWithUppercaseExtension);

    // Then - Extension check should be case-insensitive
    assertThat(result).isTrue();
  }

  @ParameterizedTest
  @CsvSource({
      "archive.txt",
      "file.jpg",
      "document.pdf",
      "script.sh",
      "data.json"
  })
  void isArchive_whenFileWithNonArchiveExtension_thenReturnsFalse(String fileName) throws IOException {
    // Given
    File nonArchiveFile = Files.createFile(tempDir.toPath().resolve(fileName)).toFile();

    // When
    boolean result = ArchiveDecompressor.isArchive(nonArchiveFile);

    // Then
    assertThat(result).isFalse();
  }
}
