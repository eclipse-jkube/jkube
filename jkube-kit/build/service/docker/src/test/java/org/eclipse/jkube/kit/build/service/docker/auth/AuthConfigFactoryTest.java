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
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unused")
class AuthConfigFactoryTest {
    static final String ECR_NAME = "123456789012.dkr.ecr.bla.amazonaws.com";
    private AuthConfigFactory factory;
    private GsonBuilder gsonBuilder;

    @Mocked
    private KitLogger log;

    @Mocked
    private AwsSdkHelper awsSdkHelper;

    private HttpServer httpServer;

    @BeforeEach
    void containerSetup() {
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
    void testGetAuthConfigFromSystemProperties() throws IOException {
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
    void testGetAuthConfigFromOpenShiftConfig() {
        // Given
        System.setProperty("jkube.docker.useOpenShiftAuth", "true");
        Map<String, Object> authConfigMap = new HashMap<>();
        new MockUp<KubernetesConfigAuthUtil>() {
            @Mock
            AuthConfig readKubeConfigAuth() {
                return AuthConfig.builder()
                    .username("test")
                    .password("sometoken")
                    .build();
            }
        };
        try {
            // When
            AuthConfig authConfig = AuthConfigFactory.getAuthConfigFromOpenShiftConfig(AuthConfigFactory.LookupMode.DEFAULT, authConfigMap);
            // Then
            assertAuthConfig(authConfig, "test", "sometoken");
        } finally {
            System.clearProperty("jkube.docker.useOpenShiftAuth");
        }
    }

    @Test
    void testGetAuthConfigFromOpenShiftConfigWithAuthConfigMap() {
        // Given
        Map<String, Object> authConfigMap = new HashMap<>();
        authConfigMap.put("useOpenShiftAuth", "true");
        new MockUp<KubernetesConfigAuthUtil>() {
            @Mock
            AuthConfig readKubeConfigAuth() {
                return AuthConfig.builder()
                    .username("test")
                    .password("sometoken")
                    .build();
            }
        };

        // When
        AuthConfig authConfig = AuthConfigFactory.getAuthConfigFromOpenShiftConfig(AuthConfigFactory.LookupMode.DEFAULT, authConfigMap);

        // Then
        assertAuthConfig(authConfig, "test", "sometoken");
    }

    @Test
    void testGetAuthConfigFromPluginConfiguration() {
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
    void testGetAuthConfigFromSettings() {
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
    void testGetAuthConfigFromDockerConfig(@Mocked KitLogger logger) throws IOException {
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
    void testGetStandardAuthConfigFromProperties(@Mocked KitLogger logger) throws IOException {
        // Given
        System.setProperty("jkube.docker.username", "testuser");
        System.setProperty("jkube.docker.password", "testpass");
        try {
            // When
            AuthConfigFactory authConfigFactory = new AuthConfigFactory(logger);
            AuthConfig authConfig = authConfigFactory.createAuthConfig(true, true, Collections.emptyMap(), Collections.emptyList(), "testuser", "testregistry.io", s -> s);
            // Then
            assertAuthConfig(authConfig, "testuser", "testpass");
        } finally {
            System.clearProperty("jkube.docker.username");
            System.clearProperty("jkube.docker.password");
        }
    }

    @Test
    void testGetStandardAuthConfigFromMavenSettings(@Mocked KitLogger logger) throws IOException {
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

    @Test
    void getAuthConfigViaAwsSdk() throws IOException {
        new Expectations() {{
            awsSdkHelper.isDefaultAWSCredentialsProviderChainPresentInClassPath();
            result = true;
        }};
        String accessKeyId = randomUUID().toString();
        String secretAccessKey = randomUUID().toString();
        new MockedAwsSdkAuthConfigFactory(accessKeyId, secretAccessKey);

        AuthConfig authConfig = factory.createAuthConfig(false, true, null, Collections.emptyList(), "user", ECR_NAME, s -> s);

        verifyAuthConfig(authConfig, accessKeyId, secretAccessKey, null, null);
    }

    @Test
    void ecsTaskRole() throws IOException {
        givenAwsSdkIsDisabled();
        String containerCredentialsUri = "/v2/credentials/" + randomUUID();
        String accessKeyId = randomUUID().toString();
        String secretAccessKey = randomUUID().toString();
        String sessionToken = randomUUID().toString();
        givenEcsMetadataService(containerCredentialsUri, accessKeyId, secretAccessKey, sessionToken);
        setupEcsMetadataConfiguration(httpServer, containerCredentialsUri);

        AuthConfig authConfig = factory.createAuthConfig(false, true, null, Collections.emptyList(), "user", ECR_NAME, s -> s);

        verifyAuthConfig(authConfig, accessKeyId, secretAccessKey, null, sessionToken);
    }

    @Test
    void fargateTaskRole() throws IOException {
        givenAwsSdkIsDisabled();
        String containerCredentialsUri = "v2/credentials/" + randomUUID();
        String accessKeyId = randomUUID().toString();
        String secretAccessKey = randomUUID().toString();
        String sessionToken = randomUUID().toString();
        givenEcsMetadataService("/" + containerCredentialsUri, accessKeyId, secretAccessKey, sessionToken);
        setupEcsMetadataConfiguration(httpServer, containerCredentialsUri);

        AuthConfig authConfig = factory.createAuthConfig(false, true, null, Collections.emptyList(), "user", ECR_NAME, s -> s);

        verifyAuthConfig(authConfig, accessKeyId, secretAccessKey, null, sessionToken);
    }

    @Test
    void awsTemporaryCredentialsArePickedUpFromEnvironment() throws IOException {
        givenAwsSdkIsDisabled();
        String accessKeyId = randomUUID().toString();
        String secretAccessKey = randomUUID().toString();
        String sessionToken = randomUUID().toString();
        new Expectations() {{
            awsSdkHelper.getAwsAccessKeyIdEnvVar();
            result = accessKeyId;
            awsSdkHelper.getAwsSecretAccessKeyEnvVar();
            result = secretAccessKey;
            awsSdkHelper.getAwsSessionTokenEnvVar();
            result = sessionToken;
        }};

        AuthConfig authConfig = factory.createAuthConfig(false, true, null, Collections.emptyList(), "user", ECR_NAME, s -> s);

        verifyAuthConfig(authConfig, accessKeyId, secretAccessKey, null, sessionToken);
    }

    @Test
    void awsStaticCredentialsArePickedUpFromEnvironment() throws IOException {
        givenAwsSdkIsDisabled();
        String accessKeyId = randomUUID().toString();
        String secretAccessKey = randomUUID().toString();
        new Expectations() {{
            awsSdkHelper.getAwsAccessKeyIdEnvVar();
            result = accessKeyId;
            awsSdkHelper.getAwsSecretAccessKeyEnvVar();
            result = secretAccessKey;
        }};

        AuthConfig authConfig = factory.createAuthConfig(false, true, null, Collections.emptyList(), "user", ECR_NAME, s -> s);

        verifyAuthConfig(authConfig, accessKeyId, secretAccessKey, null, null);
    }

    @Test
    void incompleteAwsCredentialsAreIgnored() throws IOException {
        givenAwsSdkIsDisabled();
        System.setProperty("AWS_ACCESS_KEY_ID", randomUUID().toString());

        AuthConfig authConfig = factory.createAuthConfig(false, true, null, Collections.emptyList(), "user", ECR_NAME, s -> s);

        assertThat(authConfig).isNull();
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
        new Expectations() {{
            awsSdkHelper.getEcsMetadataEndpoint();
            result = "http://" +
                    httpServer.getInetAddress().getHostAddress()+":" + httpServer.getLocalPort();

            awsSdkHelper.getAwsContainerCredentialsRelativeUri();
            result = containerCredentialsUri;
        }};
    }

    private static void givenAwsSdkIsDisabled() {
        new DisableAwsSdkAuthConfigFactory();
    }

    private void verifyAuthConfig(AuthConfig config, String username, String password, String email, String auth) {
        assertThat(config).isNotNull();
        JsonObject params = gsonBuilder.create().fromJson(new String(Base64.decodeBase64(config.toHeaderValue(log).getBytes())), JsonObject.class);
        assertThat(params)
                .returns(username, p -> p.get("username").getAsString())
                .returns(password, p -> p.get("password").getAsString());
        if (email != null) {
            assertThat(params.get("email").getAsString()).isEqualTo(email);
        }
        if (auth != null) {
            assertThat(params.get("auth").getAsString()).isEqualTo(auth);
        }
    }

    private static class MockedAwsSdkAuthConfigFactory extends MockUp<AwsSdkAuthConfigFactory> {
        private final String accessKeyId;
        private final String secretAccessKey;

        MockedAwsSdkAuthConfigFactory(String accessKeyId, String secretAccessKey) {
            this.accessKeyId = accessKeyId;
            this.secretAccessKey = secretAccessKey;
        }

        @Mock
        void $init(KitLogger log) { }

        @Mock
        AuthConfig createAuthConfig() {
            return AuthConfig.builder()
                    .username(accessKeyId)
                    .password(secretAccessKey)
                    .email(null)
                    .auth(null)
                    .identityToken(null)
                    .build();
        }

    }

    private static class DisableAwsSdkAuthConfigFactory extends MockUp<AwsSdkAuthConfigFactory> {
        @Mock
        void $init(KitLogger log) { }

        @Mock
        AuthConfig createAuthConfig() {
            return null;
        }
    }

    private void assertAuthConfig(AuthConfig authConfig, String username, String password) {
        assertThat(authConfig).isNotNull()
                .hasFieldOrPropertyWithValue("username", username)
                .hasFieldOrPropertyWithValue("password", password);
    }
}
