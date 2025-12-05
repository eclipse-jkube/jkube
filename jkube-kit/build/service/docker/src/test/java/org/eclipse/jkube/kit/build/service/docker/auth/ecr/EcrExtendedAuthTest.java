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
package org.eclipse.jkube.kit.build.service.docker.auth.ecr;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.common.KitLogger;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test exchange of local stored credentials for temporary ecr credentials
 *
 * @author chas
 * @since 2016-12-21
 */
class EcrExtendedAuthTest {
    private KitLogger logger;

    @BeforeEach
    void setUp(){
        logger = new KitLogger.SilentLogger();
    }
    @Test
    void testIsNotAws() {
        assertThat(new EcrExtendedAuth(logger, "jolokia").isAwsRegistry()).isFalse();
    }

    @Test
    void testIsAws() {
        assertThat(new EcrExtendedAuth(logger, "123456789012.dkr.ecr.eu-west-1.amazonaws.com").isAwsRegistry()).isTrue();
    }

    @Test
    void testHeaders() {
        EcrExtendedAuth eea = new EcrExtendedAuth(logger, "123456789012.dkr.ecr.eu-west-1.amazonaws.com");
        AuthConfig localCredentials = AuthConfig.builder()
                .username("username")
                .password("password")
                .build();
        Date signingTime = Date.from(
            ZonedDateTime.of(2016, 12, 17, 21, 10, 58, 0, ZoneId.of("GMT"))
                .toInstant());
        HttpPost request = eea.createSignedRequest(localCredentials, signingTime);
        assertThat(request)
                .returns("api.ecr.eu-west-1.amazonaws.com", r -> r.getFirstHeader("host").getValue())
                .returns("20161217T211058Z", r -> r.getFirstHeader("X-Amz-Date").getValue())
                .returns("AWS4-HMAC-SHA256 Credential=username/20161217/eu-west-1/ecr/aws4_request, SignedHeaders=content-type;host;x-amz-target, Signature=2ae11d499499cc951900aac0fbec96009382ba4f735bd14baa375c3e51d50aa9", r -> r.getFirstHeader("Authorization").getValue());
    }

    @Test
    void testClientClosedAndCredentialsDecoded()
        throws IOException, IllegalStateException {
        final CloseableHttpClient closeableHttpClient = mock(CloseableHttpClient.class);
        final CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        final StatusLine statusLine = mock(StatusLine.class);
        final HttpEntity entity = new StringEntity("{\"authorizationData\": [{"
                                                   + "\"authorizationToken\": \"QVdTOnBhc3N3b3Jk\","
                                                   + "\"expiresAt\": 1448878779.809,"
                                                   + "\"proxyEndpoint\": \"https://012345678910.dkr.ecr.eu-west-1.amazonaws.com\"}]}");
        when(closeableHttpClient.execute(any())).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(closeableHttpResponse.getEntity()).thenReturn(entity);
        EcrExtendedAuth eea = new EcrExtendedAuth(logger, "123456789012.dkr.ecr.eu-west-1.amazonaws.com") {
            @Override
            CloseableHttpClient createClient() {
                return closeableHttpClient;
            }
        };

        AuthConfig localCredentials = AuthConfig.builder()
                .username("username")
                .password("password")
                .build();
        AuthConfig awsCredentials = eea.extendedAuth(localCredentials);
        assertThat(awsCredentials)
                .hasFieldOrPropertyWithValue("username", "AWS")
                .hasFieldOrPropertyWithValue("password", "password");
        verify(closeableHttpClient).close();
    }

    @Test
    void testIsLocalStack() {
        assertThat(new EcrExtendedAuth(logger, "000000000000.dkr.ecr.us-east-1.localhost.localstack.cloud:4566").isAwsRegistry()).isTrue();
    }

    @Test
    void testLocalStackRegistryPattern() {
        EcrExtendedAuth eea = new EcrExtendedAuth(logger, "000000000000.dkr.ecr.us-east-1.localhost.localstack.cloud:4566");
        assertThat(eea.isAwsRegistry()).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpointTestData")
    void testHeadersWithCustomEndpoint(String testName, String endpoint, String expectedHost, String expectedUri) {
        EcrExtendedAuth eea = new TestableEcrExtendedAuth(logger, "123456789012.dkr.ecr.eu-west-1.amazonaws.com", endpoint);
        AuthConfig localCredentials = AuthConfig.builder()
                .username("username")
                .password("password")
                .build();
        Date signingTime = Date.from(
            ZonedDateTime.of(2016, 12, 17, 21, 10, 58, 0, ZoneId.of("GMT"))
                .toInstant());
        HttpPost request = eea.createSignedRequest(localCredentials, signingTime);

        // Host header should NOT include port for signing
        assertThat(request.getFirstHeader("host").getValue())
                .isEqualTo(expectedHost);

        // Request URI
        assertThat(request.getURI()).hasToString(expectedUri);

        // Verify request entity is created with account ID
        assertThat(request.getEntity()).isNotNull();
    }

    static Stream<Arguments> endpointTestData() {
        return Stream.of(
            Arguments.of(
                "HTTP endpoint with port",
                "http://localhost.localstack.cloud:4566",
                "localhost.localstack.cloud",
                "http://localhost.localstack.cloud:4566/"
            ),
            Arguments.of(
                "HTTPS endpoint with port",
                "https://localhost.localstack.cloud:4566",
                "localhost.localstack.cloud",
                "https://localhost.localstack.cloud:4566/"
            ),
            Arguments.of(
                "HTTP endpoint without port",
                "http://localhost.localstack.cloud",
                "localhost.localstack.cloud",
                "http://localhost.localstack.cloud/"
            ),
            Arguments.of(
                "No custom endpoint (AWS ECR)",
                null,
                "api.ecr.eu-west-1.amazonaws.com",
                "https://api.ecr.eu-west-1.amazonaws.com/"
            ),
            Arguments.of(
                "Empty custom endpoint (AWS ECR)",
                "",
                "api.ecr.eu-west-1.amazonaws.com",
                "https://api.ecr.eu-west-1.amazonaws.com/"
            ),
            Arguments.of(
                "Endpoint with trailing slash",
                "http://localhost.localstack.cloud:4566/",
                "localhost.localstack.cloud",
                "http://localhost.localstack.cloud:4566/"
            ),
            Arguments.of(
                "Endpoint with non-standard port",
                "https://custom.ecr.local:8443",
                "custom.ecr.local",
                "https://custom.ecr.local:8443/"
            ),
            Arguments.of(
                "Endpoint without protocol-like port (hostname:text)",
                "http://localhost.localstack.cloud:notaport",
                "localhost.localstack.cloud:notaport",
                "http://localhost.localstack.cloud:notaport/"
            )
        );
    }

    @Test
    void testGetEndpointUrl() {
        EcrExtendedAuth eea = new EcrExtendedAuth(logger, "123456789012.dkr.ecr.eu-west-1.amazonaws.com");
        // Should return null or actual env var value depending on system
        String endpoint = eea.getEndpointUrl();
        // Just verify it doesn't throw
        assertThat(endpoint).satisfiesAnyOf(
            e -> assertThat(e).isNull(),
            e -> assertThat(e).isNotEmpty()
        );
    }

    @Test
    void testLocalStackAccountIdAndRegionExtraction() {
        EcrExtendedAuth eea = new TestableEcrExtendedAuth(logger, "000000000000.dkr.ecr.us-east-1.localhost.localstack.cloud:4566", "http://localhost.localstack.cloud:4566");
        assertThat(eea.isAwsRegistry()).isTrue();
        // Verify it can create a signed request with LocalStack registry pattern
        HttpPost request = eea.createSignedRequest(
            AuthConfig.builder().username("test").password("test").build(),
            new Date()
        );
        assertThat(request).isNotNull();
        assertThat(request.getFirstHeader("host").getValue()).isEqualTo("localhost.localstack.cloud");
    }

    @ParameterizedTest(name = "isAwsRegistry({0}) should return {1}")
    @MethodSource("registryValidationTestData")
    void testStaticIsAwsRegistryMethod(String registry, boolean expected) {
        assertThat(EcrExtendedAuth.isAwsRegistry(registry)).isEqualTo(expected);
    }

    static Stream<Arguments> registryValidationTestData() {
        return Stream.of(
            Arguments.of("123456789012.dkr.ecr.eu-west-1.amazonaws.com", true),
            Arguments.of("000000000000.dkr.ecr.us-east-1.localhost.localstack.cloud:4566", true),
            Arguments.of("docker.io", false),
            Arguments.of(null, false),
            Arguments.of("", false),
            Arguments.of("invalid.registry.com", false),
            Arguments.of("123.dkr.ecr.us-east-1.amazonaws.com", false) // Invalid account ID (too short)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("securityTokenTestData")
    void testAuthConfigWithSecurityToken(String testDescription, String securityToken, boolean shouldHaveHeader) {
        EcrExtendedAuth eea = new TestableEcrExtendedAuth(logger, "123456789012.dkr.ecr.eu-west-1.amazonaws.com", null);
        AuthConfig.AuthConfigBuilder builder = AuthConfig.builder()
                .username("username")
                .password("password");

        if (securityToken != null) {
            builder.auth(securityToken);
        }

        AuthConfig localCredentials = builder.build();
        Date signingTime = Date.from(
            ZonedDateTime.of(2016, 12, 17, 21, 10, 58, 0, ZoneId.of("GMT"))
                .toInstant());
        HttpPost request = eea.createSignedRequest(localCredentials, signingTime);

        // Verify security token header presence/absence
        if (shouldHaveHeader) {
            assertThat(request.getFirstHeader("X-Amz-Security-Token")).isNotNull();
            assertThat(request.getFirstHeader("X-Amz-Security-Token").getValue())
                    .isEqualTo(securityToken);
        } else {
            assertThat(request.getFirstHeader("X-Amz-Security-Token")).isNull();
        }
    }

    static Stream<Arguments> securityTokenTestData() {
        return Stream.of(
            Arguments.of("with session token", "session-token-value", true),
            Arguments.of("without session token", null, false),
            Arguments.of("with empty session token", "", false)
        );
    }

    @ParameterizedTest(name = "Region: {0}")
    @MethodSource("regionTestData")
    void testDifferentRegions(String region, String expectedHost) {
        String registry = "123456789012.dkr.ecr." + region + ".amazonaws.com";
        EcrExtendedAuth eea = new TestableEcrExtendedAuth(logger, registry, null);
        AuthConfig credentials = AuthConfig.builder().username("user").password("pass").build();
        Date time = new Date();
        HttpPost request = eea.createSignedRequest(credentials, time);
        assertThat(request.getFirstHeader("host").getValue()).isEqualTo(expectedHost);
    }

    static Stream<Arguments> regionTestData() {
        return Stream.of(
            Arguments.of("us-east-1", "api.ecr.us-east-1.amazonaws.com"),
            Arguments.of("us-west-2", "api.ecr.us-west-2.amazonaws.com"),
            Arguments.of("eu-west-1", "api.ecr.eu-west-1.amazonaws.com"),
            Arguments.of("eu-central-1", "api.ecr.eu-central-1.amazonaws.com"),
            Arguments.of("ap-south-1", "api.ecr.ap-south-1.amazonaws.com"),
            Arguments.of("ap-southeast-1", "api.ecr.ap-southeast-1.amazonaws.com")
        );
    }

    /**
     * Testable version of EcrExtendedAuth that allows injecting custom endpoint for testing
     */
    private static class TestableEcrExtendedAuth extends EcrExtendedAuth {
        private final String testEndpoint;

        public TestableEcrExtendedAuth(KitLogger logger, String registry, String endpoint) {
            super(logger, registry);
            this.testEndpoint = endpoint;
        }

        @Override
        protected String getEndpointUrl() {
            return testEndpoint;
        }
    }

}
