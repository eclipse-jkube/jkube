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
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

import java.io.File;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import static org.eclipse.jkube.kit.common.util.FileUtil.stripPrefix;
import static org.eclipse.jkube.kit.common.util.JKubeProjectUtil.getClassLoader;
import static org.eclipse.jkube.kit.common.util.PropertiesUtil.getPropertiesFromResource;
import static org.eclipse.jkube.kit.common.util.PropertiesUtil.toMap;
import static org.eclipse.jkube.kit.common.util.YamlUtil.getPropertiesFromYamlResource;

public class QuarkusUtils {

  private static final String QUARKUS_HTTP_PORT = "quarkus.http.port";
  private static final String QUARKUS_PACKAGE_RUNNER_SUFFIX = "quarkus.package.runner-suffix";
  private static final String QUARKUS_UNIVERSE_BOM_GROUP_ID = "io.quarkus";
  private static final String QUARKUS_HTTP_ROOT_PATH = "quarkus.http.root-path";
  private static final String QUARKUS_HTTP_NON_APPLICATION_ROOT_PATH = "quarkus.http.non-application-root-path";
  private static final String QUARKUS_SMALLRYE_HEALTH_ROOT_PATH = "quarkus.smallrye-health.root-path";
  private static final String QUARKUS_SMALLRYE_HEALTH_LIVENESS_PATH = "quarkus.smallrye-health.liveness-path";
  private static final int QUARKUS_MAJOR_VERSION_SINCE_PATH_RESOLUTION_CHANGE = 1;
  private static final int QUARKUS_MINOR_VERSION_SINCE_PATH_RESOLUTION_CHANGE = 11;
  private static final int QUARKUS2_MAJOR_VERSION = 2;
  private static final int QUARKUS2_MINOR_VERSION = 0;
  private static final String QUARKUS_DEFAULT_LIVENESS_SUBPATH = "live";
  private static final String QUARKUS_DEFAULT_ROOT_PATH = "/";
  private static final String QUARKUS_DEFAULT_HEALTH_PATH_BEFORE_2_0 = "health";
  private static final String QUARKUS_DEFAULT_HEALTH_PATH_AFTER_2_0 = "q/health";

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

  /**
   * Get Quarkus version from checking quarkus-universe-bom artifactId in project dependencies
   *
   * @param javaProject {@link JavaProject} project for which version is queried
   * @return an Optional String either containing quarkus version or empty optional
   */
  public static Optional<String> getQuarkusVersion(JavaProject javaProject) {
    return Optional.ofNullable(JKubeProjectUtil.getAnyDependencyVersionWithGroupId(javaProject, QUARKUS_UNIVERSE_BOM_GROUP_ID));
  }

  /**
   * Get Quarkus SmallRye Health root path by checking project properties
   *
   * @param javaProject {@link JavaProject} project for which health path is required
   * @return a string containing fully qualified health root path
   */
  public static String resolveCompleteQuarkusHealthRootPath(JavaProject javaProject) {
    String healthPath = resolveQuarkusHealthRootPath(javaProject);
    Properties properties = getQuarkusConfiguration(javaProject);
    String quarkusVersion = getQuarkusVersion(javaProject).orElse(null);
    if (shouldUseAbsoluteHealthPaths(quarkusVersion, healthPath)) {
      return healthPath;
    }
    return resolveCompleteQuarkusHealthRootPath(quarkusVersion, properties, healthPath);
  }

  /**
   * Get Quarkus SmallRye Health liveliness path by checking project properties
   *
   * @param javaProject {@link JavaProject} project for which health liveliness path is required
   * @return a string containing liveliness path(it may or may not be absolute path dependending on user configuration)
   */
  public static String resolveQuarkusLivelinessRootPath(JavaProject javaProject) {
    Properties properties = getQuarkusConfiguration(javaProject);
    Object livenessPath = properties.get(QUARKUS_SMALLRYE_HEALTH_LIVENESS_PATH);
    return livenessPath != null ? livenessPath.toString() : QUARKUS_DEFAULT_LIVENESS_SUBPATH;
  }

  /**
   * Check whether Quarkus version is at least provided version
   *
   * @param javaProject {@link JavaProject} Project for which version is being checked
   * @param majorVersion minimum major version
   * @param minorVersion minimum minor version
   * @return a boolean value which satisfies given criteria
   */
  public static boolean isQuarkusVersionAtLeast(JavaProject javaProject, int majorVersion, int minorVersion) {
    return getQuarkusVersion(javaProject)
            .filter(s -> isQuarkusVersionAtLeast(majorVersion, minorVersion, s))
            .isPresent();
  }

  /**
   * Create final Health Check paths
   *
   * @param healthPath health path (usually configured via properties, defaults to health)
   * @param subPath sub path for liveness/readiness endpoints
   * @return string with full health check path
   */
  public static String createHealthCheckPath(String healthPath, String subPath) {
    if (healthPath.equals("/")) {
      return String.format("/%s", stripPrefix(subPath, "/"));
    }
    return String.format("/%s/%s", stripPrefix(healthPath, "/"), stripPrefix(subPath, "/"));
  }

  /**
   * Since Quarkus 1.11.5.Final Quarkus considers leading slashes in health urls
   * configured via properties. This method checks if Quarkus version is at least
   * required version and returns a boolean value whether leading slash should
   * be considered or not
   *
   * @param quarkusVersion Project quarkus version
   * @param healthPath health path
   * @return boolean value whether to use absolute health path or not.
   */
  public static boolean shouldUseAbsoluteHealthPaths(String quarkusVersion, String healthPath) {
    if (isQuarkusVersionAtLeast(QUARKUS_MAJOR_VERSION_SINCE_PATH_RESOLUTION_CHANGE, QUARKUS_MINOR_VERSION_SINCE_PATH_RESOLUTION_CHANGE, quarkusVersion)) {
      return isAbsolutePath(healthPath);
    }
    return false;
  }

  private static Optional<String> getActiveProfile(JavaProject project) {
    return Optional.ofNullable(project)
        .map(JavaProject::getProperties)
        .map(properties -> properties.get("quarkus.profile"))
        .map(Object::toString);
  }

  private static String resolveQuarkusHealthRootPath(JavaProject javaProject) {
    Properties properties = getQuarkusConfiguration(javaProject);
    String quarkusVersion = getQuarkusVersion(javaProject).orElse(null);
    Object healthPathObj = properties.get(QUARKUS_SMALLRYE_HEALTH_ROOT_PATH);
    if (healthPathObj != null) {
      return healthPathObj.toString();
    }
    return isQuarkusVersionAtLeast(QUARKUS2_MAJOR_VERSION, QUARKUS2_MINOR_VERSION, quarkusVersion) ?
            QUARKUS_DEFAULT_HEALTH_PATH_AFTER_2_0 :
            QUARKUS_DEFAULT_HEALTH_PATH_BEFORE_2_0;
  }

  private static String resolveCompleteQuarkusHealthRootPath(String quarkusVersion, Properties properties, String subPath) {
    Object nonApplicationRootPath = properties.get(QUARKUS_HTTP_NON_APPLICATION_ROOT_PATH);
    if (nonApplicationRootPath != null) {
      return createCompleteHealthPathWithNonApplicationRootPath(quarkusVersion, properties, subPath, nonApplicationRootPath);
    }
    return createCompleteHealthPath(null, properties, subPath);
  }

  private static String createCompleteHealthPathWithNonApplicationRootPath(String quarkusVersion, Properties properties, String subPath, Object nonApplicationRootPath) {
    if (shouldUseAbsoluteHealthPaths(quarkusVersion, nonApplicationRootPath.toString())) {
      return String.format("/%s/%s", stripPrefix(nonApplicationRootPath.toString(), "/"), subPath);
    }
    return createCompleteHealthPath(nonApplicationRootPath.toString(), properties, subPath);
  }

  private static String createCompleteHealthPath(String nonApplicationRootPath, Properties properties, String subPath) {
    Object applicationRootPathObj = properties.get(QUARKUS_HTTP_ROOT_PATH);
    String applicationRootPath = applicationRootPathObj != null ? applicationRootPathObj.toString() : QUARKUS_DEFAULT_ROOT_PATH;

    if (StringUtils.isNotBlank(nonApplicationRootPath)) {
      return createHealthPathWithNonApplicationRootPath(applicationRootPath, nonApplicationRootPath, subPath);
    }
    return createHealthCheckPath(applicationRootPath, subPath);
  }

  private static String createHealthPathWithNonApplicationRootPath(String applicationRootPath, String nonApplicationRootPath, String subPath) {
    if (applicationRootPath.equals("/")) {
      return String.format("%s%s/%s", applicationRootPath, nonApplicationRootPath, subPath);
    }
    return String.format("/%s/%s/%s", stripPrefix(applicationRootPath, "/"), nonApplicationRootPath, subPath);
  }

  private static boolean isQuarkusVersionAtLeast(int majorVersion, int minorVersion, String quarkusVersion) {
    if (StringUtils.isNotBlank(quarkusVersion)) {
      String[] quarkusVersionParts = quarkusVersion.split("\\.");
      int projectMajorVersion = Integer.parseInt(quarkusVersionParts[0]);
      int projectMinorVersion = Integer.parseInt(quarkusVersionParts[1]);

      if (projectMajorVersion > majorVersion) {
        return true;
      } else if (projectMajorVersion == majorVersion) {
        return projectMinorVersion >= minorVersion;
      }
    }
    return false;
  }

  private static boolean isAbsolutePath(String path) {
    return path != null && path.startsWith("/");
  }
}
