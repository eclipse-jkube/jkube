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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import org.apache.commons.io.FilenameUtils;

/**
 * Processes resource files through a pipeline of {@link FileContentProcessor}s.
 *
 * <p> Files with the same name (from different source directories) are written to the same target,
 * allowing processors to merge content when duplicates are encountered.
 *
 * <p> Merge state is kept in memory within a single {@code process()} call, so merges are scoped
 * to one pass and cannot leak across separate invocations.
 *
 * <p> Example usage:
 * <pre>
 * File[] result = ResourceFileProcessing.builder()
 *   .withFiles(resourceFiles)
 *   .withOutputDirectory(outDir)
 *   .addProcessor(ctx -&gt; transformContent(ctx))
 *   .addProcessor(ResourceFileProcessors.mergeYamlIfExists())
 *   .process();
 * </pre>
 */
public class ResourceFileProcessing {

  private ResourceFileProcessing() {
  }

  /**
   * Create a new builder for configuring file processing.
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for configuring and executing file processing.
   */
  public static class Builder {
    private File[] files;
    private File outputDirectory;
    private final List<FileContentProcessor> processors = new ArrayList<>();

    /**
     * Set the source files to process.
     *
     * @param files the resource files to process
     * @return this builder
     */
    public Builder withFiles(File... files) {
      this.files = files;
      return this;
    }

    /**
     * Set the output directory where processed files are written.
     *
     * @param outputDirectory the target directory
     * @return this builder
     */
    public Builder withOutputDirectory(File outputDirectory) {
      this.outputDirectory = outputDirectory;
      return this;
    }

    /**
     * Add a processor to the chain. Processors are executed in the order they are added.
     *
     * @param processor the content processor to add
     * @return this builder
     */
    public Builder addProcessor(FileContentProcessor processor) {
      this.processors.add(processor);
      return this;
    }

    /**
     * Process all configured files through the processor chain.
     *
     * <p> For each source file, a target file is resolved in the output directory using the source file's name.
     * If a previous file with the same name was already processed in this call, the accumulated content
     * is passed to processors via {@link ProcessingContext#getExistingContent()}.
     *
     * <p> All content is accumulated in memory and written to disk only after all files are processed,
     * ensuring merges are scoped to a single {@code process()} invocation.
     *
     * @return array of unique processed files (deduplicated by target filename, insertion order preserved)
     * @throws IOException if any processor fails or returns null
     * @throws IllegalStateException if output directory or processors are not configured
     */
    public File[] process() throws IOException {
      if (outputDirectory == null) {
        throw new IllegalStateException("Output directory must be set before processing");
      }
      if (processors.isEmpty()) {
        throw new IllegalStateException("At least one processor must be added before processing");
      }
      if (files == null) {
        return new File[0];
      }

      final Map<String, String> contentByFilename = new LinkedHashMap<>();

      for (File resource : files) {
        final String targetName = FilenameUtils.getName(resource.getPath());
        final File targetFile = new File(outputDirectory, targetName);
        final String existingContent = contentByFilename.get(targetName);

        String processedContent = existingContent;
        for (FileContentProcessor processor : processors) {
          final ProcessingContext context = new ProcessingContext(resource, targetFile, existingContent, processedContent);
          processedContent = processor.process(context);
        }

        if (processedContent == null) {
          throw new IOException(String.format("Processor returned null content for file: %s", resource));
        }
        contentByFilename.put(targetName, processedContent);
      }

      final List<File> result = new ArrayList<>();
      for (Map.Entry<String, String> entry : contentByFilename.entrySet()) {
        final File targetFile = new File(outputDirectory, entry.getKey());
        Files.write(targetFile.toPath(), entry.getValue().getBytes(StandardCharsets.UTF_8));
        result.add(targetFile);
      }

      return result.toArray(new File[0]);
    }
  }

  /**
   * Context passed to each {@link FileContentProcessor} during processing.
   *
   * <p> Contains the source file being processed, the target file being written to,
   * any pre-existing content in the target file, and the output from the previous processor in the chain.
   */
  @Getter
  public static class ProcessingContext {
    private final File sourceFile;
    private final File targetFile;
    private final String existingContent;
    private final String previousOutput;

    /**
     * @param sourceFile      the original source file being processed
     * @param targetFile      the output file in the working directory
     * @param existingContent content already accumulated for this target filename (null if first occurrence)
     * @param previousOutput  output from the previous processor in the chain (null for the first processor)
     */
    public ProcessingContext(File sourceFile, File targetFile, String existingContent, String previousOutput) {
      this.sourceFile = sourceFile;
      this.targetFile = targetFile;
      this.existingContent = existingContent;
      this.previousOutput = previousOutput;
    }
  }

  /**
   * Processes the content of a resource file. Implementations receive a {@link ProcessingContext}
   * and return the transformed content as a string.
   */
  @FunctionalInterface
  public interface FileContentProcessor {
    /**
     * Process the file content.
     *
     * @param context the processing context
     * @return processed content (must not be null)
     * @throws IOException if processing fails
     */
    String process(ProcessingContext context) throws IOException;
  }
}
