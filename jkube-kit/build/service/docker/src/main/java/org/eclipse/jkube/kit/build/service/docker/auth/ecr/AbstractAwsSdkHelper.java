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
 * Abstract base class for AWS SDK helpers.
 * Contains common functionality shared between AWS SDK v1 and v2 helpers.
 */
abstract class AbstractAwsSdkHelper implements AwsSdkAuthHelper {
  protected static final String ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID";
  protected static final String SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY";
  protected static final String SESSION_TOKEN = "AWS_SESSION_TOKEN";
  protected static final String CONTAINER_CREDENTIALS_RELATIVE_URI = "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI";
  protected static final String METADATA_ENDPOINT = "ECS_METADATA_ENDPOINT";
  protected static final String AWS_INSTANCE_LINK_LOCAL_ADDRESS = "http://169.254.170.2";

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
}
