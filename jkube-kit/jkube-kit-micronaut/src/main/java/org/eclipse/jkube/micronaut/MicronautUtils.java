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

import java.net.URLClassLoader;
import java.util.Properties;
import java.util.function.Supplier;

import static org.eclipse.jkube.kit.common.util.PropertiesUtil.getPropertiesFromResource;
import static org.eclipse.jkube.kit.common.util.YamlUtil.getPropertiesFromYamlResource;

public class MicronautUtils {

  private MicronautUtils() {}

  public static String extractPort(Properties properties, String defaultValue) {
    return properties.getProperty("micronaut.server.port", defaultValue);
  }

  public static boolean isHealthEnabled(Properties properties) {
    return properties.getProperty("endpoints.health.enabled", "false").equalsIgnoreCase("true");
  }

  @SuppressWarnings("unchecked")
  public static Properties getMicronautConfiguration(URLClassLoader urlClassLoader) {
    final Supplier<Properties>[] sources = new Supplier[]{
        () -> getPropertiesFromResource(urlClassLoader.findResource("application.properties")),
        () -> getPropertiesFromYamlResource(urlClassLoader.findResource("application.yml")),
        () -> getPropertiesFromYamlResource(urlClassLoader.findResource("application.json"))
    };
    for (Supplier<Properties> source : sources) {
      final Properties props = source.get();
      if (!props.isEmpty()) {
        return props;
      }
    }
    return new Properties();
  }

  public static boolean hasMicronautPlugin(JavaProject javaProject) {
    return JKubeProjectUtil.hasPlugin(javaProject, "io.micronaut.build", "micronaut-maven-plugin") ||
        JKubeProjectUtil.hasPlugin(javaProject, "io.micronaut.application", "io.micronaut.application.gradle.plugin");
  }
}
