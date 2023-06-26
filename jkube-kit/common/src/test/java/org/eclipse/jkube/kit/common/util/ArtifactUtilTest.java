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
package org.eclipse.jkube.kit.common.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.common.util.ArtifactUtil.LAST_MODIFIED_TIME_SAVE_FILENAME;

class ArtifactUtilTest {

  @TempDir
  Path tempDir;

  private File artifact;
  private long currentArtifactLastModifiedTime;

  @BeforeEach
  void setup() throws IOException {
    artifact = Files.createFile(tempDir.resolve("test_artifact")).toFile();
    BasicFileAttributes attr = Files.readAttributes(artifact.toPath(), BasicFileAttributes.class);
    currentArtifactLastModifiedTime = attr.lastModifiedTime().toMillis();
  }

  @Test
  @DisplayName("Should save the Last modified time of the file correctly")
  void testSavePreviousArtifactLastModifiedTime() throws IOException {
    // When
    ArtifactUtil.saveCurrentArtifactLastModifiedTime(tempDir, artifact);

    // Then
    assertThat(tempDir.resolve(LAST_MODIFIED_TIME_SAVE_FILENAME).toFile()).exists();
    Path lastModifiedTimeSavePath = tempDir.resolve(LAST_MODIFIED_TIME_SAVE_FILENAME);
    try (DataInputStream in = new DataInputStream(Files.newInputStream(lastModifiedTimeSavePath))) {
      assertThat(in.readLong()).isEqualTo(currentArtifactLastModifiedTime);
    }
  }

  @Test
  @DisplayName("Should load the Last modified time of the file correctly")
  void testRetrievePreviousArtifactLastModifiedTime() throws IOException {
    // When
    ArtifactUtil.saveCurrentArtifactLastModifiedTime(tempDir, artifact);
    long previousArtifactLastModifiedTime = ArtifactUtil.retrievePreviousArtifactLastModifiedTime(tempDir);

    // Then
    assertThat(previousArtifactLastModifiedTime).isEqualTo(currentArtifactLastModifiedTime);
  }
}
