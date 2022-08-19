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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import static org.eclipse.jkube.kit.common.util.ClassUtil.getResourcesInContextClassloader;

public class BuildPluginsHelper {
  private BuildPluginsHelper() { }

  public static final String JKUBE_PLUGIN_DESCRIPTOR = "META-INF/maven/org.eclipse.jkube/jkube-plugin";
  public static final String JKUBE_EXTRA_DIR = "docker-extra";

  /**
   * Check for dependency containing build plugins descriptor
   *
   * @param project java project
   * @param log logger
   */
  public static void executeBuildPlugins(JavaProject project, KitLogger log) {
    try {
      Enumeration<URL> dmpPlugins = getResourcesInContextClassloader(JKUBE_PLUGIN_DESCRIPTOR);
      while (dmpPlugins.hasMoreElements()) {
        URL dmpPlugin = dmpPlugins.nextElement();
        File outputDir = getAndEnsureOutputDirectory(project);
        processDmpPluginDescription(dmpPlugin, outputDir, log);
      }
    } catch (IOException e) {
      log.error("Cannot load jkube-plugins from %s", JKUBE_PLUGIN_DESCRIPTOR);
    }
  }

  private static File getAndEnsureOutputDirectory(JavaProject project) {
    File outputDir = new File(project.getBuildDirectory(), JKUBE_EXTRA_DIR);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    return outputDir;
  }

  private static void processDmpPluginDescription(URL pluginDesc, File outputDir, KitLogger log) throws IOException {
    String line = null;
    try (LineNumberReader reader =
             new LineNumberReader(new InputStreamReader(pluginDesc.openStream(), StandardCharsets.UTF_8))) {
      line = reader.readLine();
      while (line != null) {
        if (line.matches("^\\s*#")) {
          // Skip comments
          continue;
        }
        callBuildPlugin(outputDir, line, log);
        line = reader.readLine();
      }
    } catch (ClassNotFoundException e) {
      // Not declared as dependency, so just ignoring ...
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      log.verbose("Found jkube-plugin %s but could not be called : %s",
          line,
          e.getMessage());
    }
  }

  private static void callBuildPlugin(File outputDir, String buildPluginClass, KitLogger log) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Class buildPlugin = Class.forName(buildPluginClass);
    try {
      Method method = buildPlugin.getMethod("addExtraFiles", File.class);
      method.invoke(null, outputDir);
      log.info("Extra files from %s extracted", buildPluginClass);
    } catch (NoSuchMethodException exp) {
      log.verbose("Build plugin %s does not support 'addExtraFiles' method", buildPluginClass);
    }
  }
}
