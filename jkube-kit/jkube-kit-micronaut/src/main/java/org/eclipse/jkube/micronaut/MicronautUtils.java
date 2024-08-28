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
package org.eclipse.jkube.micronaut;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

import java.util.Properties;

import static org.eclipse.jkube.kit.common.util.PropertiesUtil.fromApplicationConfig;

public class MicronautUtils {
  private static final String[] MICRONAUT_APP_CONFIG_FILES_LIST = new String[] {"application.properties", "application.yml", "application.yaml", "application.json"};
  private MicronautUtils() {}

  public static String extractPort(Properties properties, String defaultValue) {
    return properties.getProperty("micronaut.server.port", defaultValue);
  }

  public static boolean isHealthEnabled(Properties properties) {
    return properties.getProperty("endpoints.health.enabled", "false").equalsIgnoreCase("true");
  }

  public static Properties getMicronautConfiguration(JavaProject javaProject) {
    return fromApplicationConfig(javaProject, MICRONAUT_APP_CONFIG_FILES_LIST);
  }

  public static boolean hasMicronautPlugin(JavaProject javaProject) {
    return JKubeProjectUtil.hasPlugin(javaProject, "io.micronaut.build", "micronaut-maven-plugin") ||
        JKubeProjectUtil.hasPlugin(javaProject, "io.micronaut.maven", "micronaut-maven-plugin") ||
        JKubeProjectUtil.hasPlugin(javaProject, "io.micronaut.application", "io.micronaut.application.gradle.plugin");
  }

  public static boolean hasNativeImagePackaging(JavaProject javaProject) {
    if (javaProject != null) {
      if (javaProject.getProperties() != null &&
        javaProject.getProperties().getProperty("packaging") != null &&
        javaProject.getProperties().getProperty("packaging").equals("native-image")) {
          return true;
      }
      return javaProject.getGradlePlugins() != null && javaProject.getGradlePlugins().contains("org.graalvm.buildtools.gradle.NativeImagePlugin");
    }
    return false;
  }
}
