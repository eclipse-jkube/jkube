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

/**
 * Factory for AWS SDK helpers that supports both AWS SDK v1 and v2.
 * Automatically detects which SDK version is available on the classpath.
 * Maintains backward compatibility with the original AwsSdkHelper API.
 */
public class AwsSdkHelper implements AwsSdkAuthHelper {
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

  @Override
  public boolean isAwsSdkAvailable() {
    return delegate.isAwsSdkAvailable();
  }

  @Override
  public String getSdkVersion() {
    return delegate.getSdkVersion();
  }

  @Override
  public String getAwsAccessKeyIdEnvVar() {
    return delegate.getAwsAccessKeyIdEnvVar();
  }

  @Override
  public String getAwsSecretAccessKeyEnvVar() {
    return delegate.getAwsSecretAccessKeyEnvVar();
  }

  @Override
  public String getAwsSessionTokenEnvVar() {
    return delegate.getAwsSessionTokenEnvVar();
  }

  @Override
  public String getAwsContainerCredentialsRelativeUri() {
    return delegate.getAwsContainerCredentialsRelativeUri();
  }

  @Override
  public String getEcsMetadataEndpoint() {
    return delegate.getEcsMetadataEndpoint();
  }

  @Override
  public org.eclipse.jkube.kit.build.api.auth.AuthConfig getCredentialsFromDefaultCredentialsProvider() {
    return delegate.getCredentialsFromDefaultCredentialsProvider();
  }
}
