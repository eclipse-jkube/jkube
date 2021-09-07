/**
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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.common.KitLogger;

public class AwsSdkAuthConfigFactory {

    private final KitLogger log;
    private AwsSdkHelper awsSdkHelper;

    public AwsSdkAuthConfigFactory(KitLogger log, AwsSdkHelper awsSdkHelper) {
        this.log = log;
        this.awsSdkHelper = awsSdkHelper;
    }

    public AuthConfig createAuthConfig() {
        try {
            Object credentials = awsSdkHelper.getCredentialsFromDefaultAWSCredentialsProviderChain();
            if (credentials == null) {
                return null;
            }

            return AuthConfig.builder()
                    .username(awsSdkHelper.getAWSAccessKeyIdFromCredentials(credentials))
                    .password(awsSdkHelper.getAwsSecretKeyFromCredentials(credentials))
                    .email("none")
                    .auth(awsSdkHelper.getSessionTokenFromCrendentials(credentials))
                    .build();
        } catch (Exception t) {
            String issueTitle = null;
            try {
                issueTitle = URLEncoder.encode("Failed calling AWS SDK: " + t.getMessage(), UTF_8.name());
            } catch (UnsupportedEncodingException ignore) {
            }
            log.warn("Failed to fetch AWS credentials: %s", t.getMessage());
            if (t.getCause() != null) {
                log.warn("Caused by: %s", t.getCause().getMessage());
            }
            log.warn("Please report a bug at https://github.com/eclipse/jkube/issues/new?%s",
                    issueTitle == null ? "" : "title=?" + issueTitle);
            log.warn("%s", t);
            return null;
        }
    }

}
