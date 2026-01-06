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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceFileProcessingTest {

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
    ResourceFileProcessing.ProcessingResult result = ResourceFileProcessing.builder()
      .withFiles((File[]) null)
      .withOutputDirectory(outDir)
      .addProcessor(ctx -> "processed")
      .process();

    // Then
    assertThat(result.getProcessedFiles()).isEmpty();
  }

  @Test
  void processFiles_withSingleFile_shouldProcessAndReturnFile() throws IOException {
    // Given
    File sourceFile = tempDir.resolve("test.txt").toFile();
    Files.write(sourceFile.toPath(), "original content".getBytes(StandardCharsets.UTF_8));

    // When
    ResourceFileProcessing.ProcessingResult result = ResourceFileProcessing.builder()
      .withFiles(sourceFile)
      .withOutputDirectory(outDir)
      .addProcessor(ctx -> "processed content")
      .process();

    // Then
    assertThat(result.getProcessedFiles()).hasSize(1);
    assertThat(result.getProcessedFiles().get(0)).hasName("test.txt");
    assertThat(new String(Files.readAllBytes(result.getProcessedFiles().get(0).toPath()), StandardCharsets.UTF_8)).isEqualTo("processed content");
  }

  @Test
  void processFiles_withMultipleFiles_shouldProcessAllFiles() throws IOException {
    // Given
    File file1 = tempDir.resolve("file1.txt").toFile();
    File file2 = tempDir.resolve("file2.txt").toFile();
    Files.write(file1.toPath(), "content1".getBytes(StandardCharsets.UTF_8));
    Files.write(file2.toPath(), "content2".getBytes(StandardCharsets.UTF_8));

    // When
    ResourceFileProcessing.ProcessingResult result = ResourceFileProcessing.builder()
      .withFiles(file1, file2)
      .withOutputDirectory(outDir)
      .addProcessor(ctx -> "processed-" + ctx.getSourceFile().getName())
      .process();

    // Then
    assertThat(result.getProcessedFiles()).hasSize(2);
    assertThat(new String(Files.readAllBytes(result.getProcessedFiles().get(0).toPath()), StandardCharsets.UTF_8)).isEqualTo("processed-file1.txt");
    assertThat(new String(Files.readAllBytes(result.getProcessedFiles().get(1).toPath()), StandardCharsets.UTF_8)).isEqualTo("processed-file2.txt");
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
    ResourceFileProcessing.ProcessingResult result = ResourceFileProcessing.builder()
      .withFiles(file1, file2)
      .withOutputDirectory(outDir)
      // Processor 1: Read the source file content
      .addProcessor(ctx -> new String(Files.readAllBytes(ctx.getSourceFile().toPath()), StandardCharsets.UTF_8))
      // Processor 2: Merge with existing content if YAML
      .addProcessor(ctx -> {
        if (ctx.getExistingContent() != null && YamlUtil.isYaml(ctx.getTargetFile())) {
          return YamlUtil.mergeYaml(ctx.getExistingContent(), ctx.getPreviousOutput());
        }
        return ctx.getPreviousOutput();
      })
      .process();

    // Then - Only one file in result (duplicates handled by LinkedHashSet)
    assertThat(result.getProcessedFiles()).hasSize(1);
    assertThat(result.getProcessedFiles().get(0)).hasName("resource.yaml");

    String mergedContent = new String(Files.readAllBytes(result.getProcessedFiles().get(0).toPath()), StandardCharsets.UTF_8);
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
    ResourceFileProcessing.ProcessingResult result = ResourceFileProcessing.builder()
      .withFiles(file1, file2)
      .withOutputDirectory(outDir)
      // Processor 1
      .addProcessor(ctx -> new String(Files.readAllBytes(ctx.getSourceFile().toPath()), StandardCharsets.UTF_8))
      // Processor 2
      .addProcessor(ctx -> ctx.getPreviousOutput())
      .process();

    // Then - Only one file in result, with latest content (no merging)
    assertThat(result.getProcessedFiles()).hasSize(1);
    assertThat(result.getProcessedFiles().get(0)).hasName("resource.txt");
    assertThat(new String(Files.readAllBytes(result.getProcessedFiles().get(0).toPath()), StandardCharsets.UTF_8)).isEqualTo("content2");
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
    ResourceFileProcessing.ProcessingResult result = ResourceFileProcessing.builder()
      .withFiles(file1, file2, file3)
      .withOutputDirectory(outDir)
      .addProcessor(ctx -> new String(Files.readAllBytes(ctx.getSourceFile().toPath()), StandardCharsets.UTF_8))
      .process();

    // Then
    assertThat(result.getProcessedFiles()).hasSize(3);
    assertThat(result.getProcessedFiles().get(0)).hasName("a.txt");
    assertThat(result.getProcessedFiles().get(1)).hasName("b.txt");
    assertThat(result.getProcessedFiles().get(2)).hasName("c.txt");
  }

  @Test
  void processFiles_withNullContentProcessors_shouldThrowException() {
    // Given
    File sourceFile = tempDir.resolve("test.txt").toFile();

    // When & Then
    assertThatThrownBy(() ->
      ResourceFileProcessing.builder()
        .withFiles(sourceFile)
        .withOutputDirectory(outDir)
        .process()
    )
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("At least one processor must be added before processing");
  }

  @Test
  void processFiles_whenProcessorReturnsNull_shouldThrowException() throws IOException {
    // Given
    File sourceFile = tempDir.resolve("test.txt").toFile();
    Files.write(sourceFile.toPath(), "content".getBytes(StandardCharsets.UTF_8));

    // When & Then
    assertThatThrownBy(() ->
      ResourceFileProcessing.builder()
        .withFiles(sourceFile)
        .withOutputDirectory(outDir)
        .addProcessor(ctx -> null)
        .process()
    )
      .isInstanceOf(IOException.class)
      .hasMessageContaining("Processor returned null content for file:");
  }

  @Test
  void processFiles_withMultipleProcessors_shouldChainCorrectly() throws IOException {
    // Given
    File sourceFile = tempDir.resolve("test.txt").toFile();
    Files.write(sourceFile.toPath(), "original".getBytes(StandardCharsets.UTF_8));

    // When - Chain 3 processors
    ResourceFileProcessing.ProcessingResult result = ResourceFileProcessing.builder()
      .withFiles(sourceFile)
      .withOutputDirectory(outDir)
      // Processor 1: Read source file
      .addProcessor(ctx -> new String(Files.readAllBytes(ctx.getSourceFile().toPath()), StandardCharsets.UTF_8))
      // Processor 2: Transform content
      .addProcessor(ctx -> ctx.getPreviousOutput() + "-transformed")
      // Processor 3: Add suffix
      .addProcessor(ctx -> ctx.getPreviousOutput() + "-final")
      .process();

    // Then
    assertThat(result.getProcessedFiles()).hasSize(1);
    String content = new String(Files.readAllBytes(result.getProcessedFiles().get(0).toPath()), StandardCharsets.UTF_8);
    assertThat(content).isEqualTo("original-transformed-final");
  }

  @Test
  void processFiles_withExistingContent_shouldPassToProcessors() throws IOException {
    // Given - First create a file in output directory
    File targetFile = new File(outDir, "existing.txt");
    Files.write(targetFile.toPath(), "existing content".getBytes(StandardCharsets.UTF_8));

    File sourceFile = tempDir.resolve("existing.txt").toFile();
    Files.write(sourceFile.toPath(), "new content".getBytes(StandardCharsets.UTF_8));

    // When
    ResourceFileProcessing.ProcessingResult result = ResourceFileProcessing.builder()
      .withFiles(sourceFile)
      .withOutputDirectory(outDir)
      .addProcessor(ctx -> {
        // Verify existingContent is passed correctly
        assertThat(ctx.getExistingContent()).isEqualTo("existing content");
        return new String(Files.readAllBytes(ctx.getSourceFile().toPath()), StandardCharsets.UTF_8);
      })
      .addProcessor(ctx -> {
        // Second processor should also receive original existingContent
        assertThat(ctx.getExistingContent()).isEqualTo("existing content");
        // But prev should be from first processor
        assertThat(ctx.getPreviousOutput()).isEqualTo("new content");
        return ctx.getPreviousOutput() + " appended";
      })
      .process();

    // Then
    assertThat(result.getProcessedFiles()).hasSize(1);
    String content = new String(Files.readAllBytes(result.getProcessedFiles().get(0).toPath()), StandardCharsets.UTF_8);
    assertThat(content).isEqualTo("new content appended");
  }
}
