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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.jkube.kit.common.util.YamlUtil.getPropertiesFromYamlString;

class ResourceFileProcessingTest {

  @TempDir
  Path tempDir;

  private File outDir;

  @BeforeEach
  void setUp() {
    outDir = tempDir.resolve("output").toFile();
    assertThat(outDir.mkdirs()).isTrue();
  }

  @Nested
  @DisplayName("process")
  class Process {

    @Test
    @DisplayName("with null input, should return empty array")
    void withNullInput_shouldReturnEmptyArray() throws IOException {
      File[] result = ResourceFileProcessing.builder()
        .withFiles((File[]) null)
        .withOutputDirectory(outDir)
        .addProcessor(ctx -> "processed")
        .process();

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("with single file, should process and return file")
    void withSingleFile_shouldProcessAndReturnFile() throws IOException {
      File sourceFile = tempDir.resolve("test.txt").toFile();
      Files.write(sourceFile.toPath(), "original content".getBytes(StandardCharsets.UTF_8));

      File[] result = ResourceFileProcessing.builder()
        .withFiles(sourceFile)
        .withOutputDirectory(outDir)
        .addProcessor(ctx -> "processed content")
        .process();

      assertThat(result).hasSize(1);
      assertThat(result[0]).hasName("test.txt");
      assertThat(new String(Files.readAllBytes(result[0].toPath()), StandardCharsets.UTF_8))
        .isEqualTo("processed content");
    }

    @Test
    @DisplayName("with multiple files, should process all files")
    void withMultipleFiles_shouldProcessAllFiles() throws IOException {
      File file1 = tempDir.resolve("file1.txt").toFile();
      File file2 = tempDir.resolve("file2.txt").toFile();
      Files.write(file1.toPath(), "content1".getBytes(StandardCharsets.UTF_8));
      Files.write(file2.toPath(), "content2".getBytes(StandardCharsets.UTF_8));

      File[] result = ResourceFileProcessing.builder()
        .withFiles(file1, file2)
        .withOutputDirectory(outDir)
        .addProcessor(ctx -> "processed-" + ctx.getSourceFile().getName())
        .process();

      assertThat(result).hasSize(2);
      assertThat(new String(Files.readAllBytes(result[0].toPath()), StandardCharsets.UTF_8))
        .isEqualTo("processed-file1.txt");
      assertThat(new String(Files.readAllBytes(result[1].toPath()), StandardCharsets.UTF_8))
        .isEqualTo("processed-file2.txt");
    }

    @Test
    @DisplayName("with duplicate YAML filenames, should merge")
    void withDuplicateYamlFileNames_shouldMerge() throws IOException {
      File sourceDir1 = tempDir.resolve("fragments1").toFile();
      File sourceDir2 = tempDir.resolve("fragments2").toFile();
      assertThat(sourceDir1.mkdirs()).isTrue();
      assertThat(sourceDir2.mkdirs()).isTrue();

      File file1 = new File(sourceDir1, "resource.yaml");
      File file2 = new File(sourceDir2, "resource.yaml");

      Files.write(file1.toPath(), "apiVersion: v1\nkind: Service\nmetadata:\n  name: myservice"
        .getBytes(StandardCharsets.UTF_8));
      Files.write(file2.toPath(), "apiVersion: v1\nkind: Service\nmetadata:\n  labels:\n    app: myapp"
        .getBytes(StandardCharsets.UTF_8));

      File[] result = ResourceFileProcessing.builder()
        .withFiles(file1, file2)
        .withOutputDirectory(outDir)
        .addProcessor(ctx -> new String(Files.readAllBytes(ctx.getSourceFile().toPath()), StandardCharsets.UTF_8))
        .addProcessor(ResourceFileProcessors.mergeYamlIfExists())
        .process();

      assertThat(result).hasSize(1);
      assertThat(result[0]).hasName("resource.yaml");

      String mergedContent = new String(Files.readAllBytes(result[0].toPath()), StandardCharsets.UTF_8);
      Properties props = getPropertiesFromYamlString(mergedContent);
      assertThat(props)
        .containsEntry("metadata.name", "myservice")
        .containsEntry("metadata.labels.app", "myapp");
    }

    @Test
    @DisplayName("with duplicate non-YAML filenames, should overwrite")
    void withDuplicateNonYamlFileNames_shouldOverwrite() throws IOException {
      File sourceDir1 = tempDir.resolve("fragments1").toFile();
      File sourceDir2 = tempDir.resolve("fragments2").toFile();
      assertThat(sourceDir1.mkdirs()).isTrue();
      assertThat(sourceDir2.mkdirs()).isTrue();

      File file1 = new File(sourceDir1, "resource.txt");
      File file2 = new File(sourceDir2, "resource.txt");

      Files.write(file1.toPath(), "content1".getBytes(StandardCharsets.UTF_8));
      Files.write(file2.toPath(), "content2".getBytes(StandardCharsets.UTF_8));

      File[] result = ResourceFileProcessing.builder()
        .withFiles(file1, file2)
        .withOutputDirectory(outDir)
        .addProcessor(ctx -> new String(Files.readAllBytes(ctx.getSourceFile().toPath()), StandardCharsets.UTF_8))
        .addProcessor(ctx -> ctx.getPreviousOutput())
        .process();

      assertThat(result).hasSize(1);
      assertThat(result[0]).hasName("resource.txt");
      assertThat(new String(Files.readAllBytes(result[0].toPath()), StandardCharsets.UTF_8))
        .isEqualTo("content2");
    }

    @Test
    @DisplayName("should maintain file order")
    void shouldMaintainFileOrder() throws IOException {
      File file1 = tempDir.resolve("a.txt").toFile();
      File file2 = tempDir.resolve("b.txt").toFile();
      File file3 = tempDir.resolve("c.txt").toFile();
      Files.write(file1.toPath(), "a".getBytes(StandardCharsets.UTF_8));
      Files.write(file2.toPath(), "b".getBytes(StandardCharsets.UTF_8));
      Files.write(file3.toPath(), "c".getBytes(StandardCharsets.UTF_8));

      File[] result = ResourceFileProcessing.builder()
        .withFiles(file1, file2, file3)
        .withOutputDirectory(outDir)
        .addProcessor(ctx -> new String(Files.readAllBytes(ctx.getSourceFile().toPath()), StandardCharsets.UTF_8))
        .process();

      assertThat(result).hasSize(3);
      assertThat(result[0]).hasName("a.txt");
      assertThat(result[1]).hasName("b.txt");
      assertThat(result[2]).hasName("c.txt");
    }

    @Test
    @DisplayName("with no processors, should throw exception")
    void withNoProcessors_shouldThrowException() {
      File sourceFile = tempDir.resolve("test.txt").toFile();

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
    @DisplayName("when processor returns null, should throw exception")
    void whenProcessorReturnsNull_shouldThrowException() throws IOException {
      File sourceFile = tempDir.resolve("test.txt").toFile();
      Files.write(sourceFile.toPath(), "content".getBytes(StandardCharsets.UTF_8));

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
    @DisplayName("with multiple processors, should chain correctly")
    void withMultipleProcessors_shouldChainCorrectly() throws IOException {
      File sourceFile = tempDir.resolve("test.txt").toFile();
      Files.write(sourceFile.toPath(), "original".getBytes(StandardCharsets.UTF_8));

      File[] result = ResourceFileProcessing.builder()
        .withFiles(sourceFile)
        .withOutputDirectory(outDir)
        .addProcessor(ctx -> new String(Files.readAllBytes(ctx.getSourceFile().toPath()), StandardCharsets.UTF_8))
        .addProcessor(ctx -> ctx.getPreviousOutput() + "-transformed")
        .addProcessor(ctx -> ctx.getPreviousOutput() + "-final")
        .process();

      assertThat(result).hasSize(1);
      String content = new String(Files.readAllBytes(result[0].toPath()), StandardCharsets.UTF_8);
      assertThat(content).isEqualTo("original-transformed-final");
    }

    @Test
    @DisplayName("with existing content, should pass to processors")
    void withExistingContent_shouldPassToProcessors() throws IOException {
      File targetFile = new File(outDir, "existing.txt");
      Files.write(targetFile.toPath(), "existing content".getBytes(StandardCharsets.UTF_8));

      File sourceFile = tempDir.resolve("existing.txt").toFile();
      Files.write(sourceFile.toPath(), "new content".getBytes(StandardCharsets.UTF_8));

      File[] result = ResourceFileProcessing.builder()
        .withFiles(sourceFile)
        .withOutputDirectory(outDir)
        .addProcessor(ctx -> {
          assertThat(ctx.getExistingContent()).isEqualTo("existing content");
          return new String(Files.readAllBytes(ctx.getSourceFile().toPath()), StandardCharsets.UTF_8);
        })
        .addProcessor(ctx -> {
          assertThat(ctx.getExistingContent()).isEqualTo("existing content");
          assertThat(ctx.getPreviousOutput()).isEqualTo("new content");
          return ctx.getPreviousOutput() + " appended";
        })
        .process();

      assertThat(result).hasSize(1);
      String content = new String(Files.readAllBytes(result[0].toPath()), StandardCharsets.UTF_8);
      assertThat(content).isEqualTo("new content appended");
    }
  }
}
