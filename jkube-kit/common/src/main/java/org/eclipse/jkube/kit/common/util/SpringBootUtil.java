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
package org.eclipse.jkube.kit.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.jkube.kit.common.JKubeProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods to access spring-boot resources.
 */
public class SpringBootUtil {

    private static final transient Logger LOG = LoggerFactory.getLogger(SpringBootUtil.class);

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
        props.putAll(getPropertiesResource(propertiesResource));
        return props;
    }

    public static Properties getPropertiesFromApplicationYamlResource(String springActiveProfile, URL ymlResource) {
        return YamlUtil.getPropertiesFromYamlResource(springActiveProfile, ymlResource);
    }

    /**
     * Returns the given properties resource on the project classpath if found or an empty properties object if not
     *
     * @param resource resource url
     * @return properties
     */
    protected static Properties getPropertiesResource(URL resource) {
        Properties answer = new Properties();
        if (resource != null) {
            try(InputStream stream = resource.openStream()) {
                answer.load(stream);
            } catch (IOException e) {
                throw new IllegalStateException("Error while reading resource from URL " + resource, e);
            }
        }
        return answer;
    }

    /**
     * Determine the spring-boot devtools version for the current project
     *
     * @param mavenProject Maven project
     * @return devtools version or null
     */
    public static Optional<String> getSpringBootDevToolsVersion(JKubeProject mavenProject) {
        return getSpringBootVersion(mavenProject);
    }

    /**
     * Determine the spring-boot major version for the current project
     *
     * @param mavenProject  project
     * @return spring boot version or null
     */
    public static Optional<String> getSpringBootVersion(JKubeProject mavenProject) {
        return Optional.ofNullable(JKubeProjectUtil.getAnyDependencyVersionWithGroupId(mavenProject, SpringBootConfigurationHelper.SPRING_BOOT_GROUP_ID));
    }

    public static String getSpringBootActiveProfile(JKubeProject jkubeProject) {
        if (jkubeProject != null && jkubeProject.getProperties() != null) {
            if (jkubeProject.getProperties().get("spring.profiles.active") != null) {
                return jkubeProject.getProperties().get("spring.profiles.active").toString();
            }
        }
        return null;
    }
}

