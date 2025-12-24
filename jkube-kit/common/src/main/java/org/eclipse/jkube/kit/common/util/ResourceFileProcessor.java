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
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Utility class for processing resource files with merging support for YAML files.
 */
public class ResourceFileProcessor {

  private ResourceFileProcessor() {
    // Utility class
  }

  /**
   * Process resource files by applying content processors and handling merging of duplicate files.
   * When multiple processors are provided, they are chained together in order.
   * Each processor receives both the original existing content and the output from the previous processor.
   *
   * @param resourceFiles     the array of resource files to process
   * @param outDir            the output directory for processed files
   * @param contentProcessors functions to process file content (e.g., filtering, interpolation, merging)
   * @return array of processed files
   * @throws IOException if file processing fails
   */
  public static File[] processFiles(File[] resourceFiles, File outDir, FileContentProcessor... contentProcessors) throws IOException {
    if (resourceFiles == null) {
      return new File[0];
    }

    if (contentProcessors == null || contentProcessors.length == 0) {
      throw new IllegalArgumentException("At least one content processor is required");
    }

    // Use LinkedHashSet to maintain order and track unique target files
    Set<File> processedFiles = new LinkedHashSet<>();

    for (File resource : resourceFiles) {
      File targetFile = new File(outDir, resource.getName());

      // Read existing content if file already exists
      String existingContent = null;
      if (targetFile.exists()) {
        existingContent = new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);
      }

      // Chain processors: each processor receives both the original existingContent
      // and the output from the previous processor
      String processedContent = existingContent;
      for (FileContentProcessor processor : contentProcessors) {
        processedContent = processor.process(resource, targetFile, existingContent, processedContent);
      }

      // Write the final content to the target file
      if (processedContent == null) {
        throw new IOException(String.format("Processor returned null content for file: %s", resource));
      }
      Files.write(targetFile.toPath(), processedContent.getBytes(StandardCharsets.UTF_8));

      // Add to set - duplicates will be automatically ignored
      processedFiles.add(targetFile);
    }
    return processedFiles.toArray(new File[0]);
  }

  /**
   * Functional interface for processing file content.
   * Processors are responsible for reading, transforming, and optionally merging file content.
   */
  @FunctionalInterface
  public interface FileContentProcessor {
    /**
     * Process the content of a resource file.
     *
     * @param sourceFile        the source file to process
     * @param targetFile        the target file where content will be written
     * @param existingContent   the original content from targetFile if it already exists, null otherwise.
     *                          This value remains constant across all processors in a chain.
     * @param previousOutput    the output from the previous processor in the chain.
     *                          For the first processor, this equals existingContent.
     * @return processed content as string
     * @throws IOException if processing fails
     */
    String process(File sourceFile, File targetFile, String existingContent, String previousOutput) throws IOException;
  }
}