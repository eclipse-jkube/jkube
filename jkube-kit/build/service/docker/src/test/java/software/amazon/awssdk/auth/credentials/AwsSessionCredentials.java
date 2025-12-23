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
 * Simulates software.amazon.awssdk.auth.credentials.AwsSessionCredentials interface.
 */
package software.amazon.awssdk.auth.credentials;

public interface AwsSessionCredentials extends AwsCredentials {
  String sessionToken();
}