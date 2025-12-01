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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceFileProcessorTest {

  @TempDir
  Path tempDir;

  private File outDir;

  @BeforeEach
  void setUp() {
    outDir = tempDir.resolve("output").toFile();
    assertThat(outDir.mkdirs()).isTrue();
  }

  @Test
  void processFiles_withNullInput_shouldReturnEmptyArray() throws IOException {
    // When
    File[] result = ResourceFileProcessor.processFiles(null, outDir, (source, target) -> "processed");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void processFiles_withSingleFile_shouldProcessAndReturnFile() throws IOException {
    // Given
    File sourceFile = tempDir.resolve("test.txt").toFile();
    Files.write(sourceFile.toPath(), "original content".getBytes(StandardCharsets.UTF_8));

    // When
    File[] result = ResourceFileProcessor.processFiles(
      new File[]{sourceFile},
      outDir,
      (source, target) -> "processed content"
    );

    // Then
    assertThat(result).hasSize(1);
    assertThat(result[0]).hasName("test.txt");
    assertThat(new String(Files.readAllBytes(result[0].toPath()), StandardCharsets.UTF_8)).isEqualTo("processed content");
  }

  @Test
  void processFiles_withMultipleFiles_shouldProcessAllFiles() throws IOException {
    // Given
    File file1 = tempDir.resolve("file1.txt").toFile();
    File file2 = tempDir.resolve("file2.txt").toFile();
    Files.write(file1.toPath(), "content1".getBytes(StandardCharsets.UTF_8));
    Files.write(file2.toPath(), "content2".getBytes(StandardCharsets.UTF_8));

    // When
    File[] result = ResourceFileProcessor.processFiles(
      new File[]{file1, file2},
      outDir,
      (source, target) -> "processed-" + source.getName()
    );

    // Then
    assertThat(result).hasSize(2);
    assertThat(new String(Files.readAllBytes(result[0].toPath()), StandardCharsets.UTF_8)).isEqualTo("processed-file1.txt");
    assertThat(new String(Files.readAllBytes(result[1].toPath()), StandardCharsets.UTF_8)).isEqualTo("processed-file2.txt");
  }

  @Test
  void processFiles_withDuplicateFileNames_shouldMergeYamlFiles() throws IOException {
    // Given
    File file1 = tempDir.resolve("resource.yaml").toFile();
    File file2 = tempDir.resolve("resource.yaml").toFile(); // Same name

    String yaml1 = "apiVersion: v1\nkind: Service\nmetadata:\n  name: myservice";
    String yaml2 = "apiVersion: v1\nkind: Service\nmetadata:\n  labels:\n    app: myapp";

    Files.write(file1.toPath(), yaml1.getBytes(StandardCharsets.UTF_8));

    // When - Process first file
    ResourceFileProcessor.processFiles(
      new File[]{file1},
      outDir,
      (source, target) -> yaml1
    );

    // When - Process second file with same name (should merge)
    File[] result2 = ResourceFileProcessor.processFiles(
      new File[]{file2},
      outDir,
      (source, target) -> yaml2
    );

    // Then
    assertThat(result2).hasSize(1);
    String mergedContent = new String(Files.readAllBytes(result2[0].toPath()), StandardCharsets.UTF_8);
    // The merged YAML should contain properties from both
    assertThat(mergedContent).contains("name:", "labels:");
  }

  @Test
  void processFiles_withNonYamlFiles_shouldNotMerge() throws IOException {
    // Given
    File file1 = tempDir.resolve("resource.txt").toFile();
    Files.write(file1.toPath(), "content1".getBytes(StandardCharsets.UTF_8));

    // When - Process first file
    ResourceFileProcessor.processFiles(
      new File[]{file1},
      outDir,
      (source, target) -> "content1"
    );

    // When - Process second file with same name (should overwrite, not merge)
    File[] result = ResourceFileProcessor.processFiles(
      new File[]{file1},
      outDir,
      (source, target) -> "content2"
    );

    // Then
    assertThat(result).hasSize(1);
    assertThat(new String(Files.readAllBytes(result[0].toPath()), StandardCharsets.UTF_8)).isEqualTo("content2");
  }

  @Test
  void processFiles_maintainsFileOrder() throws IOException {
    // Given
    File file1 = tempDir.resolve("a.txt").toFile();
    File file2 = tempDir.resolve("b.txt").toFile();
    File file3 = tempDir.resolve("c.txt").toFile();
    Files.write(file1.toPath(), "a".getBytes(StandardCharsets.UTF_8));
    Files.write(file2.toPath(), "b".getBytes(StandardCharsets.UTF_8));
    Files.write(file3.toPath(), "c".getBytes(StandardCharsets.UTF_8));

    // When
    File[] result = ResourceFileProcessor.processFiles(
      new File[]{file1, file2, file3},
      outDir,
      (source, target) -> new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8)
    );

    // Then
    assertThat(result).hasSize(3);
    assertThat(result[0]).hasName("a.txt");
    assertThat(result[1]).hasName("b.txt");
    assertThat(result[2]).hasName("c.txt");
  }
}