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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;

import static org.eclipse.jkube.kit.common.util.PropertiesUtil.getPropertiesFromResource;

/**
 * Utility methods to access spring-boot resources.
 */
public class SpringBootUtil {

    public static final String SPRING_BOOT_GROUP_ID = "org.springframework.boot";
//    public static final String SPRING_BOOT_ARTIFACT_ID = "spring-boot";
    public static final String SPRING_BOOT_DEVTOOLS_ARTIFACT_ID = "spring-boot-devtools";
    public static final String SPRING_BOOT_MAVEN_PLUGIN_ARTIFACT_ID = "spring-boot-maven-plugin";
    public static final String SPRING_BOOT_GRADLE_PLUGIN_ARTIFACT_ID = "org.springframework.boot.gradle.plugin";
    public static final String DEV_TOOLS_REMOTE_SECRET = "spring.devtools.remote.secret";
    public static final String DEV_TOOLS_REMOTE_SECRET_ENV = "SPRING_DEVTOOLS_REMOTE_SECRET";

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

        Properties props = getPropertiesFromApplicationYamlResource(springActiveProfile, ymlResource);
        props.putAll(getPropertiesFromResource(propertiesResource));
        return props;
    }

    public static Properties getPropertiesFromApplicationYamlResource(String springActiveProfile, URL ymlResource) {
        return YamlUtil.getPropertiesFromYamlResource(springActiveProfile, ymlResource);
    }

    /**
     * Determine the spring-boot devtools version for the current project
     *
     * @param mavenProject Maven project
     * @return devtools version or null
     */
    public static Optional<String> getSpringBootDevToolsVersion(JavaProject mavenProject) {
        return getSpringBootVersion(mavenProject);
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

    public static String getSpringBootActiveProfile(JavaProject project) {
        if (project != null && project.getProperties() != null
              && project.getProperties().get("spring.profiles.active") != null) {
            return project.getProperties().get("spring.profiles.active").toString();
        }
        return null;
    }

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

    public static boolean isSpringBootBuildImageSupported(JavaProject project) {
        Optional<String> springBootVersionOptional = getSpringBootVersion(project);
        if (springBootVersionOptional.isPresent()) {
            String springBootVersion = springBootVersionOptional.get();
            return isSpringBootVersionAtLeast(springBootVersion, 2, 3);
        }
        return false;
    }

    public static boolean isSpringBootVersionAtLeast(String springBootVersion, int expectedMajorVersion, int expectedMinorVersion) {
        String[] springBootVersionParts = springBootVersion.split("\\.");
        if (springBootVersionParts.length >= 3) {
            int majorVersion = Integer.parseInt(springBootVersionParts[0]);
            int minorVersion = Integer.parseInt(springBootVersionParts[1]);

          return majorVersion > expectedMajorVersion || minorVersion >= expectedMinorVersion;
        }
        return false;
    }
}

