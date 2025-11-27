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
import java.lang.reflect.Method;

/**
 * AWS SDK v2 authentication helper.
 * Uses reflection to avoid hard dependency on AWS SDK v2.
 */
public class AwsSdkHelperV2 extends AbstractAwsSdkHelper {
  // AWS SDK v2 class names
  private static final String DEFAULT_CREDENTIALS_PROVIDER = "software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider";
  private static final String AWS_SESSION_CREDENTIALS = "software.amazon.awssdk.auth.credentials.AwsSessionCredentials";
  private static final String AWS_CREDENTIALS = "software.amazon.awssdk.auth.credentials.AwsCredentials";

  @Override
  public boolean isAwsSdkAvailable() {
    try {
      Class.forName(DEFAULT_CREDENTIALS_PROVIDER);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public String getSdkVersion() {
    return "v2";
  }

  @Override
  public AuthConfig getCredentialsFromDefaultCredentialsProvider() {
    try {
      Object credentials = getCredentialsFromDefaultProvider();
      if (credentials == null) {
        return null;
      }

      return AuthConfig.builder()
          .username(getAccessKeyIdFromCredentials(credentials))
          .password(getSecretAccessKeyFromCredentials(credentials))
          .email("none")
          .auth(getSessionTokenFromCredentials(credentials))
          .build();
    } catch (Exception e) {
      // Return null if credentials cannot be retrieved
      return null;
    }
  }

  /**
   * Get credentials from AWS SDK v2 DefaultCredentialsProvider.
   * Uses reflection to call:
   * DefaultCredentialsProvider.create().resolveCredentials()
   */
  Object getCredentialsFromDefaultProvider() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> providerClass = Class.forName(DEFAULT_CREDENTIALS_PROVIDER);

    // Call DefaultCredentialsProvider.create()
    Method createMethod = providerClass.getMethod("create");
    Object provider = createMethod.invoke(null);

    // Call provider.resolveCredentials()
    Method resolveMethod = provider.getClass().getMethod("resolveCredentials");
    return resolveMethod.invoke(provider);
  }

  /**
   * Get session token from AWS SDK v2 credentials if they are session credentials.
   * Returns null if credentials don't have a session token.
   */
  String getSessionTokenFromCredentials(Object credentials) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> sessionCredentialsClass = Class.forName(AWS_SESSION_CREDENTIALS);
    if (sessionCredentialsClass.isInstance(credentials)) {
      Method sessionTokenMethod = sessionCredentialsClass.getMethod("sessionToken");
      return (String) sessionTokenMethod.invoke(credentials);
    }
    return null;
  }

  /**
   * Get access key ID from AWS SDK v2 credentials.
   * Calls credentials.accessKeyId()
   */
  String getAccessKeyIdFromCredentials(Object credentials) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> credentialsClass = Class.forName(AWS_CREDENTIALS);
    Method accessKeyIdMethod = credentialsClass.getMethod("accessKeyId");
    return (String) accessKeyIdMethod.invoke(credentials);
  }

  /**
   * Get secret access key from AWS SDK v2 credentials.
   * Calls credentials.secretAccessKey()
   */
  String getSecretAccessKeyFromCredentials(Object credentials) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> credentialsClass = Class.forName(AWS_CREDENTIALS);
    Method secretAccessKeyMethod = credentialsClass.getMethod("secretAccessKey");
    return (String) secretAccessKeyMethod.invoke(credentials);
  }
}
