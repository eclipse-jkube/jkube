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

import java.io.File;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import static org.eclipse.jkube.kit.common.util.JKubeProjectUtil.getClassLoader;
import static org.eclipse.jkube.kit.common.util.PropertiesUtil.getPropertiesFromResource;
import static org.eclipse.jkube.kit.common.util.PropertiesUtil.toMap;
import static org.eclipse.jkube.kit.common.util.YamlUtil.getPropertiesFromYamlResource;

public class QuarkusUtils {

  private static final String QUARKUS_HTTP_PORT = "quarkus.http.port";
  private static final String QUARKUS_PACKAGE_RUNNER_SUFFIX = "quarkus.package.runner-suffix";

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

  public static String runnerSuffix(Properties properties) {
    return properties.getProperty(QUARKUS_PACKAGE_RUNNER_SUFFIX, "-runner");
  }

  public static String findSingleFileThatEndsWith(JavaProject project, String suffix) {
    final File buildDir = project.getBuildDirectory();
    String[] file = buildDir.list((dir, name) -> name.endsWith(suffix));
    if (file == null || file.length != 1) {
      throw new IllegalStateException("Can't find single file with suffix '" + suffix +
          "' in " + buildDir + " (zero or more than one files found ending with '" +
          suffix + "')");
    }
    return file[0];
  }

  /**
   * Returns the applicable Quarkus configuration properties.
   *
   * <p> It takes into account properties provided either via application.properties, or application.yaml files.
   *
   * <p> Any property overridden in the project's properties (e.g. Maven properties) will take precedence over the ones
   * defined in the static configuration files.
   *
   * @param project from which to read the configuration
   * @return the applicable Quarkus configuration properties
   */
  @SuppressWarnings("unchecked")
  public static Properties getQuarkusConfiguration(JavaProject project) {
    final URLClassLoader urlClassLoader = getClassLoader(project);
    final Supplier<Properties>[] sources = new Supplier[]{
        () -> getPropertiesFromResource(urlClassLoader.findResource("application.properties")),
        () -> getPropertiesFromYamlResource(urlClassLoader.findResource("application.yaml")),
        () -> getPropertiesFromYamlResource(urlClassLoader.findResource("application.yml"))
    };
    for (Supplier<Properties> source : sources) {
      final Properties props = source.get();
      if (!props.isEmpty()) {
        props.putAll(toMap(project.getProperties()));
        return props;
      }
    }
    return project.getProperties();
  }

  private static Optional<String> getActiveProfile(JavaProject project) {
    return Optional.ofNullable(project)
        .map(JavaProject::getProperties)
        .map(properties -> properties.get("quarkus.profile"))
        .map(Object::toString);
  }
}
