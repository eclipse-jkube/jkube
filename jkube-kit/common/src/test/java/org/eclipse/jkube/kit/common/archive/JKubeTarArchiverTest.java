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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.jkube.kit.common.assertj.ArchiveAssertions;
import org.eclipse.jkube.kit.common.util.FileUtil;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.tuple;

class JKubeTarArchiverTest {

  private static final String LONG_FILE_NAME = "0123456789012345678901234567890123456789012345678901234567890123456789"
      + "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
      + "nested.file.long";

  @TempDir
  File temporaryFolder;
  private File toCompress;

  @BeforeEach
  public void prepareDirectory() throws IOException {
    toCompress = Files.createDirectory(temporaryFolder.toPath().resolve("toCompress")).toFile();
    final File nestedDir = toCompress.toPath().resolve("nested").resolve("directory").toFile();
    FileUtils.forceMkdir(nestedDir);
    final File nestedFile = nestedDir.toPath().resolve(LONG_FILE_NAME).toFile();
    FileUtils.write(nestedFile, "Nested file content", StandardCharsets.UTF_8);
    final File file = toCompress.toPath().resolve("file.txt").toFile();
    FileUtils.write(file, "File content", StandardCharsets.UTF_8);
  }

  @Test
  void createTarBallOfDirectory_defaultCompression_createsTar() throws Exception {
    // Given
    final File outputFile = Files.createTempFile(temporaryFolder.toPath(), "target", "noExtension").toFile();
    // When
    final File result = JKubeTarArchiver.createTarBallOfDirectory(outputFile, toCompress, ArchiveCompression.none);
    // Then
    ArchiveAssertions.assertThat(result)
        .isSameAs(outputFile)
        .isNotEmpty()
        .isUncompressed()
        .fileTree()
        .containsExactlyInAnyOrder(
            "file.txt",
            "nested/",
            "nested/directory/",
            "nested/directory/" + LONG_FILE_NAME);
  }

  @Test
  void createTarBallOfDirectory_gzipCompression_createsTar() throws Exception {
    // Given
    final File outputFile = Files.createTempFile(temporaryFolder.toPath(), "target", "tar.gzip").toFile();
    // When
    final File result = JKubeTarArchiver.createTarBallOfDirectory(outputFile, toCompress, ArchiveCompression.gzip);
    // Then
    ArchiveAssertions.assertThat(result)
        .isSameAs(outputFile)
        .isNotEmpty()
        .isGZip()
        .fileTree()
        .containsExactlyInAnyOrder(
            "file.txt",
            "nested/",
            "nested/directory/",
            "nested/directory/" + LONG_FILE_NAME);
  }

  @Test
  void createTarBallOfDirectory_bzip2Compression_createsTar() throws Exception {
    // Given
    final File outputFile = Files.createTempFile(temporaryFolder.toPath(), "target", "tar.bz2").toFile();
    // When
    final File result = JKubeTarArchiver.createTarBallOfDirectory(outputFile, toCompress, ArchiveCompression.bzip2);
    // Then
    ArchiveAssertions.assertThat(result)
        .isSameAs(outputFile)
        .isNotEmpty()
        .isBzip2()
        .fileTree()
        .containsExactlyInAnyOrder(
            "file.txt",
            "nested/",
            "nested/directory/",
            "nested/directory/" + LONG_FILE_NAME);
  }

  @Test
  void createTarBallOfDirectory_defaultCompressionWithEntryCustomizer_createsCustomizedTar() throws Exception {
    // Given
    final File outputFile = Files.createTempFile(temporaryFolder.toPath(), "target", "tar").toFile();
    final Consumer<TarArchiveEntry> prependDirectory = tae ->  tae.setName("directory/" + tae.getName());
    // When
    final File result = JKubeTarArchiver.createTarBall(outputFile, toCompress,
        FileUtil.listFilesAndDirsRecursivelyInDirectory(toCompress), Collections.emptyMap(), ArchiveCompression.none,
        null, prependDirectory);
    // Then
    ArchiveAssertions.assertThat(result)
        .isSameAs(outputFile)
        .isNotEmpty()
        .isUncompressed()
        .fileTree()
        .containsExactlyInAnyOrder(
            "directory/file.txt",
            "directory/nested/",
            "directory/nested/directory/",
            "directory/nested/directory/" + LONG_FILE_NAME);
  }

  @Test
  void createTarBallOfDirectory_defaultCompressionWithTarCustomizer_createsCustomizedTar() throws Exception {
    // Given
    final File outputFile = Files.createTempFile(temporaryFolder.toPath(), "target", "tar").toFile();
    final Consumer<TarArchiveOutputStream> truncateFilenames = tae ->  tae.setLongFileMode(TarArchiveOutputStream.LONGFILE_TRUNCATE);
    // When
    final File result = JKubeTarArchiver.createTarBall(outputFile, toCompress,
        FileUtil.listFilesAndDirsRecursivelyInDirectory(toCompress), Collections.emptyMap(), ArchiveCompression.none,
        truncateFilenames, null);
    // Then
    ArchiveAssertions.assertThat(result)
        .isSameAs(outputFile)
        .isNotEmpty()
        .isUncompressed()
        .fileTree()
        .containsExactlyInAnyOrder(
            "file.txt",
            "nested/",
            "nested/directory/",
            "nested/directory/01234567890123456789012345678901234567890123456789012345678901234567890123456789012");
  }

  @Test
  void createTarBallOfDirectory_defaultCompression_createsTarWithCorrectEntrySizeAndModeForDirectories() throws Exception {
    // Given
    final int defaultDirMode = Integer.parseInt("040755", 8); // 16877
    final File outputFile = Files.createTempFile(temporaryFolder.toPath(), "target", "noExtension").toFile();
    // When
    final File result = JKubeTarArchiver.createTarBallOfDirectory(outputFile, toCompress, ArchiveCompression.none);
    // Then
    ArchiveAssertions.assertThat(result)
        .isSameAs(outputFile)
        .isNotEmpty()
        .entries()
        .filteredOn(TarArchiveEntry::isDirectory)
        .hasSize(2)
        .extracting("name", "size", "mode")
        .containsExactlyInAnyOrder(
            tuple("nested/", 0L, defaultDirMode),
            tuple("nested/directory/", 0L, defaultDirMode)
        );
  }

  @Test
  void createTarBallOfDirectory_defaultCompression_createsTarWithCorrectEntrySizeAndModeForFiles() throws Exception {
    // Given
    final int defaultFileMode = Integer.parseInt("0100644", 8); // 33188
    final File outputFile = Files.createTempFile(temporaryFolder.toPath(), "target", "noExtension").toFile();
    // When
    final File result = JKubeTarArchiver.createTarBallOfDirectory(outputFile, toCompress, ArchiveCompression.none);
    // Then
    ArchiveAssertions.assertThat(result)
        .isSameAs(outputFile)
        .isNotEmpty()
        .entries()
        .filteredOn(TarArchiveEntry::isFile)
        .hasSize(2)
        .extracting("name", "size", "mode")
        .containsExactlyInAnyOrder(
            tuple("file.txt", 12L, defaultFileMode),
            tuple("nested/directory/" + LONG_FILE_NAME, 19L, defaultFileMode)
        );
  }

  @Test
  void createTarBall_defaultCompressionWithFileModes_createsTarWithCorrectEntrySizeAndModeForFiles() throws Exception {
    // Given
    final int defaultDirMode = Integer.parseInt("040755", 8); // 16877
    final File outputFile = Files.createTempFile(temporaryFolder.toPath(), "target", "noExtension").toFile();
    Map<File, String> fileModeMap = new HashMap<>();
    fileModeMap.put(new File(toCompress, "nested"), "040755");
    fileModeMap.put(new File(toCompress, "nested/directory"), "040755");

    // When
    final File result = JKubeTarArchiver.createTarBall(outputFile, toCompress, FileUtil.listFilesAndDirsRecursivelyInDirectory(toCompress), fileModeMap, ArchiveCompression.none);

    // Then
    ArchiveAssertions.assertThat(result)
        .isSameAs(outputFile)
        .isNotEmpty()
        .entries()
        .filteredOn(TarArchiveEntry::isDirectory)
        .hasSize(2)
        .extracting("name", "size", "mode")
        .containsExactlyInAnyOrder(
            tuple("nested/", 0L, defaultDirMode),
            tuple("nested/directory/", 0L, defaultDirMode)
        );
  }
}
