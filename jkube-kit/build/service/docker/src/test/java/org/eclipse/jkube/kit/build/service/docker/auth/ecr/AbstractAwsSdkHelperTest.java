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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AbstractAwsSdkHelper.
 * Tests the common functionality shared between V1 and V2 implementations.
 */
class AbstractAwsSdkHelperTest {
  private TestAwsSdkHelper helper;
  private TestEnvironment testEnv;

  @BeforeEach
  void setUp() {
    testEnv = new TestEnvironment();
    helper = new TestAwsSdkHelper(testEnv);
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
  void getEcsMetadataEndpoint_whenSet_returnsEnvVarValue() {
    testEnv.put("ECS_METADATA_ENDPOINT", "http://custom-endpoint:8080");
    String endpoint = helper.getEcsMetadataEndpoint();
    assertThat(endpoint).isEqualTo("http://custom-endpoint:8080");
  }

  @Test
  void getEcsMetadataEndpoint_whenNotSet_returnsFallbackValue() {
    String endpoint = helper.getEcsMetadataEndpoint();
    assertThat(endpoint).isEqualTo("http://169.254.170.2");
  }

  /**
   * Test implementation of AbstractAwsSdkHelper for testing purposes.
   */
  private static class TestAwsSdkHelper extends AbstractAwsSdkHelper {
    TestAwsSdkHelper(TestEnvironment environment) {
      super(environment);
    }

    @Override
    public boolean isAwsSdkAvailable() {
      return false;
    }

    @Override
    public String getSdkVersion() {
      return "test";
    }

    @Override
    public AuthConfig getCredentialsFromDefaultCredentialsProvider() {
      return null;
    }
  }
}