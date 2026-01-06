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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Common file content processors for use with {@link ResourceFileProcessing}.
 * Provides pre-built processors for common file processing operations.
 */
public class ResourceFileProcessors {

  private ResourceFileProcessors() {
    // Utility class
  }

  /**
   * Processor that reads the source file content.
   *
   * @return a processor that reads file content
   */
  public static ResourceFileProcessing.FileContentProcessor readFile() {
    return ctx -> new String(Files.readAllBytes(ctx.getSourceFile().toPath()), StandardCharsets.UTF_8);
  }

  /**
   * Processor that merges YAML content if the target file exists and is YAML.
   *
   * @return a processor that merges YAML files
   */
  public static ResourceFileProcessing.FileContentProcessor mergeYamlIfExists() {
    return ctx -> {
      if (ctx.getExistingContent() != null && YamlUtil.isYaml(ctx.getTargetFile())) {
        return YamlUtil.mergeYaml(ctx.getExistingContent(), ctx.getPreviousOutput());
      }
      return ctx.getPreviousOutput();
    };
  }

  /**
   * Processor that simply passes through the previous output unchanged.
   *
   * @return a processor that returns the previous output
   */
  public static ResourceFileProcessing.FileContentProcessor passThrough() {
    return ResourceFileProcessing.ProcessingContext::getPreviousOutput;
  }

  /**
   * Processor that transforms content using a custom function.
   *
   * @param transformer the transformation function
   * @return a processor that applies the transformation
   */
  public static ResourceFileProcessing.FileContentProcessor transform(UnaryOperator<String> transformer) {
    return ctx -> transformer.apply(ctx.getPreviousOutput());
  }

  /**
   * Processor that conditionally applies a processor based on a predicate.
   *
   * @param predicate the condition to check
   * @param processor the processor to apply if condition is true
   * @return a processor that conditionally applies the given processor
   */
  public static ResourceFileProcessing.FileContentProcessor conditional(
    Predicate<ResourceFileProcessing.ProcessingContext> predicate,
    ResourceFileProcessing.FileContentProcessor processor) {
    return ctx -> {
      if (predicate.test(ctx)) {
        return processor.process(ctx);
      }
      return ctx.getPreviousOutput();
    };
  }

  /**
   * Processor that filters content based on a predicate.
   * If the predicate returns false, the file is skipped (returns null).
   *
   * @param predicate the filter condition
   * @return a processor that filters based on the predicate
   */
  public static ResourceFileProcessing.FileContentProcessor filter(Predicate<ResourceFileProcessing.ProcessingContext> predicate) {
    return ctx -> predicate.test(ctx) ? ctx.getPreviousOutput() : null;
  }
}
