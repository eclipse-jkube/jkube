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
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AwsSdkDockerAuthConfigFactoryTest {
  private AwsSdkAuthHelper mockDelegate;

  private AwsSdkAuthConfigFactory objectUnderTest;

  @BeforeEach
  void setup() {
    mockDelegate = mock(AwsSdkAuthHelper.class);
    AwsSdkHelper awsSdkHelper = new AwsSdkHelper(mockDelegate);
    objectUnderTest = new AwsSdkAuthConfigFactory(new KitLogger.SilentLogger(), awsSdkHelper);
  }

  @Test
  void nullValueIsPassedOn() {
    when(mockDelegate.getCredentialsFromDefaultCredentialsProvider()).thenReturn(null);

    AuthConfig authConfig = objectUnderTest.createAuthConfig();

    assertThat(authConfig).isNull();
  }

  @Test
  void reflectionWorksForBasicCredentials() {
    String accessKey = randomUUID().toString();
    String secretKey = randomUUID().toString();
    AuthConfig expectedAuthConfig = AuthConfig.builder()
      .username(accessKey)
      .password(secretKey)
      .email("none")
      .build();

    when(mockDelegate.getCredentialsFromDefaultCredentialsProvider()).thenReturn(expectedAuthConfig);
    when(mockDelegate.getSdkVersion()).thenReturn("v2");

    AuthConfig authConfig = objectUnderTest.createAuthConfig();

    assertThat(authConfig).isNotNull()
        .hasFieldOrPropertyWithValue("username", accessKey)
        .hasFieldOrPropertyWithValue("password", secretKey)
        .hasFieldOrPropertyWithValue("auth", null)
        .hasFieldOrPropertyWithValue("identityToken", null);
  }

  @Test
  void reflectionWorksForSessionCredentials() {
    String accessKey = randomUUID().toString();
    String secretKey = randomUUID().toString();
    String sessionToken = randomUUID().toString();
    AuthConfig expectedAuthConfig = AuthConfig.builder()
      .username(accessKey)
      .password(secretKey)
      .email("none")
      .auth(sessionToken)
      .build();

    when(mockDelegate.getCredentialsFromDefaultCredentialsProvider()).thenReturn(expectedAuthConfig);
    when(mockDelegate.getSdkVersion()).thenReturn("v2");

    AuthConfig authConfig = objectUnderTest.createAuthConfig();

    assertThat(authConfig).isNotNull()
        .hasFieldOrPropertyWithValue("username", accessKey)
        .hasFieldOrPropertyWithValue("password", secretKey)
        .hasFieldOrPropertyWithValue("auth", sessionToken)
        .hasFieldOrPropertyWithValue("identityToken", null);
  }

  @Test
  void exceptionHandling_returnsNull() {
    when(mockDelegate.getCredentialsFromDefaultCredentialsProvider()).thenThrow(new RuntimeException("Test exception"));
    when(mockDelegate.getSdkVersion()).thenReturn("v2");

    AuthConfig authConfig = objectUnderTest.createAuthConfig();

    assertThat(authConfig).isNull();
  }

  @Test
  void exceptionHandling_withCause_returnsNull() {
    RuntimeException cause = new RuntimeException("Root cause");
    RuntimeException exception = new RuntimeException("Test exception", cause);
    when(mockDelegate.getCredentialsFromDefaultCredentialsProvider()).thenThrow(exception);
    when(mockDelegate.getSdkVersion()).thenReturn("v2");

    AuthConfig authConfig = objectUnderTest.createAuthConfig();

    assertThat(authConfig).isNull();
  }

  @Test
  void exceptionHandling_withNullMessage_returnsNull() {
    when(mockDelegate.getCredentialsFromDefaultCredentialsProvider()).thenThrow(new RuntimeException((String) null));
    when(mockDelegate.getSdkVersion()).thenReturn("v2");

    AuthConfig authConfig = objectUnderTest.createAuthConfig();

    assertThat(authConfig).isNull();
  }

  @Test
  void exceptionHandling_withVeryLongMessage_returnsNull() {
    StringBuilder sb = new StringBuilder("Failed calling AWS SDK: ");
    for (int i = 0; i < 1000; i++) {
      sb.append('x');
    }
    String longMessage = sb.toString();
    when(mockDelegate.getCredentialsFromDefaultCredentialsProvider()).thenThrow(new RuntimeException(longMessage));
    when(mockDelegate.getSdkVersion()).thenReturn("v1");

    AuthConfig authConfig = objectUnderTest.createAuthConfig();

    assertThat(authConfig).isNull();
  }

}
