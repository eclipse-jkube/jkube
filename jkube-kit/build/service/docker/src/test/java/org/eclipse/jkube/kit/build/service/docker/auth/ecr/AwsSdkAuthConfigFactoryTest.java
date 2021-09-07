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
package org.eclipse.jkube.kit.build.service.docker.auth.ecr;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.common.KitLogger;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

public class AwsSdkAuthConfigFactoryTest {
    @Mocked
    private KitLogger log;

    @Mocked
    private AwsSdkHelper awsSdkHelper;

    private AwsSdkAuthConfigFactory objectUnderTest;

    @Before
    public void setup() {
        objectUnderTest = new AwsSdkAuthConfigFactory(log, awsSdkHelper);
    }

    @Test
    public void nullValueIsPassedOn() {
        AuthConfig authConfig = objectUnderTest.createAuthConfig();

        assertNull(authConfig);
    }

    @Test
    public void reflectionWorksForBasicCredentials() throws Exception {
        String accessKey = randomUUID().toString();
        String secretKey = randomUUID().toString();
        Object credentials = new Object();
        new Expectations() {{
            awsSdkHelper.getCredentialsFromDefaultAWSCredentialsProviderChain();
            result = credentials;
            awsSdkHelper.getAWSAccessKeyIdFromCredentials(any);
            result = accessKey;
            awsSdkHelper.getAwsSecretKeyFromCredentials(any);
            result = secretKey;
        }};

        AuthConfig authConfig = objectUnderTest.createAuthConfig();

        assertNotNull(authConfig);
        assertEquals(accessKey, authConfig.getUsername());
        assertEquals(secretKey, authConfig.getPassword());
        assertNull(authConfig.getAuth());
        assertNull(authConfig.getIdentityToken());
    }

    @Test
    public void reflectionWorksForSessionCredentials() throws Exception {
        String accessKey = randomUUID().toString();
        String secretKey = randomUUID().toString();
        String sessionToken = randomUUID().toString();
        Object credentials = new Object();
        new Expectations() {{
            awsSdkHelper.getCredentialsFromDefaultAWSCredentialsProviderChain();
            result = credentials;
            awsSdkHelper.getAWSAccessKeyIdFromCredentials(any);
            result = accessKey;
            awsSdkHelper.getAwsSecretKeyFromCredentials(any);
            result = secretKey;
            awsSdkHelper.getSessionTokenFromCrendentials(any);
            result = sessionToken;
        }};
        AuthConfig authConfig = objectUnderTest.createAuthConfig();

        assertNotNull(authConfig);
        assertEquals(accessKey, authConfig.getUsername());
        assertEquals(secretKey, authConfig.getPassword());
        assertEquals(sessionToken, authConfig.getAuth());
        assertNull(authConfig.getIdentityToken());
    }

}