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

import java.lang.reflect.InvocationTargetException;

public class AwsSdkHelper {
    private static final String ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID";
    private static final String SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY";
    private static final String SESSION_TOKEN = "AWS_SESSION_TOKEN";
    private static final String CONTAINER_CREDENTIALS_RELATIVE_URI = "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI";
    private static final String METADATA_ENDPOINT = "ECS_METADATA_ENDPOINT";
    private static final String AWS_INSTANCE_LINK_LOCAL_ADDRESS = "http://169.254.170.2";
    private static final String DEFAULT_AWSCREDENTIALS_PROVIDER_CHAIN = "com.amazonaws.auth.DefaultAWSCredentialsProviderChain";
    private static final String AWS_SESSION_CREDENTIALS = "com.amazonaws.auth.AWSSessionCredentials";
    private static final String AWS_CREDENTIALS = "com.amazonaws.auth.AWSCredentials";

    public boolean isDefaultAWSCredentialsProviderChainPresentInClassPath() {
        try {
            Class.forName(DEFAULT_AWSCREDENTIALS_PROVIDER_CHAIN);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public String getAwsAccessKeyIdEnvVar() {
        return System.getenv(ACCESS_KEY_ID);
    }

    public String getAwsSecretAccessKeyEnvVar() {
        return System.getenv(SECRET_ACCESS_KEY);
    }

    public String getAwsSessionTokenEnvVar() {
        return System.getenv(SESSION_TOKEN);
    }

    public String getAwsContainerCredentialsRelativeUri() {
        return System.getenv(CONTAINER_CREDENTIALS_RELATIVE_URI);
    }

    public String getEcsMetadataEndpoint() {
        String endpoint = System.getenv(METADATA_ENDPOINT);
        if (endpoint == null) {
            return AWS_INSTANCE_LINK_LOCAL_ADDRESS;
        }
        return endpoint;
    }

    public Object getCredentialsFromDefaultAWSCredentialsProviderChain() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> credentialsProviderChainClass = Class.forName(DEFAULT_AWSCREDENTIALS_PROVIDER_CHAIN);
        Object credentialsProviderChain = credentialsProviderChainClass.getDeclaredConstructor().newInstance();
        return credentialsProviderChainClass.getMethod("getCredentials").invoke(credentialsProviderChain);
    }

    public String getSessionTokenFromCrendentials(Object credentials) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> sessionCredentialsClass = Class.forName(AWS_SESSION_CREDENTIALS);
        return sessionCredentialsClass.isInstance(credentials)
                ? (String) sessionCredentialsClass.getMethod("getSessionToken").invoke(credentials) : null;
    }

    public String getAWSAccessKeyIdFromCredentials(Object credentials) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> credentialsClass = Class.forName(AWS_CREDENTIALS);
        return (String) credentialsClass.getMethod("getAWSAccessKeyId").invoke(credentials);
    }

    public String getAwsSecretKeyFromCredentials(Object credentials) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> credentialsClass = Class.forName(AWS_CREDENTIALS);
        return (String) credentialsClass.getMethod("getAWSSecretKey").invoke(credentials);
    }
}
