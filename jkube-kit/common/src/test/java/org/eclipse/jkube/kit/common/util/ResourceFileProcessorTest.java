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
    File[] result = ResourceFileProcessor.processFiles(null, outDir, (source, target, existing, prev) -> "processed");

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
      (source, target, existing, prev) -> "processed content"
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
      (source, target, existing, prev) -> "processed-" + source.getName()
    );

    // Then
    assertThat(result).hasSize(2);
    assertThat(new String(Files.readAllBytes(result[0].toPath()), StandardCharsets.UTF_8)).isEqualTo("processed-file1.txt");
    assertThat(new String(Files.readAllBytes(result[1].toPath()), StandardCharsets.UTF_8)).isEqualTo("processed-file2.txt");
  }

  @Test
  void processFiles_withDuplicateFileNames_shouldMergeYamlFiles() throws IOException {
    // Given - Two source files with same target name (simulating fragments from different directories)
    File sourceDir1 = tempDir.resolve("fragments1").toFile();
    File sourceDir2 = tempDir.resolve("fragments2").toFile();
    assertThat(sourceDir1.mkdirs()).isTrue();
    assertThat(sourceDir2.mkdirs()).isTrue();

    File file1 = new File(sourceDir1, "resource.yaml");
    File file2 = new File(sourceDir2, "resource.yaml");

    String yaml1 = "apiVersion: v1\nkind: Service\nmetadata:\n  name: myservice";
    String yaml2 = "apiVersion: v1\nkind: Service\nmetadata:\n  labels:\n    app: myapp";

    Files.write(file1.toPath(), yaml1.getBytes(StandardCharsets.UTF_8));
    Files.write(file2.toPath(), yaml2.getBytes(StandardCharsets.UTF_8));

    // When - Process both files in a single call (as happens in real usage)
    File[] result = ResourceFileProcessor.processFiles(
      new File[]{file1, file2},
      outDir,
      // Processor 1: Read the source file content
      (source, target, existingContent, prev) ->
        new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8),
      // Processor 2: Merge with existing content if YAML
      (source, target, existingContent, prev) -> {
        if (existingContent != null && YamlUtil.isYaml(target)) {
          return YamlUtil.mergeYaml(existingContent, prev);
        }
        return prev;
      }
    );

    // Then - Only one file in result (duplicates handled by LinkedHashSet)
    assertThat(result).hasSize(1);
    assertThat(result[0]).hasName("resource.yaml");

    String mergedContent = new String(Files.readAllBytes(result[0].toPath()), StandardCharsets.UTF_8);
    // The merged YAML should contain properties from both
    assertThat(mergedContent).contains("name:", "labels:");
  }

  @Test
  void processFiles_withDuplicateNonYamlFiles_shouldOverwriteNotMerge() throws IOException {
    // Given - Two source files with same target name (non-YAML files)
    File sourceDir1 = tempDir.resolve("fragments1").toFile();
    File sourceDir2 = tempDir.resolve("fragments2").toFile();
    assertThat(sourceDir1.mkdirs()).isTrue();
    assertThat(sourceDir2.mkdirs()).isTrue();

    File file1 = new File(sourceDir1, "resource.txt");
    File file2 = new File(sourceDir2, "resource.txt");

    Files.write(file1.toPath(), "content1".getBytes(StandardCharsets.UTF_8));
    Files.write(file2.toPath(), "content2".getBytes(StandardCharsets.UTF_8));

    // When - Process both non-YAML files in a single call
    File[] result = ResourceFileProcessor.processFiles(
      new File[]{file1, file2},
      outDir,
      // Processor 1
      (source, target, existingContent, prev) ->
        new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8),
      // Processor 2
      (source, target, existingContent, prev) -> {
        // Non-YAML files should NOT merge, just use latest content
        return prev;
      }
    );

    // Then - Only one file in result, with latest content (no merging)
    assertThat(result).hasSize(1);
    assertThat(result[0]).hasName("resource.txt");
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
      (source, target, existing, prev) -> new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8)
    );

    // Then
    assertThat(result).hasSize(3);
    assertThat(result[0]).hasName("a.txt");
    assertThat(result[1]).hasName("b.txt");
    assertThat(result[2]).hasName("c.txt");
  }
}