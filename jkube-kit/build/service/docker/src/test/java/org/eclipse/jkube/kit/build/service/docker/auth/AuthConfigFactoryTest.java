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
package org.eclipse.jkube.kit.build.service.docker.auth;

import com.google.gson.JsonObject;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.api.helper.DockerFileUtil;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.common.SystemMock;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AuthConfigFactoryTest {
    @Test
    public void testGetAuthConfigFromSystemProperties() throws IOException {
        // Given
        new SystemMock()
            .put("jkube.docker.username", "testuser")
            .put("jkube.docker.password", "testpass");
        // When
        AuthConfig authConfig = AuthConfigFactory.getAuthConfigFromSystemProperties(AuthConfigFactory.LookupMode.DEFAULT, s -> s);
        // Then
        assertAuthConfig(authConfig, "testuser", "testpass");
    }

    @Test
    public void testGetAuthConfigFromOpenShiftConfig() {
        // Given
        new SystemMock().put("jkube.docker.useOpenShiftAuth", "true");
        Map<String, Object> authConfigMap = new HashMap<>();
        new MockUp<DockerFileUtil>() {
            @Mock
            Map<String, ?> readKubeConfig() {
                Map<String, Object> context = new HashMap<>();
                context.put("name", "test-ctxt");
                context.put("context", Collections.singletonMap("user", "test/api-rh-dev-openshift:443"));

                Map<String, Object> user = new HashMap<>();
                user.put("name", "test/api-rh-dev-openshift:443");
                user.put("user", Collections.singletonMap("token", "sometoken"));

                Map<String, Object> kubeConfig = new HashMap<>();
                kubeConfig.put("current-context", "test-ctxt");
                kubeConfig.put("contexts", Collections.singletonList(context));
                kubeConfig.put("users", Collections.singletonList(user));

                return kubeConfig;
            }
        };
        // When
        AuthConfig authConfig = AuthConfigFactory.getAuthConfigFromOpenShiftConfig(AuthConfigFactory.LookupMode.DEFAULT, authConfigMap);
        // Then
        assertAuthConfig(authConfig, "test", "sometoken");
    }

    @Test
    public void testGetAuthConfigFromOpenShiftConfigWithAuthConfigMap() {
        // Given
        Map<String, Object> authConfigMap = new HashMap<>();
        authConfigMap.put("useOpenShiftAuth", "true");
        new MockUp<DockerFileUtil>() {
            @Mock
            Map<String, ?> readKubeConfig() {
                Map<String, Object> context = new HashMap<>();
                context.put("name", "test-ctxt");
                context.put("context", Collections.singletonMap("user", "test/api-rh-dev-openshift:443"));

                Map<String, Object> user = new HashMap<>();
                user.put("name", "test/api-rh-dev-openshift:443");
                user.put("user", Collections.singletonMap("token", "sometoken"));

                Map<String, Object> kubeConfig = new HashMap<>();
                kubeConfig.put("current-context", "test-ctxt");
                kubeConfig.put("contexts", Collections.singletonList(context));
                kubeConfig.put("users", Collections.singletonList(user));

                return kubeConfig;
            }
        };

        // When
        AuthConfig authConfig = AuthConfigFactory.getAuthConfigFromOpenShiftConfig(AuthConfigFactory.LookupMode.DEFAULT, authConfigMap);

        // Then
        assertAuthConfig(authConfig, "test", "sometoken");
    }

    @Test
    public void testGetAuthConfigFromPluginConfiguration() {
        // Given
        Map<String, Object> authConfigMap = new HashMap<>();
        authConfigMap.put("username", "testuser");
        authConfigMap.put("password", "testpass");
        authConfigMap.put("email", "test@example.com");

        // When
        AuthConfig authConfig = AuthConfigFactory.getAuthConfigFromPluginConfiguration(AuthConfigFactory.LookupMode.DEFAULT, authConfigMap, s -> s);

        // Then
        assertNotNull(authConfig);
        assertAuthConfig(authConfig, "testuser", "testpass");
        assertEquals("test@example.com", authConfig.getEmail());
    }

    @Test
    public void testGetAuthConfigFromSettings() {
        // Given
        List<RegistryServerConfiguration> settings = new ArrayList<>();
        settings.add(RegistryServerConfiguration.builder()
                .id("testregistry.io")
                .username("testuser")
                .password("testpass")
                .build());

        // When
        AuthConfig authConfig = AuthConfigFactory.getAuthConfigFromSettings(settings, "testuser", "testregistry.io", s -> s);

        // Then
        assertAuthConfig(authConfig, "testuser", "testpass");
    }

    @Test
    public void testGetAuthConfigFromDockerConfig(@Mocked KitLogger logger) throws IOException {
        // Given
        new MockUp<DockerFileUtil>() {
            @Mock
            JsonObject readDockerConfig() {
                JsonObject dockerConfig = new JsonObject();
                JsonObject auths = new JsonObject();
                JsonObject creds = new JsonObject();
                creds.addProperty("auth", "dGVzdHVzZXI6dGVzdHBhc3M=");
                auths.add("https://index.docker.io/v1/", creds);
                dockerConfig.add("auths", auths);
                return dockerConfig;
            }
        };

        // When
        AuthConfig authConfig = AuthConfigFactory.getAuthConfigFromDockerConfig("https://index.docker.io/v1/", logger);

        // Then
        assertAuthConfig(authConfig, "testuser", "testpass");
    }

    @Test
    public void testGetStandardAuthConfigFromProperties(@Mocked KitLogger logger) throws IOException {
        // Given
        new SystemMock()
            .put("jkube.docker.username", "testuser")
            .put("jkube.docker.password", "testpass");
        // When
        AuthConfigFactory authConfigFactory = new AuthConfigFactory(logger);
        AuthConfig authConfig = authConfigFactory.createAuthConfig(true, true, Collections.emptyMap(), Collections.emptyList(), "testuser", "testregistry.io", s -> s);
        // Then
        assertAuthConfig(authConfig, "testuser", "testpass");
    }

    @Test
    public void testGetStandardAuthConfigFromMavenSettings(@Mocked KitLogger logger) throws IOException {
        // Given
        List<RegistryServerConfiguration> settings = new ArrayList<>();
        settings.add(RegistryServerConfiguration.builder()
                .id("testregistry.io")
                .username("testuser")
                .password("testpass")
                .build());

        // When
        AuthConfigFactory authConfigFactory = new AuthConfigFactory(logger);
        AuthConfig authConfig = authConfigFactory.createAuthConfig(true, true, Collections.emptyMap(), settings, "testuser", "testregistry.io", s -> s);

        // Then
        assertAuthConfig(authConfig, "testuser", "testpass");
    }

    private void assertAuthConfig(AuthConfig authConfig, String username, String password) {
        assertNotNull(authConfig);
        assertEquals(username, authConfig.getUsername());
        assertEquals(password, authConfig.getPassword());
    }
}
