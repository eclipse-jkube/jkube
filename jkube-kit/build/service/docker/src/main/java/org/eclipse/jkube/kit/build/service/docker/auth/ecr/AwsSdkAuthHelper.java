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

/**
 * Interface for AWS SDK authentication helpers.
 * Supports both AWS SDK v1 and v2 through reflection to avoid hard dependencies.
 */
public interface AwsSdkAuthHelper {

  /**
   * Check if AWS SDK is present in the classpath.
   *
   * @return true if AWS SDK is available, false otherwise
   */
  boolean isAwsSdkAvailable();

  /**
   * Get AWS SDK version.
   *
   * @return version string (e.g., "v1", "v2")
   */
  String getSdkVersion();

  /**
   * Get AWS Access Key ID from environment variable.
   *
   * @return AWS Access Key ID or null
   */
  String getAwsAccessKeyIdEnvVar();

  /**
   * Get AWS Secret Access Key from environment variable.
   *
   * @return AWS Secret Access Key or null
   */
  String getAwsSecretAccessKeyEnvVar();

  /**
   * Get AWS Session Token from environment variable.
   *
   * @return AWS Session Token or null
   */
  String getAwsSessionTokenEnvVar();

  /**
   * Get AWS Container Credentials Relative URI from environment variable.
   *
   * @return relative URI or null
   */
  String getAwsContainerCredentialsRelativeUri();

  /**
   * Get ECS Metadata Endpoint.
   * If the ECS_METADATA_ENDPOINT environment variable is not set,
   * returns the default ECS metadata endpoint v2 (http://169.254.170.2).
   *
   * @return ECS metadata endpoint URL
   * @see <a href="https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-metadata-endpoint-v2.html">ECS Task Metadata Endpoint v2</a>
   */
  String getEcsMetadataEndpoint();

  /**
   * Get AWS credentials using default credentials provider chain.
   *
   * @return AuthConfig with credentials or null if not available
   */
  AuthConfig getCredentialsFromDefaultCredentialsProvider();
}
