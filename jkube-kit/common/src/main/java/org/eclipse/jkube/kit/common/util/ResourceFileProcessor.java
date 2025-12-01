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
   * Process resource files by applying a content processor and merging duplicate files.
   *
   * @param resourceFiles    the array of resource files to process
   * @param outDir           the output directory for processed files
   * @param contentProcessor function to process file content (e.g., filtering, interpolation)
   * @return array of processed files
   * @throws IOException if file processing fails
   */
  public static File[] processFiles(File[] resourceFiles, File outDir, FileContentProcessor contentProcessor) throws IOException {
    if (resourceFiles == null) {
      return new File[0];
    }

    // Use LinkedHashSet to maintain order and track unique target files
    Set<File> processedFiles = new LinkedHashSet<>();

    for (File resource : resourceFiles) {
      File targetFile = new File(outDir, resource.getName());

      // Save existing content if file already exists (means we're merging)
      String existingContent = null;
      if (targetFile.exists()) {
        existingContent = new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);
      }

      // Process the resource file content
      String processedContent = contentProcessor.process(resource, targetFile);

      // If there was existing content, merge it with the new content
      if (existingContent != null && YamlUtil.isYaml(targetFile)) {
        processedContent = mergeContent(existingContent, processedContent);
      }

      // Write the final content to the target file
      Files.write(targetFile.toPath(), processedContent.getBytes(StandardCharsets.UTF_8));

      // Add to set - duplicates will be automatically ignored
      processedFiles.add(targetFile);
    }
    return processedFiles.toArray(new File[0]);
  }

  /**
   * Merge existing and new content for YAML files.
   *
   * @param existingContent the existing file content
   * @param newContent      the new content to merge
   * @return merged content
   * @throws IOException if YAML merging fails
   */
  private static String mergeContent(String existingContent, String newContent) throws IOException {
    // For YAML files, use YamlUtil for deep property-level merge
    return YamlUtil.mergeYaml(existingContent, newContent);
  }

  /**
   * Functional interface for processing file content.
   */
  @FunctionalInterface
  public interface FileContentProcessor {
    /**
     * Process the content of a resource file.
     *
     * @param sourceFile the source file to process
     * @param targetFile the target file where content will be written
     * @return processed content as string
     * @throws IOException if processing fails
     */
    String process(File sourceFile, File targetFile) throws IOException;
  }
}