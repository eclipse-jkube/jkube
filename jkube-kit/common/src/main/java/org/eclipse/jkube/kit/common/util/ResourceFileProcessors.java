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

/**
 * Pre-built {@link ResourceFileProcessing.FileContentProcessor} implementations
 * for common file processing operations.
 */
public class ResourceFileProcessors {

  private ResourceFileProcessors() {
  }

  /**
   * Processor that deep-merges structured content when a same-named target already has accumulated content.
   *
   * <p> Supports both YAML ({@code .yaml}, {@code .yml}) and JSON ({@code .json}) fragments.
   * If the target file has existing content and is a supported format, the previous processor's output
   * is merged into the existing content using {@link YamlUtil#mergeYaml(String, String)}.
   * For unsupported file types or when no existing content is present, the previous output is returned unchanged.
   *
   * @return a processor that merges same-name YAML/JSON fragments
   */
  public static ResourceFileProcessing.FileContentProcessor mergeYamlIfExists() {
    return ctx -> {
      if (ctx.getExistingContent() != null && isStructuredResource(ctx.getTargetFile())) {
        return YamlUtil.mergeYaml(ctx.getExistingContent(), ctx.getPreviousOutput());
      }
      return ctx.getPreviousOutput();
    };
  }

  private static boolean isStructuredResource(java.io.File file) {
    return YamlUtil.isYaml(file) || file.getName().toLowerCase().endsWith(".json");
  }
}
