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

/**
 * Factory for AWS SDK helpers that supports both AWS SDK v1 and v2.
 * Automatically detects which SDK version is available on the classpath.
 * Maintains backward compatibility with the original AwsSdkHelper API.
 */
public class AwsSdkHelper {
  private final AwsSdkAuthHelper delegate;

  /**
   * Creates an AwsSdkHelper that automatically detects the available AWS SDK version.
   * Tries AWS SDK v2 first, then falls back to v1.
   */
  public AwsSdkHelper() {
    this.delegate = createHelper();
  }

  /**
   * Constructor for testing that allows injecting a specific helper implementation.
   *
   * @param delegate the helper implementation to use
   */
  public AwsSdkHelper(AwsSdkAuthHelper delegate) {
    this.delegate = delegate;
  }

  /**
   * Creates the appropriate AWS SDK helper based on what's available on the classpath.
   * Prefers AWS SDK v2 over v1 when both are available.
   *
   * @return AWS SDK helper instance
   */
  private static AwsSdkAuthHelper createHelper() {
    // Try v2 first (recommended)
    AwsSdkHelperV2 v2Helper = new AwsSdkHelperV2();
    if (v2Helper.isAwsSdkAvailable()) {
      return v2Helper;
    }

    // Fall back to v1
    AwsSdkHelperV1 v1Helper = new AwsSdkHelperV1();
    if (v1Helper.isAwsSdkAvailable()) {
      return v1Helper;
    }

    // Return v1 helper even if not available (for null checks compatibility)
    return v1Helper;
  }

  /**
   * Get the detected AWS SDK version.
   *
   * @return "v1", "v2", or "none" if no SDK is available
   */
  public String getSdkVersion() {
    return delegate.getSdkVersion();
  }

  /**
   * Check if AWS SDK credentials provider is present in the classpath.
   * Works with both AWS SDK v1 and v2.
   *
   * @return true if AWS SDK is available
   */
  public boolean isDefaultAWSCredentialsProviderChainPresentInClassPath() {
    return delegate.isAwsSdkAvailable();
  }

  public String getAwsAccessKeyIdEnvVar() {
    return delegate.getAwsAccessKeyIdEnvVar();
  }

  public String getAwsSecretAccessKeyEnvVar() {
    return delegate.getAwsSecretAccessKeyEnvVar();
  }

  public String getAwsSessionTokenEnvVar() {
    return delegate.getAwsSessionTokenEnvVar();
  }

  public String getAwsContainerCredentialsRelativeUri() {
    return delegate.getAwsContainerCredentialsRelativeUri();
  }

  public String getEcsMetadataEndpoint() {
    return delegate.getEcsMetadataEndpoint();
  }

  /**
   * Get credentials from the default AWS credentials provider chain.
   * This method is deprecated - use getAuthConfigFromDefaultCredentialsProvider() instead.
   *
   * @return credentials object (AWS SDK specific type)
   * @throws ClassNotFoundException    if AWS SDK classes not found
   * @throws NoSuchMethodException     if AWS SDK methods not found
   * @throws InvocationTargetException if AWS SDK method invocation fails
   * @throws InstantiationException    if AWS SDK object instantiation fails
   * @throws IllegalAccessException    if AWS SDK method access fails
   * @deprecated Use {@link #getAuthConfigFromDefaultCredentialsProvider()} instead
   */
  @Deprecated
  public Object getCredentialsFromDefaultAWSCredentialsProviderChain() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    // This method is kept for backward compatibility but we can't return the actual credentials object
    // since we don't know which SDK version is being used. Callers should use the new method.
    throw new UnsupportedOperationException(
        "This method is deprecated. Use getAuthConfigFromDefaultCredentialsProvider() instead, " +
            "which works with both AWS SDK v1 and v2.");
  }

  /**
   * Get session token from credentials object.
   * This method is deprecated - use getAuthConfigFromDefaultCredentialsProvider() instead.
   *
   * @param credentials credentials object
   * @return session token or null
   * @throws ClassNotFoundException    if AWS SDK classes not found
   * @throws NoSuchMethodException     if AWS SDK methods not found
   * @throws InvocationTargetException if AWS SDK method invocation fails
   * @throws IllegalAccessException    if AWS SDK method access fails
   * @deprecated Use {@link #getAuthConfigFromDefaultCredentialsProvider()} instead
   */
  @Deprecated
  public String getSessionTokenFromCrendentials(Object credentials) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    throw new UnsupportedOperationException(
        "This method is deprecated. Use getAuthConfigFromDefaultCredentialsProvider() instead, " +
            "which works with both AWS SDK v1 and v2.");
  }

  /**
   * Get AWS access key ID from credentials object.
   * This method is deprecated - use getAuthConfigFromDefaultCredentialsProvider() instead.
   *
   * @param credentials credentials object
   * @return access key ID
   * @throws ClassNotFoundException    if AWS SDK classes not found
   * @throws NoSuchMethodException     if AWS SDK methods not found
   * @throws InvocationTargetException if AWS SDK method invocation fails
   * @throws IllegalAccessException    if AWS SDK method access fails
   * @deprecated Use {@link #getAuthConfigFromDefaultCredentialsProvider()} instead
   */
  @Deprecated
  public String getAWSAccessKeyIdFromCredentials(Object credentials) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    throw new UnsupportedOperationException(
        "This method is deprecated. Use getAuthConfigFromDefaultCredentialsProvider() instead, " +
            "which works with both AWS SDK v1 and v2.");
  }

  /**
   * Get AWS secret access key from credentials object.
   * This method is deprecated - use getAuthConfigFromDefaultCredentialsProvider() instead.
   *
   * @param credentials credentials object
   * @return secret access key
   * @throws ClassNotFoundException    if AWS SDK classes not found
   * @throws NoSuchMethodException     if AWS SDK methods not found
   * @throws InvocationTargetException if AWS SDK method invocation fails
   * @throws IllegalAccessException    if AWS SDK method access fails
   * @deprecated Use {@link #getAuthConfigFromDefaultCredentialsProvider()} instead
   */
  @Deprecated
  public String getAwsSecretKeyFromCredentials(Object credentials) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    throw new UnsupportedOperationException(
        "This method is deprecated. Use getAuthConfigFromDefaultCredentialsProvider() instead, " +
            "which works with both AWS SDK v1 and v2.");
  }

  /**
   * Get AuthConfig from the default AWS credentials provider.
   * Works with both AWS SDK v1 and v2.
   *
   * @return AuthConfig with credentials or null if not available
   */
  public org.eclipse.jkube.kit.build.api.auth.AuthConfig getAuthConfigFromDefaultCredentialsProvider() {
    return delegate.getCredentialsFromDefaultCredentialsProvider();
  }
}
