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
package io.jkube.kit.common.util;


import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard properties from Spring Boot <code>application.properties</code> files
 */
public class SpringBootConfigurationHelper {

    private static final Logger LOG = LoggerFactory.getLogger(SpringBootConfigurationHelper.class);

    public static final String SPRING_BOOT_GROUP_ID = "org.springframework.boot";
    public static final String SPRING_BOOT_ARTIFACT_ID = "spring-boot";
    public static final String SPRING_BOOT_DEVTOOLS_ARTIFACT_ID = "spring-boot-devtools";
    public static final String SPRING_BOOT_MAVEN_PLUGIN_ARTIFACT_ID = "spring-boot-maven-plugin";
    public static final String DEV_TOOLS_REMOTE_SECRET = "spring.devtools.remote.secret";
    public static final String DEV_TOOLS_REMOTE_SECRET_ENV = "SPRING_DEVTOOLS_REMOTE_SECRET";

    /*
        Following are property keys for spring-boot-1 and their spring-boot-2 equivalent
     */
    private static final String[] MANAGEMENT_PORT = {"management.port", "management.server.port"};
    private static final String[] SERVER_PORT = {"server.port", "server.port"};
    private static final String[] SERVER_KEYSTORE = {"server.ssl.key-store", "server.ssl.key-store"};
    private static final String[] MANAGEMENT_KEYSTORE = {"management.ssl.key-store", "management.server.ssl.key-store"};
    private static final String[] SERVLET_PATH = {"server.servlet-path", "server.servlet.path"};
    private static final String[] SERVER_CONTEXT_PATH = {"server.context-path", "server.servlet.context-path"};
    private static final String[] MANAGEMENT_CONTEXT_PATH = {"management.context-path", "management.server.servlet.context-path"};
    private static final String[] ACTUATOR_BASE_PATH = {null, "management.endpoints.web.base-path"};
    private static final String[] ACTUATOR_DEFAULT_BASE_PATH = {"", "/actuator"};

    private int propertyOffset;

    private static final int DEFAULT_SERVER_PORT = 8080;

    public SpringBootConfigurationHelper(Optional<String> springBootVersion) {
        this.propertyOffset = propertyOffset(springBootVersion);
    }

    public String getManagementPortPropertyKey() {
        return lookup(MANAGEMENT_PORT);
    }

    public Integer getManagementPort(Properties properties) {
        String value = properties.getProperty(getManagementPortPropertyKey());
        return value != null ? Integer.parseInt(value) : null;
    }

    public String getServerPortPropertyKey() {
        return lookup(SERVER_PORT);
    }

    public Integer getServerPort(Properties properties) {
        String value = properties.getProperty(getServerPortPropertyKey());
        return value != null ? Integer.parseInt(value) : DEFAULT_SERVER_PORT;
    }


    public String getServerKeystorePropertyKey() {
        return lookup(SERVER_KEYSTORE);
    }

    public String getManagementKeystorePropertyKey() {
        return lookup(MANAGEMENT_KEYSTORE);
    }

    public String getServletPathPropertyKey() {
        return lookup(SERVLET_PATH);
    }

    public String getServerContextPathPropertyKey() {
        return lookup(SERVER_CONTEXT_PATH);
    }

    public String getManagementContextPathPropertyKey() {
        return lookup(MANAGEMENT_CONTEXT_PATH);
    }

    public String getActuatorBasePathPropertyKey() {
        return lookup(ACTUATOR_BASE_PATH);
    }

    public String getActuatorDefaultBasePath() {
        return lookup(ACTUATOR_DEFAULT_BASE_PATH);
    }

    private String lookup(String[] keys) {
        return keys[propertyOffset];
    }

    private int propertyOffset(Optional<String> springBootVersion) {
        Optional<Integer> majorVersion = majorVersion(springBootVersion);
        int idx = majorVersion.map(v -> v - 1).orElse(0);
        idx = Math.min(idx, 1);
        idx = Math.max(idx, 0);
        return idx;
    }

    private Optional<Integer> majorVersion(Optional<String> version) {
        if (version.isPresent()) {
            try {
                return Optional.of(Integer.parseInt(version.get().substring(0, version.get().indexOf('.'))));
            } catch (Exception e) {
                LOG.warn("Cannot spring boot major version from {}", version);
            }
        }
        return Optional.empty();
    }

}
