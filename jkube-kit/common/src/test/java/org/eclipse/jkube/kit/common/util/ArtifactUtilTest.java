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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ArtifactUtilTest {

  @TempDir
  Path tempDir;
  private Path target;
  private KitLogger kitLogger;

  @BeforeEach
  void setup() throws IOException {
    target = tempDir.resolve("target");
    FileUtil.createDirectory(target.toFile());
    kitLogger = spy(new KitLogger.SilentLogger());
  }

  @Test
  @DisplayName("with null artifact, should log missing build warning")
  void nullArtifact() {
    // When
    ArtifactUtil.warnStaleArtifact(kitLogger, null);
    // Then
    verify(kitLogger).warn("Final output artifact file was not detected. The project may have not been built. " +
      "HINT: try to compile and package your application prior to running the container image build task.");
  }

  @Test
  @DisplayName("with nonexistent artifact, should log missing build warning")
  void noArtifact() {
    // When
    ArtifactUtil.warnStaleArtifact(kitLogger, target.resolve("i-dont-exist").toFile());
    // Then
    verify(kitLogger).warn("Final output artifact file was not detected. The project may have not been built. " +
      "HINT: try to compile and package your application prior to running the container image build task.");
  }

  @Nested
  @DisplayName("with artifact")
  class WithArtifact {

    private long currentArtifactLastModifiedTime;
    private File artifact;

    @BeforeEach
    void setup() throws IOException {
      artifact = Files.createFile(target.resolve("the-artifact.file")).toFile();
      currentArtifactLastModifiedTime = Files.readAttributes(artifact.toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis();
    }

    @Test
    @DisplayName("no previous build, should not log warning")
    void noPreviousBuild() {
      // When
      ArtifactUtil.warnStaleArtifact(kitLogger, artifact);
      // Then
      verify(kitLogger, times(0)).warn(anyString());
    }

    @Test
    @DisplayName("no previous build,saves artifact last modified time")
    void noPreviousBuildSavesArtifactBuildTime() {
      // When
      ArtifactUtil.warnStaleArtifact(kitLogger, artifact);
      // Then
      assertThat(target.resolve(".jkube-last-modified").toFile())
        .hasContent("" + currentArtifactLastModifiedTime);
    }
    @Test
    @DisplayName("previous build with different time, should not log warning")
    void previousBuildWithDifferentTime() throws IOException {
      // Given
      Files.write(target.resolve(".jkube-last-modified"), "123456789".getBytes());
      // When
      ArtifactUtil.warnStaleArtifact(kitLogger, artifact);
      // Then
      verify(kitLogger, times(0)).warn(anyString());
    }

    @Test
    @DisplayName("previous build with different time, saves artifact last modified time")
    void previousBuildWithDifferentTimeSavesArtifactBuildTime() throws IOException {
      // Given
      Files.write(target.resolve(".jkube-last-modified"), "123456789".getBytes());
      // When
      ArtifactUtil.warnStaleArtifact(kitLogger, artifact);
      // Then
      assertThat(target.resolve(".jkube-last-modified").toFile())
        .hasContent("" + currentArtifactLastModifiedTime);
    }

    @Test
    @DisplayName("previous build with same time, should log rebuild warning")
    void previousBuildWithSameTime() throws IOException {
      // Given
      Files.write(target.resolve(".jkube-last-modified"), String.valueOf(currentArtifactLastModifiedTime).getBytes());
      // When
      ArtifactUtil.warnStaleArtifact(kitLogger, artifact);
      // Then
      verify(kitLogger).info("Final output artifact file was not rebuilt since last build. " +
        "HINT: try to compile and package your application prior to running the container image build task.");
    }
  }
}
