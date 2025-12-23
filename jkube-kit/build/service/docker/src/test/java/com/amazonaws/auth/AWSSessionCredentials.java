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
 * Mock AWS SDK v1 class for testing purposes only.
 * Simulates com.amazonaws.auth.AWSSessionCredentials interface.
 */
package com.amazonaws.auth;

public interface AWSSessionCredentials extends AWSCredentials {
  String getSessionToken();
}