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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BuildReferenceDateUtilTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testGetBuildReferenceDateWhenFileDoesntExist() throws IOException {
    assertNotNull(BuildReferenceDateUtil.getBuildReferenceDate("target", "docker"));
  }

  @Test
  public void testGetBuildReferenceDate() throws IOException {
    // Given
    File buildDirectory = temporaryFolder.newFolder("testGetBuildReferenceDate");
    File buildTimestampFile = new File(buildDirectory, "build.timestamp");
    String timestamp = "1605029866235";
    boolean fileCreated = buildTimestampFile.createNewFile();
    try (FileWriter fileWriter = new FileWriter(buildTimestampFile)) {
      fileWriter.write(timestamp);
    }

    // When
    Date result = BuildReferenceDateUtil.getBuildReferenceDate(buildDirectory.getAbsolutePath(), buildTimestampFile.getName());

    // Then
    assertTrue(fileCreated);
    assertNotNull(result);
    assertEquals(Long.parseLong(timestamp), result.getTime());
  }

  @Test
  public void testGetBuildTimestampFile() {
    // Given
    String projectBuildDirectory = "target/docker";
    String dockerBuildTimestampFile = "build.timestamp";

    // When
    File result = BuildReferenceDateUtil.getBuildTimestampFile(projectBuildDirectory, dockerBuildTimestampFile);

    // Then
    assertNotNull(result);
    assertEquals("target/docker/build.timestamp", result.getPath());
  }

  @Test
  public void testGetBuildTimestampFromPluginContext() throws IOException {
    // Given
    File buildDirectory = temporaryFolder.newFolder("testGetBuildTimestampFromPluginContext");
    File buildTimestampFile = new File(buildDirectory, "build.timestamp");
    String timestamp = "1605029866235";
    Map<String, Object> pluginContext = new HashMap<>();
    String buildTimestampContextKey = "buildTimestampContextKey";
    pluginContext.put(buildTimestampContextKey, Date.from(Instant.ofEpochMilli(Long.parseLong(timestamp))));

    // When
    Date result = BuildReferenceDateUtil.getBuildTimestamp(pluginContext, buildTimestampContextKey, buildDirectory.getPath(),
        buildTimestampFile.getName());

    // Then
    assertNotNull(result);
    assertEquals(Long.parseLong(timestamp), result.getTime());
  }

  @Test
  public void testGetBuildTimestampFromFile() throws IOException {
    // Given
    File buildDirectory = temporaryFolder.newFolder("testGetBuildTimestampFromFile");
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
    assertTrue(fileCreated);
    assertNotNull(result);
    assertEquals(Long.parseLong(timestamp), result.getTime());
  }
}