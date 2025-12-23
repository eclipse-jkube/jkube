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
 * Simulates AWS SDK v2 basic credentials.
 */
package software.amazon.awssdk.auth.credentials;

public class AwsBasicCredentials implements AwsCredentials {
  private final String accessKeyId;
  private final String secretAccessKey;

  public AwsBasicCredentials(String accessKeyId, String secretAccessKey) {
    this.accessKeyId = accessKeyId;
    this.secretAccessKey = secretAccessKey;
  }

  @Override
  public String accessKeyId() {
    return accessKeyId;
  }

  @Override
  public String secretAccessKey() {
    return secretAccessKey;
  }

  public static AwsBasicCredentials create(String accessKeyId, String secretAccessKey) {
    return new AwsBasicCredentials(accessKeyId, secretAccessKey);
  }
}