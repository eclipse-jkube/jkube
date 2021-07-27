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
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Consumer;

import org.eclipse.jkube.kit.common.assertj.ArchiveAssertions;
import org.eclipse.jkube.kit.common.util.FileUtil;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JKubeTarArchiverTest {

  private static final String LONG_FILE_NAME = "0123456789012345678901234567890123456789012345678901234567890123456789"
      + "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
      + "nested.file.long";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private File toCompress;

  @Before
  public void prepareDirectory() throws IOException {
    toCompress = temporaryFolder.newFolder("toCompress");
    final File nestedDir = toCompress.toPath().resolve("nested").resolve("directory").toFile();
    FileUtils.forceMkdir(nestedDir);
    final File nestedFile = nestedDir.toPath().resolve(LONG_FILE_NAME).toFile();
    FileUtils.write(nestedFile, "Nested file content", StandardCharsets.UTF_8);
    final File file = toCompress.toPath().resolve("file.txt").toFile();
    FileUtils.write(file, "File content", StandardCharsets.UTF_8);
  }

  @Test
  public void createTarBallOfDirectory_defaultCompression_createsTar() throws Exception {
    // Given
    final File outputFile = temporaryFolder.newFile("target.noExtension");
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
  public void createTarBallOfDirectory_gzipCompression_createsTar() throws Exception {
    // Given
    final File outputFile = temporaryFolder.newFile("target.tar.gzip");
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
  public void createTarBallOfDirectory_bzip2Compression_createsTar() throws Exception {
    // Given
    final File outputFile = temporaryFolder.newFile("target.tar.bz2");
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
  public void createTarBallOfDirectory_defaultCompressionWithEntryCustomizer_createsCustomizedTar() throws Exception {
    // Given
    final File outputFile = temporaryFolder.newFile("target.tar");
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
  public void createTarBallOfDirectory_defaultCompressionWithTarCustomizer_createsCustomizedTar() throws Exception {
    // Given
    final File outputFile = temporaryFolder.newFile("target.tar");
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
  public void createTarArchiveEntry_whenUsedForDirectories_shouldCreateEntryWithZeroSize() {
    // Given
    File tmpDir = new File(getClass().getResource("/jkubetararchiver-directory/").getFile());

    // When
    TarArchiveEntry tarArchiveEntry = JKubeTarArchiver.createTarArchiveEntry(Collections.emptyMap(), tmpDir, "/tmp/foo");

    // Then
    assertThat(tarArchiveEntry)
            .isNotNull()
            .hasFieldOrPropertyWithValue("size", 0L);
  }

  @Test
  public void createTarArchiveEntry_whenUsedForFiles_shouldCreateEntryWithNonZeroSize() {
    // Given
    File tmpFile = new File(getClass().getResource("/jkubetararchiver-directory/jkubetararchiver-non-empty-file.txt").getFile());

    // When
    TarArchiveEntry tarArchiveEntry = JKubeTarArchiver.createTarArchiveEntry(Collections.singletonMap(tmpFile, "0644"), tmpFile, "/tmp/foo");

    // Then
    assertThat(tarArchiveEntry)
            .isNotNull()
            .hasFieldOrPropertyWithValue("size", 10L)
            .hasFieldOrPropertyWithValue("mode", Integer.parseInt("0644", 8));
  }
}
