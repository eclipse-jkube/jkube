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
package org.eclipse.jkube.kit.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BuildReferenceDateUtilTest {

  @TempDir
  File temporaryFolder;

  @Test
  void testGetBuildReferenceDateWhenFileDoesntExist() throws IOException {
    assertThat(BuildReferenceDateUtil.getBuildReferenceDate("target", "docker")).isNotNull();
  }

  @Test
  void testGetBuildReferenceDate() throws IOException {
    // Given
    File buildDirectory = Files.createDirectories(temporaryFolder.toPath().resolve("testGetBuildReferenceDate")).toFile();
    File buildTimestampFile = new File(buildDirectory, "build.timestamp");
    String timestamp = "1605029866235";
    boolean fileCreated = buildTimestampFile.createNewFile();
    try (FileWriter fileWriter = new FileWriter(buildTimestampFile)) {
      fileWriter.write(timestamp);
    }

    // When
    Date result = BuildReferenceDateUtil.getBuildReferenceDate(buildDirectory.getAbsolutePath(), buildTimestampFile.getName());

    // Then
    assertThat(fileCreated).isTrue();
    assertThat(result).isNotNull();
    assertThat(result.getTime()).isEqualTo(Long.parseLong(timestamp));
  }

  @Test
  void testGetBuildTimestampFile() {
    // Given
    String projectBuildDirectory = "target" + File.separator + "docker";
    String dockerBuildTimestampFile = "build.timestamp";

    // When
    File result = BuildReferenceDateUtil.getBuildTimestampFile(projectBuildDirectory, dockerBuildTimestampFile);

    // Then
    assertThat(result).isEqualTo(Paths.get("target", "docker", "build.timestamp").toFile());
  }

  @Test
  void testGetBuildTimestampFromPluginContext() throws IOException {
    // Given
    File buildDirectory = new File(temporaryFolder, "testGetBuildTimestampFromPluginContext");
    File buildTimestampFile = new File(buildDirectory, "build.timestamp");
    String timestamp = "1605029866235";
    Map<String, Object> pluginContext = new HashMap<>();
    String buildTimestampContextKey = "buildTimestampContextKey";
    pluginContext.put(buildTimestampContextKey, Date.from(Instant.ofEpochMilli(Long.parseLong(timestamp))));

    // When
    Date result = BuildReferenceDateUtil.getBuildTimestamp(pluginContext, buildTimestampContextKey, buildDirectory.getPath(),
        buildTimestampFile.getName());

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getTime()).isEqualTo(Long.parseLong(timestamp));
  }

  @Test
  void testGetBuildTimestampFromFile() throws IOException {
    // Given
    File buildDirectory = Files.createDirectories(temporaryFolder.toPath().resolve("testGetBuildTimestampFromFile")).toFile();
    File buildTimestampFile = new File(buildDirectory, "build.timestamp");
    String timestamp = "1605029866235";
    boolean fileCreated = buildTimestampFile.createNewFile();
    try (FileWriter fileWriter = new FileWriter(buildTimestampFile)) {
      fileWriter.write(timestamp);
    }
    Map<String, Object> pluginContext = new HashMap<>();
    String buildTimestampContextKey = "buildTimestampContextKey";

    // When
    Date result = BuildReferenceDateUtil.getBuildTimestamp(pluginContext, buildTimestampContextKey, buildDirectory.getPath(),
        buildTimestampFile.getName());

    // Then
    assertThat(fileCreated).isTrue();
    assertThat(result).isNotNull();
    assertThat(result.getTime()).isEqualTo(Long.parseLong(timestamp));
  }
}
