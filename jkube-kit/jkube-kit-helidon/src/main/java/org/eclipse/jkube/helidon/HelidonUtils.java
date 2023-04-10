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
package org.eclipse.jkube.helidon;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import static org.eclipse.jkube.kit.common.util.JKubeProjectUtil.getClassLoader;
import static org.eclipse.jkube.kit.common.util.PropertiesUtil.getPropertiesFromResource;
import static org.eclipse.jkube.kit.common.util.PropertiesUtil.toMap;
import static org.eclipse.jkube.kit.common.util.YamlUtil.getPropertiesFromYamlResource;

public class HelidonUtils {
  private static final String HELIDON_HTTP_PORT = "server.port";

  private HelidonUtils() { }

  public static boolean hasHelidonDependencies(JavaProject javaProject) {
    return JKubeProjectUtil.hasTransitiveDependency(javaProject, "io.helidon.webserver", "helidon-webserver");
  }

  public static boolean hasHelidonGraalNativeImageExtension(JavaProject javaProject) {
    return JKubeProjectUtil.hasDependency(javaProject, "io.helidon.integrations.graal", "helidon-graal-native-image-extension") ||
        JKubeProjectUtil.hasDependency(javaProject, "io.helidon.integrations.graal", "helidon-mp-graal-native-image-extension");
  }

  public static boolean hasHelidonHealthDependency(JavaProject javaProject) {
    return JKubeProjectUtil.hasTransitiveDependency(javaProject, "io.helidon.health", "helidon-health");
  }

  public static Properties getHelidonConfiguration(JavaProject javaProject) {
    final URLClassLoader urlClassLoader = getClassLoader(javaProject);
    final List<Supplier<Properties>> sources = Arrays.asList(
        () -> getPropertiesFromResource(urlClassLoader.findResource("META-INF/microprofile-config.properties")),
        () -> getPropertiesFromYamlResource(urlClassLoader.findResource("application.yaml")),
        () -> getPropertiesFromYamlResource(urlClassLoader.findResource("application.yml"))
    );
    for (Supplier<Properties> source : sources) {
      final Properties props = source.get();
      if (!props.isEmpty()) {
        props.putAll(toMap(javaProject.getProperties()));
        return props;
      }
    }
    return javaProject.getProperties();
  }

  public static String extractPort(Properties properties, String defaultValue) {
    return properties.getProperty(HELIDON_HTTP_PORT, defaultValue);
  }
}
