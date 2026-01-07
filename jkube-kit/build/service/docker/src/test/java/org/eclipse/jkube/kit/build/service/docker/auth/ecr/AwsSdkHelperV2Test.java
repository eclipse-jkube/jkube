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
import org.eclipse.jkube.kit.common.TestEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AwsSdkHelperV2Test {
  private AwsSdkHelperV2 helper;
  private TestEnvironment testEnv;

  @BeforeEach
  void setUp() {
    testEnv = new TestEnvironment();
    helper = new AwsSdkHelperV2(testEnv);
  }

  @Test
  void getSdkVersion_returnsV2() {
    assertThat(helper.getSdkVersion()).isEqualTo("v2");
  }

  @Test
  void isAwsSdkAvailable_returnsTrue_whenMockClassesPresent() {
    assertThat(helper.isAwsSdkAvailable()).isTrue();
  }

  @Test
  void getAwsAccessKeyIdEnvVar_whenNotSet_returnsNull() {
    String value = helper.getAwsAccessKeyIdEnvVar();
    assertThat(value).isNull();
  }

  @Test
  void getAwsAccessKeyIdEnvVar_whenSet_returnsValue() {
    testEnv.put("AWS_ACCESS_KEY_ID", "test-key-123");
    String value = helper.getAwsAccessKeyIdEnvVar();
    assertThat(value).isEqualTo("test-key-123");
  }

  @Test
  void getAwsSecretAccessKeyEnvVar_whenNotSet_returnsNull() {
    String value = helper.getAwsSecretAccessKeyEnvVar();
    assertThat(value).isNull();
  }

  @Test
  void getAwsSecretAccessKeyEnvVar_whenSet_returnsValue() {
    testEnv.put("AWS_SECRET_ACCESS_KEY", "test-secret-456");
    String value = helper.getAwsSecretAccessKeyEnvVar();
    assertThat(value).isEqualTo("test-secret-456");
  }

  @Test
  void getAwsSessionTokenEnvVar_whenNotSet_returnsNull() {
    String value = helper.getAwsSessionTokenEnvVar();
    assertThat(value).isNull();
  }

  @Test
  void getAwsSessionTokenEnvVar_whenSet_returnsValue() {
    testEnv.put("AWS_SESSION_TOKEN", "test-token-789");
    String value = helper.getAwsSessionTokenEnvVar();
    assertThat(value).isEqualTo("test-token-789");
  }

  @Test
  void getAwsContainerCredentialsRelativeUri_whenNotSet_returnsNull() {
    String value = helper.getAwsContainerCredentialsRelativeUri();
    assertThat(value).isNull();
  }

  @Test
  void getAwsContainerCredentialsRelativeUri_whenSet_returnsValue() {
    testEnv.put("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI", "/v2/credentials/test-uuid");
    String value = helper.getAwsContainerCredentialsRelativeUri();
    assertThat(value).isEqualTo("/v2/credentials/test-uuid");
  }

  @Test
  void getAccessKeyIdFromCredentials_withAwsCredentials_returnsAccessKeyId() throws Exception {
    software.amazon.awssdk.auth.credentials.AwsCredentials credentials =
      software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("test-access-key", "test-secret-key");

    String accessKeyId = helper.getAccessKeyIdFromCredentials(credentials);

    assertThat(accessKeyId).isEqualTo("test-access-key");
  }

  @Test
  void getSecretKeyFromCredentials_withAwsCredentials_returnsSecretKey() throws Exception {
    software.amazon.awssdk.auth.credentials.AwsCredentials credentials =
      software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("test-access-key", "test-secret-key");

    String secretKey = helper.getSecretAccessKeyFromCredentials(credentials);

    assertThat(secretKey).isEqualTo("test-secret-key");
  }

  @Test
  void getSessionTokenFromCredentials_withBasicCredentials_returnsNull() throws Exception {
    software.amazon.awssdk.auth.credentials.AwsCredentials credentials =
      software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("test-access-key", "test-secret-key");

    String sessionToken = helper.getSessionTokenFromCredentials(credentials);

    assertThat(sessionToken).isNull();
  }

  @Test
  void getSessionTokenFromCredentials_withSessionCredentials_returnsSessionToken() throws Exception {
    software.amazon.awssdk.auth.credentials.AwsSessionCredentials credentials = new TestAwsSessionCredentials(
      "test-access-key", "test-secret-key", "test-session-token"
    );

    String sessionToken = helper.getSessionTokenFromCredentials(credentials);

    assertThat(sessionToken).isEqualTo("test-session-token");
  }

  @Test
  void getAccessKeyIdFromCredentials_withSessionCredentials_returnsAccessKeyId() throws Exception {
    software.amazon.awssdk.auth.credentials.AwsSessionCredentials credentials = new TestAwsSessionCredentials(
      "test-access-key", "test-secret-key", "test-session-token"
    );

    String accessKeyId = helper.getAccessKeyIdFromCredentials(credentials);

    assertThat(accessKeyId).isEqualTo("test-access-key");
  }

  @Test
  void getSecretKeyFromCredentials_withSessionCredentials_returnsSecretKey() throws Exception {
    software.amazon.awssdk.auth.credentials.AwsSessionCredentials credentials = new TestAwsSessionCredentials(
      "test-access-key", "test-secret-key", "test-session-token"
    );

    String secretKey = helper.getSecretAccessKeyFromCredentials(credentials);

    assertThat(secretKey).isEqualTo("test-secret-key");
  }

  /**
   * Test implementation of AWS Session Credentials for testing purposes
   */
  private static class TestAwsSessionCredentials implements software.amazon.awssdk.auth.credentials.AwsSessionCredentials {
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String sessionToken;

    TestAwsSessionCredentials(String accessKeyId, String secretAccessKey, String sessionToken) {
      this.accessKeyId = accessKeyId;
      this.secretAccessKey = secretAccessKey;
      this.sessionToken = sessionToken;
    }

    @Override
    public String accessKeyId() {
      return accessKeyId;
    }

    @Override
    public String secretAccessKey() {
      return secretAccessKey;
    }

    @Override
    public String sessionToken() {
      return sessionToken;
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
    AwsSdkHelperV2 testHelper = new AwsSdkHelperV2(testEnv) {
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
  void getSecretKeyFromCredentials_withInvalidCredentials_throwsException() {
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
  void getEcsMetadataEndpoint_whenNotSet_returnsDefaultValue() {
    String endpoint = helper.getEcsMetadataEndpoint();
    assertThat(endpoint).isEqualTo("http://169.254.170.2");
  }

  @Test
  void getEcsMetadataEndpoint_whenSet_returnsValue() {
    testEnv.put("ECS_METADATA_ENDPOINT", "http://custom:8080");
    String endpoint = helper.getEcsMetadataEndpoint();
    assertThat(endpoint).isEqualTo("http://custom:8080");
  }
}