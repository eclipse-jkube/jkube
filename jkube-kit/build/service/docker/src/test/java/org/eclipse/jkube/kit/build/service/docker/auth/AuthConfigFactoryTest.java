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

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.api.helper.DockerFileUtil;
import org.eclipse.jkube.kit.build.api.helper.KubernetesConfigAuthUtil;
import org.eclipse.jkube.kit.build.service.docker.auth.ecr.AwsSdkAuthConfigFactory;
import org.eclipse.jkube.kit.build.service.docker.auth.ecr.AwsSdkHelper;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;

import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class AuthConfigFactoryTest {
    static final String ECR_NAME = "123456789012.dkr.ecr.bla.amazonaws.com";
    private AuthConfigFactory factory;
    private GsonBuilder gsonBuilder;
    private KitLogger log;
    private AwsSdkHelper awsSdkHelper;
    private HttpServer httpServer;

    @BeforeEach
    void containerSetup() {
        log = new KitLogger.SilentLogger();
        awsSdkHelper = mock(AwsSdkHelper.class);
        factory = new AuthConfigFactory(log, awsSdkHelper);
        gsonBuilder = new GsonBuilder();
    }

    @AfterEach
    void shutdownHttpServer() {
        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
        }
    }

    @Test
    void getAuthConfigFromSystemProperties() throws IOException {
        // Given
        System.setProperty("jkube.docker.username", "testuser");
        System.setProperty("jkube.docker.password", "testpass");
        try {
            // When
            AuthConfig authConfig = AuthConfigFactory.getAuthConfigFromSystemProperties(AuthConfigFactory.LookupMode.DEFAULT, s -> s);
            // Then
            assertAuthConfig(authConfig, "testuser", "testpass");
        } finally {
            System.clearProperty("jkube.docker.username");
            System.clearProperty("jkube.docker.password");
        }
    }

    @Test
    void getAuthConfigFromOpenShiftConfig() {
        // Given
        System.setProperty("jkube.docker.useOpenShiftAuth", "true");
        Map<String, Object> authConfigMap = new HashMap<>();
        try (MockedStatic<KubernetesConfigAuthUtil> mockStatic = Mockito.mockStatic(KubernetesConfigAuthUtil.class)) {
            mockStatic.when(KubernetesConfigAuthUtil::readKubeConfigAuth).thenReturn(AuthConfig.builder()
                    .username("test")
                    .password("sometoken")
                    .build());
            // When
            AuthConfig authConfig = AuthConfigFactory.getAuthConfigFromOpenShiftConfig(AuthConfigFactory.LookupMode.DEFAULT, authConfigMap);
            // Then
            assertAuthConfig(authConfig, "test", "sometoken");
        } finally {
            System.clearProperty("jkube.docker.useOpenShiftAuth");
        }
    }

    @Test
    void getAuthConfigFromOpenShiftConfigWithAuthConfigMap() {
        // Given
        Map<String, Object> authConfigMap = new HashMap<>();
        authConfigMap.put("useOpenShiftAuth", "true");
        try (MockedStatic<KubernetesConfigAuthUtil> mockStatic = Mockito.mockStatic(KubernetesConfigAuthUtil.class)) {
            mockStatic.when(KubernetesConfigAuthUtil::readKubeConfigAuth).thenReturn(AuthConfig.builder()
                .username("test")
                .password("sometoken")
                .build());
            // When
            AuthConfig authConfig = AuthConfigFactory.getAuthConfigFromOpenShiftConfig(AuthConfigFactory.LookupMode.DEFAULT, authConfigMap);

            // Then
            assertAuthConfig(authConfig, "test", "sometoken");
        }
    }

    @Test
    void getAuthConfigFromPluginConfiguration() {
        // Given
        Map<String, Object> authConfigMap = new HashMap<>();
        authConfigMap.put("username", "testuser");
        authConfigMap.put("password", "testpass");
        authConfigMap.put("email", "test@example.com");

        // When
        AuthConfig authConfig = AuthConfigFactory.getAuthConfigFromPluginConfiguration(AuthConfigFactory.LookupMode.DEFAULT, authConfigMap, s -> s);

        // Then
        assertAuthConfig(authConfig, "testuser", "testpass");
        assertThat(authConfig).hasFieldOrPropertyWithValue("email", "test@example.com");
    }

    @Test
    void getAuthConfigFromSettings() {
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
    void getAuthConfigFromDockerConfig() throws IOException {
        // Given
        JsonObject dockerConfig = new JsonObject();
        JsonObject auths = new JsonObject();
        JsonObject creds = new JsonObject();
        creds.addProperty("auth", "dGVzdHVzZXI6dGVzdHBhc3M=");
        auths.add("https://index.docker.io/v1/", creds);
        dockerConfig.add("auths", auths);
        try (MockedStatic<DockerFileUtil> mockStatic = Mockito.mockStatic(DockerFileUtil.class)) {
            mockStatic.when(DockerFileUtil::readDockerConfig).thenReturn(dockerConfig);
            // When
            AuthConfig authConfig = AuthConfigFactory.getAuthConfigFromDockerConfig("https://index.docker.io/v1/", log);

            // Then
            assertAuthConfig(authConfig, "testuser", "testpass");
        }
    }

    @Test
    void getStandardAuthConfigFromProperties() throws IOException {
        // Given
        System.setProperty("jkube.docker.username", "testuser");
        System.setProperty("jkube.docker.password", "testpass");
        try {
            // When
            AuthConfigFactory authConfigFactory = new AuthConfigFactory(log);
            AuthConfig authConfig = authConfigFactory.createAuthConfig(true, true, Collections.emptyMap(), Collections.emptyList(), "testuser", "testregistry.io", s -> s);
            // Then
            assertAuthConfig(authConfig, "testuser", "testpass");
        } finally {
            System.clearProperty("jkube.docker.username");
            System.clearProperty("jkube.docker.password");
        }
    }

    @Test
    void getStandardAuthConfigFromMavenSettings() throws IOException {
        // Given
        List<RegistryServerConfiguration> settings = new ArrayList<>();
        settings.add(RegistryServerConfiguration.builder()
                .id("testregistry.io")
                .username("testuser")
                .password("testpass")
                .build());

        // When
        AuthConfigFactory authConfigFactory = new AuthConfigFactory(log);
        AuthConfig authConfig = authConfigFactory.createAuthConfig(true, true, Collections.emptyMap(), settings, "testuser", "testregistry.io", s -> s);

        // Then
        assertAuthConfig(authConfig, "testuser", "testpass");
    }

    @Test
    void getAuthConfigViaAwsSdk() throws IOException {
        String accessKeyId = randomUUID().toString();
        String secretAccessKey = randomUUID().toString();
        try (MockedConstruction<AwsSdkAuthConfigFactory> ignored = mockConstruction(AwsSdkAuthConfigFactory.class, (mock, ctx) ->
          when(mock.createAuthConfig()).thenReturn(AuthConfig.builder()
            .username(accessKeyId)
            .password(secretAccessKey)
            .build()))
        ) {
            when(awsSdkHelper.isDefaultAWSCredentialsProviderChainPresentInClassPath()).thenReturn(true);

            AuthConfig authConfig = factory.createAuthConfig(false, true, null, Collections.emptyList(), "user", ECR_NAME, s -> s);

            verifyAuthConfig(authConfig, accessKeyId, secretAccessKey, null);
        }
    }

    @Test
    void ecsTaskRole() throws IOException {
        try (MockedConstruction<AwsSdkAuthConfigFactory> ignored = mockConstruction(AwsSdkAuthConfigFactory.class, (mock, ctx) ->
          when(mock.createAuthConfig()).thenReturn(null))
        ) {
            String containerCredentialsUri = "/v2/credentials/" + randomUUID();
            String accessKeyId = randomUUID().toString();
            String secretAccessKey = randomUUID().toString();
            String sessionToken = randomUUID().toString();
            givenEcsMetadataService(containerCredentialsUri, accessKeyId, secretAccessKey, sessionToken);
            setupEcsMetadataConfiguration(httpServer, containerCredentialsUri);

            AuthConfig authConfig = factory.createAuthConfig(false, true, null, Collections.emptyList(), "user", ECR_NAME, s -> s);

            verifyAuthConfig(authConfig, accessKeyId, secretAccessKey, sessionToken);
        }
    }

    @Test
    void fargateTaskRole() throws IOException {
        try (MockedConstruction<AwsSdkAuthConfigFactory> ignored = mockConstruction(AwsSdkAuthConfigFactory.class, (mock, ctx) ->
          when(mock.createAuthConfig()).thenReturn(null))
        ) {
            String containerCredentialsUri = "v2/credentials/" + randomUUID();
            String accessKeyId = randomUUID().toString();
            String secretAccessKey = randomUUID().toString();
            String sessionToken = randomUUID().toString();
            givenEcsMetadataService("/" + containerCredentialsUri, accessKeyId, secretAccessKey, sessionToken);
            setupEcsMetadataConfiguration(httpServer, containerCredentialsUri);

            AuthConfig authConfig = factory.createAuthConfig(false, true, null, Collections.emptyList(), "user", ECR_NAME, s -> s);

            verifyAuthConfig(authConfig, accessKeyId, secretAccessKey, sessionToken);
        }
    }

    @Test
    void awsTemporaryCredentialsArePickedUpFromEnvironment() throws IOException {
        try (MockedConstruction<AwsSdkAuthConfigFactory> ignored = mockConstruction(AwsSdkAuthConfigFactory.class, (mock, ctx) ->
          when(mock.createAuthConfig()).thenReturn(null))
        ) {
            String accessKeyId = randomUUID().toString();
            String secretAccessKey = randomUUID().toString();
            String sessionToken = randomUUID().toString();
            when(awsSdkHelper.getAwsAccessKeyIdEnvVar()).thenReturn(accessKeyId);
            when(awsSdkHelper.getAwsSecretAccessKeyEnvVar()).thenReturn(secretAccessKey);
            when(awsSdkHelper.getAwsSessionTokenEnvVar()).thenReturn(sessionToken);
            AuthConfig authConfig = factory.createAuthConfig(false, true, null, Collections.emptyList(), "user", ECR_NAME, s -> s);

            verifyAuthConfig(authConfig, accessKeyId, secretAccessKey, sessionToken);
        }
    }

    @Test
    void awsStaticCredentialsArePickedUpFromEnvironment() throws IOException {
        try (MockedConstruction<AwsSdkAuthConfigFactory> ignored = mockConstruction(AwsSdkAuthConfigFactory.class, (mock, ctx) ->
          when(mock.createAuthConfig()).thenReturn(null))
        ) {
            String accessKeyId = randomUUID().toString();
            String secretAccessKey = randomUUID().toString();
            when(awsSdkHelper.getAwsAccessKeyIdEnvVar()).thenReturn(accessKeyId);
            when(awsSdkHelper.getAwsSecretAccessKeyEnvVar()).thenReturn(secretAccessKey);

            AuthConfig authConfig = factory.createAuthConfig(false, true, null, Collections.emptyList(), "user", ECR_NAME, s -> s);

            verifyAuthConfig(authConfig, accessKeyId, secretAccessKey, null);
        }
    }

    @Test
    void incompleteAwsCredentialsAreIgnored() throws IOException {
        try (MockedConstruction<AwsSdkAuthConfigFactory> ignored = mockConstruction(AwsSdkAuthConfigFactory.class, (mock, ctx) ->
          when(mock.createAuthConfig()).thenReturn(null));
         MockedStatic<DockerFileUtil> dfu = mockStatic(DockerFileUtil.class)
        ) {
            System.setProperty("AWS_ACCESS_KEY_ID", randomUUID().toString());
            dfu.when(DockerFileUtil::readDockerConfig).thenReturn(null);

            AuthConfig authConfig = factory.createAuthConfig(false, true, null, Collections.emptyList(), "user", ECR_NAME, s -> s);

            assertThat(authConfig).isNull();
        } finally {
            System.clearProperty("AWS_ACCESS_KEY_ID");
        }
    }

    private void givenEcsMetadataService(String containerCredentialsUri, String accessKeyId, String secretAccessKey, String sessionToken) throws IOException {
        httpServer =
                ServerBootstrap.bootstrap()
                        .setLocalAddress(InetAddress.getLoopbackAddress())
                        .registerHandler("*", (request, response, context) -> {
                            System.out.println("REQUEST: " + request.getRequestLine());
                            if (containerCredentialsUri.matches(request.getRequestLine().getUri())) {
                                response.setEntity(new StringEntity(gsonBuilder.create().toJson(ImmutableMap.of(
                                        "AccessKeyId", accessKeyId,
                                        "SecretAccessKey", secretAccessKey,
                                        "Token", sessionToken
                                ))));
                            } else {
                                response.setStatusCode(SC_NOT_FOUND);
                            }
                        })
                        .create();
        httpServer.start();
    }

    private void setupEcsMetadataConfiguration(HttpServer httpServer, String containerCredentialsUri) {
        when(awsSdkHelper.getEcsMetadataEndpoint()).thenReturn("http://" +
                httpServer.getInetAddress().getHostAddress()+":" + httpServer.getLocalPort());
        when(awsSdkHelper.getAwsContainerCredentialsRelativeUri()).thenReturn(containerCredentialsUri);
    }

    private void verifyAuthConfig(AuthConfig config, String username, String password, String auth) {
        assertThat(config).isNotNull();
        JsonObject params = gsonBuilder.create().fromJson(new String(Base64.decodeBase64(config.toHeaderValue(log).getBytes())), JsonObject.class);
        assertThat(params)
                .returns(username, p -> p.get("username").getAsString())
                .returns(password, p -> p.get("password").getAsString());
        if (auth != null) {
            assertThat(params.get("auth").getAsString()).isEqualTo(auth);
        }
    }

    private void assertAuthConfig(AuthConfig authConfig, String username, String password) {
        assertThat(authConfig).isNotNull()
                .hasFieldOrPropertyWithValue("username", username)
                .hasFieldOrPropertyWithValue("password", password);
    }
}

