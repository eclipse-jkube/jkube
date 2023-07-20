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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AwsSdkAuthConfigFactoryTest {
    private AwsSdkHelper awsSdkHelper;

    private AwsSdkAuthConfigFactory objectUnderTest;

    @BeforeEach
    void setup() {
        awsSdkHelper = mock(AwsSdkHelper.class);
        objectUnderTest = new AwsSdkAuthConfigFactory(new KitLogger.SilentLogger(), awsSdkHelper);
    }

    @Test
    void nullValueIsPassedOn() {
        AuthConfig authConfig = objectUnderTest.createAuthConfig();

        assertThat(authConfig).isNull();
    }

    @Test
    void reflectionWorksForBasicCredentials() throws Exception {
        String accessKey = randomUUID().toString();
        String secretKey = randomUUID().toString();
        Object credentials = new Object();
        when(awsSdkHelper.getCredentialsFromDefaultAWSCredentialsProviderChain()).thenReturn(credentials);
        when(awsSdkHelper.getAWSAccessKeyIdFromCredentials(any())).thenReturn(accessKey);
        when(awsSdkHelper.getAwsSecretKeyFromCredentials(any())).thenReturn(secretKey);
        AuthConfig authConfig = objectUnderTest.createAuthConfig();

        assertThat(authConfig).isNotNull()
                .hasFieldOrPropertyWithValue("username", accessKey)
                .hasFieldOrPropertyWithValue("password", secretKey)
                .hasFieldOrPropertyWithValue("auth", null)
                .hasFieldOrPropertyWithValue("identityToken", null);
    }

    @Test
    void reflectionWorksForSessionCredentials() throws Exception {
        String accessKey = randomUUID().toString();
        String secretKey = randomUUID().toString();
        String sessionToken = randomUUID().toString();
        Object credentials = new Object();
        when(awsSdkHelper.getCredentialsFromDefaultAWSCredentialsProviderChain()).thenReturn(credentials);
        when(awsSdkHelper.getAWSAccessKeyIdFromCredentials(any())).thenReturn(accessKey);
        when(awsSdkHelper.getAwsSecretKeyFromCredentials(any())).thenReturn(secretKey);
        when(awsSdkHelper.getSessionTokenFromCrendentials(any())).thenReturn(sessionToken);
        AuthConfig authConfig = objectUnderTest.createAuthConfig();

        assertThat(authConfig).isNotNull()
                .hasFieldOrPropertyWithValue("username", accessKey)
                .hasFieldOrPropertyWithValue("password", secretKey)
                .hasFieldOrPropertyWithValue("auth", sessionToken)
                .hasFieldOrPropertyWithValue("identityToken", null);
    }

}
