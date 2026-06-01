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

public class ResourceFileProcessors {

  private ResourceFileProcessors() {
  }

  public static ResourceFileProcessing.FileContentProcessor mergeYamlIfExists() {
    return ctx -> {
      if (ctx.getExistingContent() != null && YamlUtil.isYaml(ctx.getTargetFile())) {
        return YamlUtil.mergeYaml(ctx.getExistingContent(), ctx.getPreviousOutput());
      }
      return ctx.getPreviousOutput();
    };
  }
}
