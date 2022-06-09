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

import java.io.File;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

import org.apache.commons.lang3.StringUtils;

import static org.eclipse.jkube.kit.common.util.FileUtil.stripPrefix;
import static org.eclipse.jkube.kit.common.util.JKubeProjectUtil.getClassLoader;
import static org.eclipse.jkube.kit.common.util.PropertiesUtil.getPropertiesFromResource;
import static org.eclipse.jkube.kit.common.util.PropertiesUtil.toMap;
import static org.eclipse.jkube.kit.common.util.SemanticVersionUtil.isVersionAtLeast;
import static org.eclipse.jkube.kit.common.util.YamlUtil.getPropertiesFromYamlResource;

public class QuarkusUtils {

  public static final String QUARKUS_GROUP_ID = "io.quarkus";
  private static final String RED_HAT_QUARKUS_BUILD_GROUP_ID = "com.redhat.quarkus.platform";
  public static final String QUARKUS_PLATFORM_GROUP_ID = "io.quarkus.platform";
  private static final String QUARKUS_HTTP_PORT = "quarkus.http.port";
  private static final String QUARKUS_PACKAGE_RUNNER_SUFFIX = "quarkus.package.runner-suffix";
  private static final String QUARKUS_HTTP_ROOT_PATH = "quarkus.http.root-path";
  private static final String QUARKUS_HTTP_NON_APPLICATION_ROOT_PATH = "quarkus.http.non-application-root-path";
  private static final String QUARKUS_SMALLRYE_HEALTH_ROOT_PATH = "quarkus.smallrye-health.root-path";
  private static final String QUARKUS_SMALLRYE_HEALTH_READINESS_PATH = "quarkus.smallrye-health.readiness-path";
  private static final String QUARKUS_SMALLRYE_HEALTH_LIVENESS_PATH = "quarkus.smallrye-health.liveness-path";
  private static final String QUARKUS_MAVEN_PLUGIN_ARTIFACTID = "quarkus-maven-plugin";
  private static final int QUARKUS_MAJOR_VERSION_SINCE_PATH_RESOLUTION_CHANGE = 1;
  private static final int QUARKUS_MINOR_VERSION_SINCE_PATH_RESOLUTION_CHANGE = 11;
  private static final int QUARKUS2_MAJOR_VERSION = 2;
  private static final int QUARKUS2_MINOR_VERSION = 0;
  private static final String DEFAULT_ROOT_PATH = "/";
  private static final String DEFAULT_NON_APPLICATION_ROOT_BEFORE_2_0 = "";
  private static final String DEFAULT_NON_APPLICATION_ROOT_AFTER_2_0 = "q";
  private static final String DEFAULT_HEALTH_ROOT_PATH = "health";
  private static final String DEFAULT_READINESS_SUBPATH = "ready";
  private static final String DEFAULT_LIVENESS_SUBPATH = "live";

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

  /**
   * Get Quarkus SmallRye Health readiness path from Quarkus configuration
   *
   * @param javaProject {@link JavaProject} project for which health readiness path is required
   * @return a string containing the readiness path
   */
  public static String resolveQuarkusReadinessPath(JavaProject javaProject) {
    return getQuarkusConfiguration(javaProject)
        .getProperty(QUARKUS_SMALLRYE_HEALTH_READINESS_PATH, DEFAULT_READINESS_SUBPATH);
  }

  /**
   * Get Quarkus SmallRye Health liveness path from Quarkus configuration
   *
   * @param javaProject {@link JavaProject} project for which health liveness path is required
   * @return a string containing the liveness path
   */
  public static String resolveQuarkusLivenessPath(JavaProject javaProject) {
    return getQuarkusConfiguration(javaProject)
        .getProperty(QUARKUS_SMALLRYE_HEALTH_LIVENESS_PATH, DEFAULT_LIVENESS_SUBPATH);
  }

  public static boolean hasQuarkusPlugin(JavaProject javaProject) {
    return JKubeProjectUtil.hasPlugin(javaProject, QUARKUS_GROUP_ID, QUARKUS_MAVEN_PLUGIN_ARTIFACTID) ||
           JKubeProjectUtil.hasPlugin(javaProject, QUARKUS_PLATFORM_GROUP_ID, QUARKUS_MAVEN_PLUGIN_ARTIFACTID) ||
           JKubeProjectUtil.hasPlugin(javaProject, RED_HAT_QUARKUS_BUILD_GROUP_ID, QUARKUS_MAVEN_PLUGIN_ARTIFACTID) ||
           JKubeProjectUtil.hasPlugin(javaProject, QUARKUS_GROUP_ID, "io.quarkus.gradle.plugin");
  }

  /**
   * Since Quarkus 1.11.5.Final Quarkus considers leading slashes in configured application
   * properties health paths as absolute urls.
   * This method checks if Quarkus version is at least required version and returns a boolean value
   * whether leading slash should be considered or not.
   *
   * @param quarkusVersion Quarkus version for the current Project
   * @param healthPath health path
   * @return boolean value whether to use absolute health path or not.
   * @see <a href="https://quarkus.io/blog/path-resolution-in-quarkus/">https://quarkus.io/blog/path-resolution-in-quarkus/</a>
   */
  static boolean shouldUseAbsoluteHealthPaths(String quarkusVersion, String healthPath) {
    return isVersionAtLeast(QUARKUS_MAJOR_VERSION_SINCE_PATH_RESOLUTION_CHANGE,
        QUARKUS_MINOR_VERSION_SINCE_PATH_RESOLUTION_CHANGE, quarkusVersion)
        && isAbsolutePath(healthPath);
  }

  /**
   * Get Quarkus SmallRye Health root path by checking project properties
   *
   * @param javaProject {@link JavaProject} project for which health path is required
   * @param subPath the applicable readiness/liveness subpath to append
   * @return a string containing fully qualified health root path
   */
  public static String resolveCompleteQuarkusHealthRootPath(JavaProject javaProject, String subPath) {
    final String quarkusVersion = findQuarkusVersion(javaProject);
    final Properties quarkusProperties = getQuarkusConfiguration(javaProject);
    final String healthRootPath = quarkusProperties.getProperty(QUARKUS_SMALLRYE_HEALTH_ROOT_PATH, DEFAULT_HEALTH_ROOT_PATH);
    final String nonApplicationRootPath = resolveQuarkusNonApplicationRootPath(quarkusVersion, quarkusProperties);
    final String rootPath = quarkusProperties.getProperty(QUARKUS_HTTP_ROOT_PATH, DEFAULT_ROOT_PATH);
    String ret = "";
    for (String component : new String[]{subPath, healthRootPath, nonApplicationRootPath, rootPath}) {
      ret = concatPath(component, ret);
      if (shouldUseAbsoluteHealthPaths(quarkusVersion, component)) {
        return ret;
      }
    }
    return ret;
  }

  private static String resolveQuarkusNonApplicationRootPath(String quarkusVersion, Properties quarkusProperties) {
    final String defaultValue = isVersionAtLeast(QUARKUS2_MAJOR_VERSION, QUARKUS2_MINOR_VERSION, quarkusVersion) ?
        DEFAULT_NON_APPLICATION_ROOT_AFTER_2_0 : DEFAULT_NON_APPLICATION_ROOT_BEFORE_2_0;
    return quarkusProperties.getProperty(QUARKUS_HTTP_NON_APPLICATION_ROOT_PATH, defaultValue);
  }

  public static String concatPath(String... paths) {
    return "/" + Stream.of(paths)
        .filter(StringUtils::isNotBlank)
        .filter(path -> !"/".equals(path))
        .map(path -> stripPrefix(path, "/"))
        .collect(Collectors.joining("/"));
  }

  static String findQuarkusVersion(JavaProject javaProject) {
    return Optional.ofNullable(JKubeProjectUtil.getAnyDependencyVersionWithGroupId(javaProject, QUARKUS_GROUP_ID))
        .orElse(null);
  }

  private static boolean isAbsolutePath(String path) {
    return path != null && path.startsWith("/");
  }
}
