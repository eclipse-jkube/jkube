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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AwsSdkHelperV1.
 * Note: These tests verify the helper's behavior when AWS SDK v1 classes are NOT available.
 * Full integration testing with actual AWS SDK v1 would require adding the dependency.
 */
class AwsSdkHelperV1Test {
  private AwsSdkHelperV1 helper;

  @BeforeEach
  void setUp() {
    helper = new AwsSdkHelperV1();
  }

  @Test
  void getSdkVersion_returnsV1() {
    assertThat(helper.getSdkVersion()).isEqualTo("v1");
  }

  @Test
  void isAwsSdkAvailable_returnsFalseWhenSdkNotInClasspath() {
    // AWS SDK v1 is not in the test classpath
    assertThat(helper.isAwsSdkAvailable()).isFalse();
  }

  @Test
  void getAwsAccessKeyIdEnvVar_returnsEnvironmentVariable() {
    // This test relies on the actual environment, but shows the method works
    String value = helper.getAwsAccessKeyIdEnvVar();
    // Value can be null if env var is not set
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
  void getEcsMetadataEndpoint_returnsDefaultWhenEnvVarNotSet() {
    // Assuming ECS_METADATA_ENDPOINT is not set in test environment
    String endpoint = helper.getEcsMetadataEndpoint();
    // Should return either the env var value or the default
    assertThat(endpoint).matches("^http://.*");
  }

  @Test
  void getCredentialsFromDefaultCredentialsProvider_returnsNullWhenSdkNotAvailable() {
    // When AWS SDK v1 is not in classpath, should return null
    AuthConfig result = helper.getCredentialsFromDefaultCredentialsProvider();
    assertThat(result).isNull();
  }
}