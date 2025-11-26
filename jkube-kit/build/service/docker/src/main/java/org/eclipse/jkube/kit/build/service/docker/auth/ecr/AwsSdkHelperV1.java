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

import java.lang.reflect.InvocationTargetException;

/**
 * AWS SDK v1 authentication helper.
 * Uses reflection to avoid hard dependency on AWS SDK v1.
 */
public class AwsSdkHelperV1 implements AwsSdkAuthHelper {
  private static final String ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID";
  private static final String SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY";
  private static final String SESSION_TOKEN = "AWS_SESSION_TOKEN";
  private static final String CONTAINER_CREDENTIALS_RELATIVE_URI = "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI";
  private static final String METADATA_ENDPOINT = "ECS_METADATA_ENDPOINT";
  private static final String AWS_INSTANCE_LINK_LOCAL_ADDRESS = "http://169.254.170.2";
  private static final String DEFAULT_AWSCREDENTIALS_PROVIDER_CHAIN = "com.amazonaws.auth.DefaultAWSCredentialsProviderChain";
  private static final String AWS_SESSION_CREDENTIALS = "com.amazonaws.auth.AWSSessionCredentials";
  private static final String AWS_CREDENTIALS = "com.amazonaws.auth.AWSCredentials";

  @Override
  public boolean isAwsSdkAvailable() {
    try {
      Class.forName(DEFAULT_AWSCREDENTIALS_PROVIDER_CHAIN);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public String getSdkVersion() {
    return "v1";
  }

  @Override
  public String getAwsAccessKeyIdEnvVar() {
    return System.getenv(ACCESS_KEY_ID);
  }

  @Override
  public String getAwsSecretAccessKeyEnvVar() {
    return System.getenv(SECRET_ACCESS_KEY);
  }

  @Override
  public String getAwsSessionTokenEnvVar() {
    return System.getenv(SESSION_TOKEN);
  }

  @Override
  public String getAwsContainerCredentialsRelativeUri() {
    return System.getenv(CONTAINER_CREDENTIALS_RELATIVE_URI);
  }

  @Override
  public String getEcsMetadataEndpoint() {
    String endpoint = System.getenv(METADATA_ENDPOINT);
    if (endpoint == null) {
      return AWS_INSTANCE_LINK_LOCAL_ADDRESS;
    }
    return endpoint;
  }

  @Override
  public AuthConfig getCredentialsFromDefaultCredentialsProvider() {
    try {
      Object credentials = getCredentialsFromDefaultAWSCredentialsProviderChain();
      if (credentials == null) {
        return null;
      }

      return AuthConfig.builder()
          .username(getAWSAccessKeyIdFromCredentials(credentials))
          .password(getAwsSecretKeyFromCredentials(credentials))
          .email("none")
          .auth(getSessionTokenFromCredentials(credentials))
          .build();
    } catch (Exception e) {
      // Return null if credentials cannot be retrieved
      return null;
    }
  }

  private Object getCredentialsFromDefaultAWSCredentialsProviderChain() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    Class<?> credentialsProviderChainClass = Class.forName(DEFAULT_AWSCREDENTIALS_PROVIDER_CHAIN);
    Object credentialsProviderChain = credentialsProviderChainClass.getDeclaredConstructor().newInstance();
    return credentialsProviderChainClass.getMethod("getCredentials").invoke(credentialsProviderChain);
  }

  private String getSessionTokenFromCredentials(Object credentials) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> sessionCredentialsClass = Class.forName(AWS_SESSION_CREDENTIALS);
    return sessionCredentialsClass.isInstance(credentials)
        ? (String) sessionCredentialsClass.getMethod("getSessionToken").invoke(credentials) : null;
  }

  private String getAWSAccessKeyIdFromCredentials(Object credentials) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> credentialsClass = Class.forName(AWS_CREDENTIALS);
    return (String) credentialsClass.getMethod("getAWSAccessKeyId").invoke(credentials);
  }

  private String getAwsSecretKeyFromCredentials(Object credentials) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> credentialsClass = Class.forName(AWS_CREDENTIALS);
    return (String) credentialsClass.getMethod("getAWSSecretKey").invoke(credentials);
  }
}
