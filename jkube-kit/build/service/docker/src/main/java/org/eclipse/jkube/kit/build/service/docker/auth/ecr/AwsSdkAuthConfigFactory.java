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
import org.eclipse.jkube.kit.common.KitLogger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Factory for creating AWS authentication configuration using AWS SDK.
 * Supports both AWS SDK v1 and v2 through the AwsSdkHelper abstraction.
 */
public class AwsSdkAuthConfigFactory {
  private final KitLogger log;
  private final AwsSdkHelper awsSdkHelper;

  public AwsSdkAuthConfigFactory(KitLogger log, AwsSdkHelper awsSdkHelper) {
    this.log = log;
    this.awsSdkHelper = awsSdkHelper;
  }

  /**
   * Create authentication configuration from AWS SDK default credentials provider.
   * Automatically works with both AWS SDK v1 and v2.
   *
   * @return AuthConfig with AWS credentials or null if credentials cannot be retrieved
   */
  public AuthConfig createAuthConfig() {
    try {
      log.debug("Attempting to get AWS credentials from SDK %s", awsSdkHelper.getSdkVersion());
      AuthConfig authConfig = awsSdkHelper.getCredentialsFromDefaultCredentialsProvider();

      if (authConfig == null) {
        log.debug("No AWS credentials found from SDK default credentials provider");
        return null;
      }

      log.debug("Successfully retrieved AWS credentials from SDK %s", awsSdkHelper.getSdkVersion());
      return authConfig;
    } catch (Exception t) {
      String issueTitle = null;
      try {
        issueTitle = URLEncoder.encode("Failed calling AWS SDK: " + t.getMessage(), UTF_8.name());
      } catch (UnsupportedEncodingException ignore) {
      }
      log.warn("Failed to fetch AWS credentials using SDK %s: %s", awsSdkHelper.getSdkVersion(), t.getMessage());
      if (t.getCause() != null) {
        log.warn("Caused by: %s", t.getCause().getMessage());
      }
      log.warn("Please report a bug at https://github.com/eclipse-jkube/jkube/issues/new?%s",
          issueTitle == null ? "" : "title=?" + issueTitle);
      log.debug("Exception details: %s", t);
      return null;
    }
  }

}
