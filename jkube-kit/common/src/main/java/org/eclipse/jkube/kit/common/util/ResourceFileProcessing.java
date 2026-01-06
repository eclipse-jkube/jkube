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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import org.apache.commons.io.FilenameUtils;

/**
 * Main class for processing resource files through a pipeline of processors.
 * Provides a fluent builder API for configuring and executing file processing operations.
 *
 * <p>Example usage:</p>
 * <pre>
 * ProcessingResult result = ResourceFileProcessing.builder()
 *   .withFiles(resourceFiles)
 *   .withOutputDirectory(outDir)
 *   .addProcessor(ctx -> transformContent(ctx))
 *   .addProcessor(ResourceFileProcessors.mergeYamlIfExists())
 *   .process();
 * </pre>
 */
public class ResourceFileProcessing {

  private ResourceFileProcessing() {
    // Utility class
  }

  /**
   * Create a new builder for fluent file processing.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for processing files with a chainable API.
   */
  public static class Builder {
    private File[] files;
    private File outputDirectory;
    private ProcessingOptions options = ProcessingOptions.defaults();
    private final List<FileContentProcessor> processors = new ArrayList<>();

    /**
     * Set the files to process.
     *
     * @param files the files to process
     * @return this builder
     */
    public Builder withFiles(File... files) {
      this.files = files;
      return this;
    }

    /**
     * Set the output directory.
     *
     * @param outputDirectory the output directory
     * @return this builder
     */
    public Builder withOutputDirectory(File outputDirectory) {
      this.outputDirectory = outputDirectory;
      return this;
    }

    /**
     * Set the processing options.
     *
     * @param options the processing options
     * @return this builder
     */
    public Builder withOptions(ProcessingOptions options) {
      this.options = options;
      return this;
    }

    /**
     * Add a processor to the chain.
     *
     * @param processor the processor to add
     * @return this builder
     */
    public Builder addProcessor(FileContentProcessor processor) {
      this.processors.add(processor);
      return this;
    }

    /**
     * Process the files with the configured processors.
     *
     * @return the processing result
     * @throws IOException if processing fails
     * @throws IllegalStateException if files or output directory are not set
     */
    public ProcessingResult process() throws IOException {
      if (outputDirectory == null) {
        throw new IllegalStateException("Output directory must be set before processing");
      }
      if (processors.isEmpty()) {
        throw new IllegalStateException("At least one processor must be added before processing");
      }

      return processFiles(files, outputDirectory, options, processors.toArray(new FileContentProcessor[0]));
    }

    /**
     * Process resource files with custom options.
     *
     * @param resourceFiles     the array of resource files to process
     * @param outDir            the output directory for processed files
     * @param options           processing options
     * @param contentProcessors functions to process file content
     * @return processing result with files and metadata
     * @throws IOException if file processing fails and error strategy is FAIL_FAST
     */
    private ProcessingResult processFiles(File[] resourceFiles, File outDir, ProcessingOptions options,
                                          FileContentProcessor... contentProcessors) throws IOException {
      if (resourceFiles == null) {
        return new ProcessingResult(Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), 0);
      }

      if (contentProcessors == null || contentProcessors.length == 0) {
        throw new IllegalArgumentException("At least one content processor is required");
      }

      long startTime = System.currentTimeMillis();
      Set<File> processedFiles = new LinkedHashSet<>();
      List<ProcessingError> errors = new ArrayList<>();
      Map<File, ProcessingMetadata> metadata = new HashMap<>();

      for (File resource : resourceFiles) {
        try {
          File targetFile = options.getNamingStrategy().getTargetFile(resource, outDir);
          long fileStartTime = System.currentTimeMillis();

          // Read existing content if file already exists
          String existingContent = null;
          if (targetFile.exists()) {
            existingContent = new String(Files.readAllBytes(targetFile.toPath()), options.getEncoding());
          }

          // Create processing context
          ProcessingContext context = new ProcessingContext(resource, targetFile, existingContent, existingContent);

          // Chain processors
          String processedContent = existingContent;
          for (FileContentProcessor processor : contentProcessors) {
            context = new ProcessingContext(resource, targetFile, existingContent, processedContent, context.getAttributes());
            processedContent = processor.process(context);
          }

          // Write the final content to the target file
          if (processedContent == null) {
            throw new IOException(String.format("Processor returned null content for file: %s", resource));
          }
          Files.write(targetFile.toPath(), processedContent.getBytes(options.getEncoding()));

          // Track metadata
          long processingTime = System.currentTimeMillis() - fileStartTime;
          metadata.put(targetFile, new ProcessingMetadata(resource, processingTime, processedContent.length()));

          processedFiles.add(targetFile);
        } catch (IOException e) {
          ProcessingError error = new ProcessingError(resource, e);
          errors.add(error);

          if (options.getErrorStrategy() == ErrorStrategy.FAIL_FAST) {
            throw e;
          }
        }
      }

      long totalTime = System.currentTimeMillis() - startTime;
      return new ProcessingResult(new ArrayList<>(processedFiles), errors, metadata, totalTime);
    }
  }

  /**
   * Strategy for handling errors during file processing.
   */
  public enum ErrorStrategy {
    /** Stop processing on first error and throw exception */
    FAIL_FAST,
    /** Skip failed files and continue processing, collect errors */
    SKIP_ON_ERROR
  }

  /**
   * Configuration options for file processing.
   */
  @Getter
  public static class ProcessingOptions {
    private final Charset encoding;
    private final ErrorStrategy errorStrategy;
    private final FileNamingStrategy namingStrategy;

    private ProcessingOptions(Charset encoding, ErrorStrategy errorStrategy, FileNamingStrategy namingStrategy) {
      this.encoding = encoding;
      this.errorStrategy = errorStrategy;
      this.namingStrategy = namingStrategy;
    }

    public static ProcessingOptions defaults() {
      return new ProcessingOptions(StandardCharsets.UTF_8, ErrorStrategy.FAIL_FAST, new DefaultFileNamingStrategy());
    }

    public static OptionsBuilder builder() {
      return new OptionsBuilder();
    }

    public static class OptionsBuilder {
      private Charset encoding = StandardCharsets.UTF_8;
      private ErrorStrategy errorStrategy = ErrorStrategy.FAIL_FAST;
      private FileNamingStrategy namingStrategy = new DefaultFileNamingStrategy();

      public OptionsBuilder withEncoding(Charset encoding) {
        this.encoding = encoding;
        return this;
      }

      public OptionsBuilder withErrorStrategy(ErrorStrategy errorStrategy) {
        this.errorStrategy = errorStrategy;
        return this;
      }

      public OptionsBuilder withNamingStrategy(FileNamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
        return this;
      }

      public ProcessingOptions build() {
        return new ProcessingOptions(encoding, errorStrategy, namingStrategy);
      }
    }
  }

  /**
   * Context object passed to processors containing file information and shared state.
   */
  @Getter
  public static class ProcessingContext {
    private final File sourceFile;
    private final File targetFile;
    private final String existingContent;
    private final String previousOutput;
    private final Map<String, Object> attributes;

    public ProcessingContext(File sourceFile, File targetFile, String existingContent, String previousOutput) {
      this(sourceFile, targetFile, existingContent, previousOutput, new HashMap<>());
    }

    public ProcessingContext(File sourceFile, File targetFile, String existingContent, String previousOutput,
                            Map<String, Object> attributes) {
      this.sourceFile = sourceFile;
      this.targetFile = targetFile;
      this.existingContent = existingContent;
      this.previousOutput = previousOutput;
      this.attributes = new HashMap<>(attributes);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
      return (T) attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
      attributes.put(key, value);
    }
  }

  /**
   * Result of file processing operation.
   */
  @Getter
  public static class ProcessingResult {
    private final List<File> processedFiles;
    private final List<ProcessingError> errors;
    private final Map<File, ProcessingMetadata> metadata;
    private final long totalProcessingTime;

    public ProcessingResult(List<File> processedFiles, List<ProcessingError> errors,
                           Map<File, ProcessingMetadata> metadata, long totalProcessingTime) {
      this.processedFiles = Collections.unmodifiableList(processedFiles);
      this.errors = Collections.unmodifiableList(errors);
      this.metadata = Collections.unmodifiableMap(metadata);
      this.totalProcessingTime = totalProcessingTime;
    }

    public boolean hasErrors() {
      return !errors.isEmpty();
    }

    public List<File> getSuccessfulFiles() {
      return processedFiles;
    }

    public List<File> getFailedFiles() {
      List<File> failed = new ArrayList<>();
      for (ProcessingError error : errors) {
        failed.add(error.getSourceFile());
      }
      return failed;
    }
  }

  /**
   * Metadata about a processed file.
   */
  @Getter
  public static class ProcessingMetadata {
    private final File sourceFile;
    private final long processingTimeMs;
    private final long outputSize;

    public ProcessingMetadata(File sourceFile, long processingTimeMs, long outputSize) {
      this.sourceFile = sourceFile;
      this.processingTimeMs = processingTimeMs;
      this.outputSize = outputSize;
    }
  }

  /**
   * Error information for a failed file processing.
   */
  @Getter
  public static class ProcessingError {
    private final File sourceFile;
    private final IOException exception;

    public ProcessingError(File sourceFile, IOException exception) {
      this.sourceFile = sourceFile;
      this.exception = exception;
    }
  }

  /**
   * Strategy for determining target file names.
   */
  @FunctionalInterface
  public interface FileNamingStrategy {
    File getTargetFile(File sourceFile, File outDir);
  }

  /**
   * Default naming strategy that uses the source file name.
   */
  public static class DefaultFileNamingStrategy implements FileNamingStrategy {
    @Override
    public File getTargetFile(File sourceFile, File outDir) {
      String sanitizedName = FilenameUtils.getName(sourceFile.getPath());
      return new File(outDir, sanitizedName);
    }
  }

  /**
   * Functional interface for processing file content with context-based API.
   */
  @FunctionalInterface
  public interface FileContentProcessor {
    /**
     * Process the content of a resource file using the processing context.
     *
     * @param context the processing context containing source file, target file, content, and shared state
     * @return processed content as string
     * @throws IOException if processing fails
     */
    String process(ProcessingContext context) throws IOException;
  }
}