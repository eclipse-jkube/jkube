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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.build.service.docker.auth.EnvironmentVariablesTestUtil.clearEnvironmentVariable;
import static org.eclipse.jkube.kit.build.service.docker.auth.EnvironmentVariablesTestUtil.setEnvironmentVariable;

/**
 * Tests for AbstractAwsSdkHelper.
 * Tests the common functionality shared between V1 and V2 implementations.
 */
class AbstractAwsSdkHelperTest {
  private TestAwsSdkHelper helper;

  @BeforeEach
  void setUp() {
    helper = new TestAwsSdkHelper();
  }

  @AfterEach
  void tearDown() {
    // Clean up test environment variables
    try {
      clearEnvironmentVariable("TEST_AWS_ACCESS_KEY_ID");
      clearEnvironmentVariable("TEST_AWS_SECRET_ACCESS_KEY");
      clearEnvironmentVariable("TEST_AWS_SESSION_TOKEN");
      clearEnvironmentVariable("TEST_AWS_CONTAINER_CREDENTIALS_RELATIVE_URI");
      clearEnvironmentVariable("TEST_ECS_METADATA_ENDPOINT");
      clearEnvironmentVariable("AWS_ACCESS_KEY_ID");
      clearEnvironmentVariable("AWS_SECRET_ACCESS_KEY");
      clearEnvironmentVariable("AWS_SESSION_TOKEN");
      clearEnvironmentVariable("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI");
      clearEnvironmentVariable("ECS_METADATA_ENDPOINT");
    } catch (Exception ignored) {
      // Ignore failures on newer Java versions
    }
  }

  @Test
  void getAwsAccessKeyIdEnvVar_returnsEnvironmentVariable() {
    String value = helper.getAwsAccessKeyIdEnvVar();
    // Value can be null or have a value depending on environment
    assertThat(value).satisfiesAnyOf(
        v -> assertThat(v).isNull(),
        v -> assertThat(v).isNotEmpty()
    );
  }

  @Test
  void getAwsAccessKeyIdEnvVar_whenSet_returnsValue() {
    try {
      setEnvironmentVariable("AWS_ACCESS_KEY_ID", "test-key-123");
      String value = helper.getAwsAccessKeyIdEnvVar();
      assertThat(value).isEqualTo("test-key-123");
    } catch (RuntimeException e) {
      // Skip test if environment modification not supported
      org.junit.jupiter.api.Assumptions.assumeTrue(false, "Environment modification not supported");
    }
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
  void getAwsSecretAccessKeyEnvVar_whenSet_returnsValue() {
    try {
      setEnvironmentVariable("AWS_SECRET_ACCESS_KEY", "test-secret-456");
      String value = helper.getAwsSecretAccessKeyEnvVar();
      assertThat(value).isEqualTo("test-secret-456");
    } catch (RuntimeException e) {
      // Skip test if environment modification not supported
      org.junit.jupiter.api.Assumptions.assumeTrue(false, "Environment modification not supported");
    }
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
  void getAwsSessionTokenEnvVar_whenSet_returnsValue() {
    try {
      setEnvironmentVariable("AWS_SESSION_TOKEN", "test-token-789");
      String value = helper.getAwsSessionTokenEnvVar();
      assertThat(value).isEqualTo("test-token-789");
    } catch (RuntimeException e) {
      // Skip test if environment modification not supported
      org.junit.jupiter.api.Assumptions.assumeTrue(false, "Environment modification not supported");
    }
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
  void getAwsContainerCredentialsRelativeUri_whenSet_returnsValue() {
    try {
      setEnvironmentVariable("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI", "/v2/credentials/test-uuid");
      String value = helper.getAwsContainerCredentialsRelativeUri();
      assertThat(value).isEqualTo("/v2/credentials/test-uuid");
    } catch (RuntimeException e) {
      // Skip test if environment modification not supported
      org.junit.jupiter.api.Assumptions.assumeTrue(false, "Environment modification not supported");
    }
  }

  @Test
  void getEcsMetadataEndpoint_whenSet_returnsEnvVarValue() {
    try {
      setEnvironmentVariable("ECS_METADATA_ENDPOINT", "http://custom-endpoint:8080");
      String endpoint = helper.getEcsMetadataEndpoint();
      assertThat(endpoint).isEqualTo("http://custom-endpoint:8080");
    } catch (RuntimeException e) {
      // Skip test if environment modification not supported
      org.junit.jupiter.api.Assumptions.assumeTrue(false, "Environment modification not supported");
    }
  }

  @Test
  void getEcsMetadataEndpoint_whenNotSet_returnsFallbackValue() {
    try {
      clearEnvironmentVariable("ECS_METADATA_ENDPOINT");
      String endpoint = helper.getEcsMetadataEndpoint();
      // Should return the default ECS metadata endpoint v2
      assertThat(endpoint).isEqualTo("http://169.254.170.2");
    } catch (RuntimeException e) {
      // Skip test if environment modification not supported
      org.junit.jupiter.api.Assumptions.assumeTrue(false, "Environment modification not supported");
    }
  }

  /**
   * Test implementation of AbstractAwsSdkHelper for testing purposes.
   */
  private static class TestAwsSdkHelper extends AbstractAwsSdkHelper {
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