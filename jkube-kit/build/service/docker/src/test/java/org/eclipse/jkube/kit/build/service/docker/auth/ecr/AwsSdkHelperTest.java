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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AwsSdkHelperTest {
  private AwsSdkAuthHelper mockDelegate;
  private AwsSdkHelper awsSdkHelper;

  @BeforeEach
  void setUp() {
    mockDelegate = mock(AwsSdkAuthHelper.class);
    awsSdkHelper = new AwsSdkHelper(mockDelegate);
  }

  @Test
  void isAwsSdkAvailable_delegatesToHelper() {
    when(mockDelegate.isAwsSdkAvailable()).thenReturn(true);

    assertThat(awsSdkHelper.isAwsSdkAvailable()).isTrue();
  }

  @Test
  void getSdkVersion_delegatesToHelper() {
    when(mockDelegate.getSdkVersion()).thenReturn("v2");

    assertThat(awsSdkHelper.getSdkVersion()).isEqualTo("v2");
  }

  @Test
  void getAwsAccessKeyIdEnvVar_delegatesToHelper() {
    when(mockDelegate.getAwsAccessKeyIdEnvVar()).thenReturn("AKIAIOSFODNN7EXAMPLE");

    assertThat(awsSdkHelper.getAwsAccessKeyIdEnvVar()).isEqualTo("AKIAIOSFODNN7EXAMPLE");
  }

  @Test
  void getAwsSecretAccessKeyEnvVar_delegatesToHelper() {
    when(mockDelegate.getAwsSecretAccessKeyEnvVar()).thenReturn("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

    assertThat(awsSdkHelper.getAwsSecretAccessKeyEnvVar()).isEqualTo("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
  }

  @Test
  void getAwsSessionTokenEnvVar_delegatesToHelper() {
    when(mockDelegate.getAwsSessionTokenEnvVar()).thenReturn("sessionToken");

    assertThat(awsSdkHelper.getAwsSessionTokenEnvVar()).isEqualTo("sessionToken");
  }

  @Test
  void getAwsContainerCredentialsRelativeUri_delegatesToHelper() {
    when(mockDelegate.getAwsContainerCredentialsRelativeUri()).thenReturn("/v2/credentials");

    assertThat(awsSdkHelper.getAwsContainerCredentialsRelativeUri()).isEqualTo("/v2/credentials");
  }

  @Test
  void getEcsMetadataEndpoint_delegatesToHelper() {
    when(mockDelegate.getEcsMetadataEndpoint()).thenReturn("http://169.254.170.2");

    assertThat(awsSdkHelper.getEcsMetadataEndpoint()).isEqualTo("http://169.254.170.2");
  }

  @Test
  void getCredentialsFromDefaultCredentialsProvider_delegatesToHelper() {
    AuthConfig expectedConfig = AuthConfig.builder()
        .username("AKIAIOSFODNN7EXAMPLE")
        .password("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
        .build();
    when(mockDelegate.getCredentialsFromDefaultCredentialsProvider()).thenReturn(expectedConfig);

    AuthConfig result = awsSdkHelper.getCredentialsFromDefaultCredentialsProvider();

    assertThat(result).isEqualTo(expectedConfig);
  }

  @Test
  void constructor_withoutArguments_createsHelperBasedOnClasspath() {
    // This test verifies that the default constructor works
    // The actual SDK detection is tested in the v1/v2 specific tests
    AwsSdkHelper helper = new AwsSdkHelper();
    assertThat(helper).isNotNull();
    assertThat(helper.getSdkVersion()).isIn("v1", "v2");
  }
}