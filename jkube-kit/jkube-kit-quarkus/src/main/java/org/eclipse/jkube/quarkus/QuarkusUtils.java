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
package org.eclipse.jkube.quarkus;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.JavaProject;

import java.net.URLClassLoader;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import static org.eclipse.jkube.kit.common.util.PropertiesUtil.getPropertiesFromResource;
import static org.eclipse.jkube.kit.common.util.YamlUtil.getPropertiesFromYamlResource;

public class QuarkusUtils {

  private static final String QUARKUS_HTTP_PORT = "quarkus.http.port";

  private QuarkusUtils() {}

  public static String extractPort(JavaProject javaProject, Properties properties, String defaultValue) {
    final Optional<String> activeProfile = getActiveProfile(javaProject);
    if (activeProfile.isPresent()) {
      final String profilePort = properties.getProperty(String.format("%%%s.%s", activeProfile.get(), QUARKUS_HTTP_PORT));
      if (StringUtils.isNotBlank(profilePort)) {
        return profilePort;
      }
    }
    return properties.getProperty(QUARKUS_HTTP_PORT, defaultValue);
  }

  @SuppressWarnings("unchecked")
  public static Properties getQuarkusConfiguration(URLClassLoader urlClassLoader) {
    final Supplier<Properties>[] sources = new Supplier[]{
        () -> getPropertiesFromResource(urlClassLoader.findResource("application.properties")),
        () -> getPropertiesFromYamlResource(urlClassLoader.findResource("application.yml"))
    };
    for (Supplier<Properties> source : sources) {
      final Properties props = source.get();
      if (!props.isEmpty()) {
        return props;
      }
    }
    return new Properties();
  }

  private static Optional<String> getActiveProfile(JavaProject project) {
    return Optional.ofNullable(project)
        .map(JavaProject::getProperties)
        .map(properties -> properties.get("quarkus.profile"))
        .map(Object::toString);
  }
}
