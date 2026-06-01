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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import org.apache.commons.io.FilenameUtils;

public class ResourceFileProcessing {

  private ResourceFileProcessing() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private File[] files;
    private File outputDirectory;
    private final List<FileContentProcessor> processors = new ArrayList<>();

    public Builder withFiles(File... files) {
      this.files = files;
      return this;
    }

    public Builder withOutputDirectory(File outputDirectory) {
      this.outputDirectory = outputDirectory;
      return this;
    }

    public Builder addProcessor(FileContentProcessor processor) {
      this.processors.add(processor);
      return this;
    }

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

      final Set<File> processedFiles = new LinkedHashSet<>();

      for (File resource : files) {
        final File targetFile = new File(outputDirectory, FilenameUtils.getName(resource.getPath()));

        String existingContent = null;
        if (targetFile.exists()) {
          existingContent = new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);
        }

        String processedContent = existingContent;
        for (FileContentProcessor processor : processors) {
          final ProcessingContext context = new ProcessingContext(resource, targetFile, existingContent, processedContent);
          processedContent = processor.process(context);
        }

        if (processedContent == null) {
          throw new IOException(String.format("Processor returned null content for file: %s", resource));
        }
        Files.write(targetFile.toPath(), processedContent.getBytes(StandardCharsets.UTF_8));

        processedFiles.add(targetFile);
      }

      return processedFiles.toArray(new File[0]);
    }
  }

  @Getter
  public static class ProcessingContext {
    private final File sourceFile;
    private final File targetFile;
    private final String existingContent;
    private final String previousOutput;

    public ProcessingContext(File sourceFile, File targetFile, String existingContent, String previousOutput) {
      this.sourceFile = sourceFile;
      this.targetFile = targetFile;
      this.existingContent = existingContent;
      this.previousOutput = previousOutput;
    }
  }

  @FunctionalInterface
  public interface FileContentProcessor {
    String process(ProcessingContext context) throws IOException;
  }
}
