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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;

import static org.eclipse.jkube.kit.common.util.EnvUtil.isWindows;
import static org.eclipse.jkube.kit.common.util.PropertiesUtil.JKUBE_INTERNAL_APP_CONFIG_FILE_LOCATION;
import static org.eclipse.jkube.kit.common.util.PropertiesUtil.getPropertiesFromResource;

/**
 * Utility methods to access spring-boot resources.
 */
public class SpringBootUtil {

  public static final String SPRING_BOOT_GROUP_ID = "org.springframework.boot";
  public static final String SPRING_BOOT_DEVTOOLS_ARTIFACT_ID = "spring-boot-devtools";
  public static final String SPRING_BOOT_MAVEN_PLUGIN_ARTIFACT_ID = "spring-boot-maven-plugin";
  public static final String SPRING_BOOT_GRADLE_PLUGIN_ARTIFACT_ID = "org.springframework.boot.gradle.plugin";
  public static final String DEV_TOOLS_REMOTE_SECRET = "spring.devtools.remote.secret";
  public static final String DEV_TOOLS_REMOTE_SECRET_ENV = "SPRING_DEVTOOLS_REMOTE_SECRET";

  private static final String SPRING_WEB_FLUX_ARTIFACT_ID = "spring-boot-starter-webflux";
  private static final String PLACEHOLDER_PREFIX = "${";
  private static final String PLACEHOLDER_SUFFIX = "}";
  private static final String VALUE_SEPARATOR = ":";

  private SpringBootUtil() {}

  /**
   * Returns the spring boot configuration (supports `application.properties` and `application.yml`)
   * or an empty properties object if not found, it assumes first profile as default profile.
   *
   * @param compileClassLoader compile class loader
   * @return properties object
   */
  public static Properties getSpringBootApplicationProperties(URLClassLoader compileClassLoader) {
    return getSpringBootApplicationProperties(null, compileClassLoader);
  }

  /**
   * Returns the spring boot configuration (supports `application.properties` and `application.yml`)
   * or an empty properties object if not found
   *
   * @param springActiveProfile currently active spring-boot profile
   * @param compileClassLoader compile class loader
   * @return properties object
   */
  public static Properties getSpringBootApplicationProperties(String springActiveProfile, URLClassLoader compileClassLoader) {
    URL ymlResource = compileClassLoader.findResource("application.yml");
    URL propertiesResource = compileClassLoader.findResource("application.properties");

    Properties props = YamlUtil.getPropertiesFromYamlResource(springActiveProfile, ymlResource);
    props.putAll(getPropertiesFromResource(propertiesResource));
    if (ymlResource != null) {
      props.put(JKUBE_INTERNAL_APP_CONFIG_FILE_LOCATION, ymlResource.toString());
    } else if (propertiesResource != null) {
      props.put(JKUBE_INTERNAL_APP_CONFIG_FILE_LOCATION, propertiesResource.toString());
    }
    return new SpringBootPropertyPlaceholderHelper(PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, true)
      .replaceAllPlaceholders(props);
  }

  /**
   * Determine the spring-boot major version for the current project
   *
   * @param javaProject  project
   * @return spring boot version or null
   */
  public static Optional<String> getSpringBootVersion(JavaProject javaProject) {
    return Optional.ofNullable(JKubeProjectUtil.getAnyDependencyVersionWithGroupId(javaProject, SPRING_BOOT_GROUP_ID));
  }

  /**
   * Returns the currently active spring-boot profile or null if not found.
   * @param project the JavaProject for which to search the active profile.
   * @return the currently active spring-boot profile or null if not found.
   */
  public static String getSpringBootActiveProfile(JavaProject project) {
    if (project != null && project.getProperties() != null
      && project.getProperties().get("spring.profiles.active") != null) {
      return project.getProperties().get("spring.profiles.active").toString();
    }
    return null;
  }

  /**
   * Returns a Map containing the Spring Boot configuration for the applicable plugin (Maven or Gradle).
   * @param javaProject the JavaProject for which to search the Spring Boot plugin configuration.
   * @return a Map containing the Spring Boot configuration or an empty Map if no plugin is found.
   */
  public static Map<String, Object> getSpringBootPluginConfiguration(JavaProject javaProject) {
    Plugin mavenPlugin = JKubeProjectUtil.getPlugin(javaProject, SPRING_BOOT_MAVEN_PLUGIN_ARTIFACT_ID);
    if (mavenPlugin != null) {
      return mavenPlugin.getConfiguration();
    }
    Plugin gradlePlugin = JKubeProjectUtil.getPlugin(javaProject, SPRING_BOOT_GRADLE_PLUGIN_ARTIFACT_ID);
    if (gradlePlugin != null) {
      return gradlePlugin.getConfiguration();
    }
    return Collections.emptyMap();
  }

  public static boolean isSpringBootRepackage(JavaProject project) {
    Plugin plugin = JKubeProjectUtil.getPlugin(project, SPRING_BOOT_MAVEN_PLUGIN_ARTIFACT_ID);
    return Optional.ofNullable(plugin)
      .map(Plugin::getExecutions)
      .map(e -> e.contains("repackage"))
      .orElse(false);
  }

  public static Plugin getNativePlugin(JavaProject project) {
    Plugin plugin = JKubeProjectUtil.getPlugin(project, "org.graalvm.buildtools", "native-maven-plugin");
    if (plugin != null) {
      return plugin;
    }
    return JKubeProjectUtil.getPlugin(project, "org.graalvm.buildtools.native", "org.graalvm.buildtools.native.gradle.plugin");
  }

  /**
   * Returns the native executable artifact produced file by the Spring Boot build or null if not found.
   * @param project the JavaProject for which to search the native executable artifact
   * @return the native executable artifact produced file by the Spring Boot build or null if not found
   * @throws IllegalStateException if more than one native executable artifact is found
   */
  public static File findNativeArtifactFile(JavaProject project) {
    File[] nativeExecutableArtifacts = null;
    for (String location : new String[] {"", "native/nativeCompile/"}) {
      nativeExecutableArtifacts = new File(project.getBuildDirectory(), location)
        .listFiles(f -> SpringBootUtil.isExecutableNativeBinary(f , project));
      if (nativeExecutableArtifacts != null && nativeExecutableArtifacts.length > 0) {
        break;
      }
    }
    if (nativeExecutableArtifacts == null) {
      return null;
    }
    if (nativeExecutableArtifacts.length == 1) {
      return nativeExecutableArtifacts[0];
    }
    // TODO: find better way to distinguish between native executables
    final List<File> filteredBinaries = Arrays.stream(nativeExecutableArtifacts)
      .filter(f -> !f.getName().endsWith(".so"))
      .collect(Collectors.toList());
    if (filteredBinaries.size() == 1) {
      return filteredBinaries.iterator().next();
    }
    throw new IllegalStateException("More than one native executable file found in " + project.getBuildDirectory().getAbsolutePath());
  }

  /**
   * Checks if a file is an executable native binary.
   * <p>
   * A file is considered an executable native binary if:
   * <ul>
   *   <li>It is a regular file and executable</li>
   *   <li>Its name matches the project name (assumed to be the main artifact), OR</li>
   *   <li>On Windows: it's a valid PE (Portable Executable) file</li>
   *   <li>On Unix-like systems: it's executable (additional validation may be needed for archives)</li>
   * </ul>
   *
   * @param file the file to check
   * @param project the project context for name matching
   * @return true if the file is an executable native binary, false otherwise
   */
  private static boolean isExecutableNativeBinary(File file, JavaProject project) {
    // Basic file validation
    if (!file.isFile() || !file.canExecute()) {
      return false;
    }

    // If the filename matches the project name, consider it the native artifact
    if (file.getName().equals(project.getArtifactId())) {
      return true;
    }

    // Platform-specific validation
    return isValidNativeBinaryForPlatform(file);
  }

  /**
   * Validates if a file is a native binary for the current platform.
   *
   * @param file the file to validate
   * @return true if the file is a valid native binary for the platform
   */
  private static boolean isValidNativeBinaryForPlatform(File file) {
    try {
      if (isWindows()) {
        // On Windows, verify it's a valid PE file
        return PEHeaderUtil.isPEFile(file);
      }
      // On Unix-like systems, executable files are considered native binaries
      // Note: This may include shell scripts; additional validation could check for ELF headers
      return true;
    } catch (Exception e) {
      // If validation fails due to I/O or parsing errors, assume not a valid binary
      return false;
    }
  }

  public static boolean hasSpringWebFluxDependency(JavaProject javaProject) {
    return JKubeProjectUtil.hasDependency(javaProject, SPRING_BOOT_GROUP_ID, SPRING_WEB_FLUX_ARTIFACT_ID);
  }
}

