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

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class JKubeArchiveDecompressorTest {
  @TempDir
  private File temporaryFolder;

  @ParameterizedTest
  @CsvSource({
      "/archives/pack-v0.31.0-linux.tgz,pack",
      "/archives/pack-v0.31.0-windows.zip,pack.exe"
  })
  void extractArchive_whenArchiveWithSingleFileProvided_thenExtractToSpecifiedDir(String filePath, String expectedFileInExtractedArchiveName) throws IOException {
    // Given
    File input = new File(getClass().getResource(filePath).getFile());

    // When
    JKubeArchiveDecompressor.extractArchive(input, temporaryFolder);

    // Then
    FileAssertions.assertThat(temporaryFolder)
        .exists()
        .fileTree()
        .containsExactlyInAnyOrder(expectedFileInExtractedArchiveName);
  }

  @ParameterizedTest
  @CsvSource({
      "/archives/nested-archive.tgz,nested,nested/folder,nested/folder/artifact",
      "/archives/nested-archive.zip,nested,nested/folder,nested/folder/artifact.exe"
  })
  void extractArchive_whenArchiveWithNestedDir_thenExtractToSpecifiedDir(String filePath, String parentDir, String artifactParentDir, String artifact) throws IOException {
    // Given
    File input = new File(getClass().getResource(filePath).getFile());

    // When
    JKubeArchiveDecompressor.extractArchive(input, temporaryFolder);

    // Then
    FileAssertions.assertThat(temporaryFolder)
        .exists()
        .fileTree()
        .containsExactlyInAnyOrder(parentDir, artifactParentDir, artifact);
  }

  @Test
  void extractArchive_whenUnsupportedArchiveProvided_thenThrowException() {
    // Given
    File input = new File(getClass().getResource("/archives/foo.xz").getFile());

    // When
    assertThatIllegalStateException()
        .isThrownBy(() -> JKubeArchiveDecompressor.extractArchive(input, temporaryFolder))
        .withMessage("Unsupported archive file provided");
  }
}
