/**
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
package org.eclipse.jkube.kit.common;

import org.eclipse.jkube.kit.api.JKubeBuildPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ServiceLoader;

public class BuildPluginsHelper {
  private BuildPluginsHelper() { }

  public static final String JKUBE_EXTRA_DIR = "docker-extra";

  /**
   * Check for dependency containing build plugins descriptor
   *
   * @param project java project
   * @param log logger
   */
  public static void executeBuildPlugins(JavaProject project, KitLogger log) {
    File outputDir = getAndEnsureOutputDirectory(project);
    processDmpPluginDescription(outputDir, log);
  }

  private static File getAndEnsureOutputDirectory(JavaProject project) {
    File outputDir = new File(project.getBuildDirectory(), JKUBE_EXTRA_DIR);
    if (!outputDir.exists() && !outputDir.mkdirs()) {
      throw new IllegalStateException("Unable to create directory " + outputDir.getAbsolutePath());
    }
    return outputDir;
  }

  private static void processDmpPluginDescription(File outputDir, KitLogger log) {
    ServiceLoader<JKubeBuildPlugin> jKubeBuildPluginProvider = ServiceLoader.load(JKubeBuildPlugin.class,
        Thread.currentThread().getContextClassLoader());
    for (JKubeBuildPlugin jKubeBuildPlugin : jKubeBuildPluginProvider) {
      callBuildPlugin(jKubeBuildPlugin, outputDir, log);
    }
  }

  private static void callBuildPlugin(JKubeBuildPlugin jKubeBuildPlugin, File outputDir, KitLogger log) {
    try {
      jKubeBuildPlugin.addExtraFiles(outputDir);
      log.info("Extra files from %s extracted", jKubeBuildPlugin.getName());
    } catch (IOException exception) {
      log.verbose("Failure while extracting files from %s, %s", jKubeBuildPlugin.getName(), exception);
    }
  }
}
