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
/*
 * Mock AWS SDK v2 class for testing purposes only.
 * Simulates software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider class.
 */
package software.amazon.awssdk.auth.credentials;

/**
 * Mock implementation of AWS SDK v2 DefaultCredentialsProvider.
 * This is used for testing the reflection-based credential loading.
 */
public class DefaultCredentialsProvider {
  private final AwsCredentials credentials;

  private DefaultCredentialsProvider() {
    // Return test credentials based on environment variables
    String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
    String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
    String sessionToken = System.getenv("AWS_SESSION_TOKEN");

    if (accessKey != null && secretKey != null) {
      if (sessionToken != null) {
        this.credentials = AwsSessionCredentialsImpl.create(accessKey, secretKey, sessionToken);
      } else {
        this.credentials = AwsBasicCredentials.create(accessKey, secretKey);
      }
    } else {
      this.credentials = null;
    }
  }

  public static DefaultCredentialsProvider create() {
    return new DefaultCredentialsProvider();
  }

  public AwsCredentials resolveCredentials() {
    if (credentials == null) {
      throw new RuntimeException("Unable to load AWS credentials");
    }
    return credentials;
  }
}