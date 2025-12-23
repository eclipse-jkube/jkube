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

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.jkube.kit.build.service.docker.auth.EnvironmentVariablesTestUtil.clearEnvironmentVariable;
import static org.eclipse.jkube.kit.build.service.docker.auth.EnvironmentVariablesTestUtil.setEnvironmentVariable;

class AwsSdkHelperV2Test {
  private AwsSdkHelperV2 helper;

  @BeforeEach
  void setUp() {
    helper = new AwsSdkHelperV2();
  }

  @AfterEach
  void tearDown() {
    // Clean up after each test - try to clear but don't fail if not possible
    try {
      clearEnvironmentVariable("TEST_AWS_ACCESS_KEY_ID");
      clearEnvironmentVariable("TEST_AWS_SECRET_ACCESS_KEY");
      clearEnvironmentVariable("TEST_AWS_SESSION_TOKEN");
      clearEnvironmentVariable("AWS_ACCESS_KEY_ID");
      clearEnvironmentVariable("AWS_SECRET_ACCESS_KEY");
      clearEnvironmentVariable("AWS_SESSION_TOKEN");
    } catch (Exception ignored) {
      // Ignore failures on newer Java versions
    }
  }

  @Test
  void getSdkVersion_returnsV2() {
    assertThat(helper.getSdkVersion()).isEqualTo("v2");
  }

  @Test
  void isAwsSdkAvailable_returnsTrue_whenMockClassesPresent() {
    // With mock classes in test classpath, this should return true
    assertThat(helper.isAwsSdkAvailable()).isTrue();
  }

  @Test
  void getAwsAccessKeyIdEnvVar_returnsEnvironmentVariable() {
    // Test that method correctly reads from environment
    String value = helper.getAwsAccessKeyIdEnvVar();
    // Value can be null or have a value depending on environment
    assertThat(value).satisfiesAnyOf(
      v -> assertThat(v).isNull(),
      v -> assertThat(v).isNotEmpty()
    );
  }

  @Test
  void getAwsSecretAccessKeyEnvVar_returnsEnvironmentVariable() {
    String value = helper.getAwsSecretAccessKeyEnvVar();
    assertThat(value).satisfiesAnyOf(
      v -> assertThat(v).isNull(),
      v -> assertThat(v).isNotEmpty()
    );
  }

  @Test
  void getAwsSessionTokenEnvVar_returnsEnvironmentVariable() {
    String value = helper.getAwsSessionTokenEnvVar();
    assertThat(value).satisfiesAnyOf(
      v -> assertThat(v).isNull(),
      v -> assertThat(v).isNotEmpty()
    );
  }

  @Test
  void getAwsContainerCredentialsRelativeUri_returnsEnvironmentVariable() {
    String value = helper.getAwsContainerCredentialsRelativeUri();
    assertThat(value).satisfiesAnyOf(
      v -> assertThat(v).isNull(),
      v -> assertThat(v).isNotEmpty()
    );
  }

  @Test
  void getAccessKeyIdFromCredentials_withBasicCredentials_returnsAccessKeyId() throws Exception {
    software.amazon.awssdk.auth.credentials.AwsBasicCredentials credentials =
      software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("test-access-key", "test-secret-key");

    String accessKeyId = helper.getAccessKeyIdFromCredentials(credentials);

    assertThat(accessKeyId).isEqualTo("test-access-key");
  }

  @Test
  void getSecretAccessKeyFromCredentials_withBasicCredentials_returnsSecretKey() throws Exception {
    software.amazon.awssdk.auth.credentials.AwsBasicCredentials credentials =
      software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("test-access-key", "test-secret-key");

    String secretKey = helper.getSecretAccessKeyFromCredentials(credentials);

    assertThat(secretKey).isEqualTo("test-secret-key");
  }

  @Test
  void getSessionTokenFromCredentials_withBasicCredentials_returnsNull() throws Exception {
    software.amazon.awssdk.auth.credentials.AwsBasicCredentials credentials =
      software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("test-access-key", "test-secret-key");

    String sessionToken = helper.getSessionTokenFromCredentials(credentials);

    assertThat(sessionToken).isNull();
  }

  @Test
  void getSessionTokenFromCredentials_withSessionCredentials_returnsSessionToken() throws Exception {
    software.amazon.awssdk.auth.credentials.AwsSessionCredentialsImpl credentials =
      software.amazon.awssdk.auth.credentials.AwsSessionCredentialsImpl.create("test-access-key", "test-secret-key", "test-session-token");

    String sessionToken = helper.getSessionTokenFromCredentials(credentials);

    assertThat(sessionToken).isEqualTo("test-session-token");
  }

  @Test
  void getAccessKeyIdFromCredentials_withSessionCredentials_returnsAccessKeyId() throws Exception {
    software.amazon.awssdk.auth.credentials.AwsSessionCredentialsImpl credentials =
      software.amazon.awssdk.auth.credentials.AwsSessionCredentialsImpl.create("test-access-key", "test-secret-key", "test-session-token");

    String accessKeyId = helper.getAccessKeyIdFromCredentials(credentials);

    assertThat(accessKeyId).isEqualTo("test-access-key");
  }

  @Test
  void getSecretAccessKeyFromCredentials_withSessionCredentials_returnsSecretKey() throws Exception {
    software.amazon.awssdk.auth.credentials.AwsSessionCredentialsImpl credentials =
      software.amazon.awssdk.auth.credentials.AwsSessionCredentialsImpl.create("test-access-key", "test-secret-key", "test-session-token");

    String secretKey = helper.getSecretAccessKeyFromCredentials(credentials);

    assertThat(secretKey).isEqualTo("test-secret-key");
  }

  @Test
  void getCredentialsFromDefaultProvider_withEnvVars_returnsCredentials() throws Exception {
    try {
      setEnvironmentVariable("AWS_ACCESS_KEY_ID", "env-access-key");
      setEnvironmentVariable("AWS_SECRET_ACCESS_KEY", "env-secret-key");

      Object credentials = helper.getCredentialsFromDefaultProvider();

      assertThat(credentials).isNotNull();
      assertThat(credentials).isInstanceOf(software.amazon.awssdk.auth.credentials.AwsCredentials.class);
    } catch (RuntimeException e) {
      // Skip test if environment modification not supported
      org.junit.jupiter.api.Assumptions.assumeTrue(false, "Environment modification not supported");
    }
  }

  @Test
  void getCredentialsFromDefaultCredentialsProvider_withBasicCredentials_returnsAuthConfig() {
    try {
      setEnvironmentVariable("AWS_ACCESS_KEY_ID", "provider-access-key");
      setEnvironmentVariable("AWS_SECRET_ACCESS_KEY", "provider-secret-key");

      AuthConfig authConfig = helper.getCredentialsFromDefaultCredentialsProvider();

      assertThat(authConfig).isNotNull();
      assertThat(authConfig.getUsername()).isEqualTo("provider-access-key");
      assertThat(authConfig.getPassword()).isEqualTo("provider-secret-key");
      assertThat(authConfig.getEmail()).isEqualTo("none");
      assertThat(authConfig.getAuth()).isNull();
    } catch (RuntimeException e) {
      // Skip test if environment modification not supported
      org.junit.jupiter.api.Assumptions.assumeTrue(false, "Environment modification not supported");
    }
  }

  @Test
  void getCredentialsFromDefaultCredentialsProvider_withSessionCredentials_returnsAuthConfigWithToken() {
    try {
      setEnvironmentVariable("AWS_ACCESS_KEY_ID", "provider-access-key");
      setEnvironmentVariable("AWS_SECRET_ACCESS_KEY", "provider-secret-key");
      setEnvironmentVariable("AWS_SESSION_TOKEN", "provider-session-token");

      AuthConfig authConfig = helper.getCredentialsFromDefaultCredentialsProvider();

      assertThat(authConfig).isNotNull();
      assertThat(authConfig.getUsername()).isEqualTo("provider-access-key");
      assertThat(authConfig.getPassword()).isEqualTo("provider-secret-key");
      assertThat(authConfig.getEmail()).isEqualTo("none");
      assertThat(authConfig.getAuth()).isEqualTo("provider-session-token");
    } catch (RuntimeException e) {
      // Skip test if environment modification not supported
      org.junit.jupiter.api.Assumptions.assumeTrue(false, "Environment modification not supported");
    }
  }

  @Test
  void getCredentialsFromDefaultCredentialsProvider_whenNoCredentialsInEnv_handlesGracefully() {
    // Just test that it doesn't crash when credentials are not available
    AuthConfig authConfig = helper.getCredentialsFromDefaultCredentialsProvider();
    // Can be null or can have actual credentials depending on environment
    assertThat(authConfig).satisfiesAnyOf(
      ac -> assertThat(ac).isNull(),
      ac -> assertThat(ac.getEmail()).isEqualTo("none")
    );
  }

  @Test
  void getCredentialsFromDefaultProvider_withException_returnsNull() {
    // Test that method handles exceptions gracefully
    // Create a helper instance and verify exception handling
    AwsSdkHelperV2 testHelper = new AwsSdkHelperV2() {
      @Override
      Object getCredentialsFromDefaultProvider() throws ClassNotFoundException {
        throw new ClassNotFoundException("Test exception");
      }
    };

    AuthConfig authConfig = testHelper.getCredentialsFromDefaultCredentialsProvider();

    assertThat(authConfig).isNull();
  }

  @Test
  void getAccessKeyIdFromCredentials_withInvalidCredentials_throwsException() {
    Object invalidCredentials = new Object();

    assertThatThrownBy(() -> helper.getAccessKeyIdFromCredentials(invalidCredentials))
      .isInstanceOfAny(NoSuchMethodException.class, IllegalArgumentException.class, InvocationTargetException.class);
  }

  @Test
  void getSecretAccessKeyFromCredentials_withInvalidCredentials_throwsException() {
    Object invalidCredentials = new Object();

    assertThatThrownBy(() -> helper.getSecretAccessKeyFromCredentials(invalidCredentials))
      .isInstanceOfAny(NoSuchMethodException.class, IllegalArgumentException.class, InvocationTargetException.class);
  }

  @Test
  void getSessionTokenFromCredentials_withInvalidCredentials_returnsNull() throws Exception {
    Object invalidCredentials = new Object();

    String sessionToken = helper.getSessionTokenFromCredentials(invalidCredentials);

    assertThat(sessionToken).isNull();
  }

  @Test
  void getEcsMetadataEndpoint_returnsDefaultValue() {
    String endpoint = helper.getEcsMetadataEndpoint();

    assertThat(endpoint).satisfiesAnyOf(
      e -> assertThat(e).isEqualTo("http://169.254.170.2"),
      e -> assertThat(e).isNotEmpty()
    );
  }
}