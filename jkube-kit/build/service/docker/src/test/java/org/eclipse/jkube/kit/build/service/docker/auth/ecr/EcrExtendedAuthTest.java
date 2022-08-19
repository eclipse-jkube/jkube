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

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.common.KitLogger;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test exchange of local stored credentials for temporary ecr credentials
 *
 * @author chas
 * @since 2016-12-21
 */
class EcrExtendedAuthTest {

    @Mocked
    private KitLogger logger;

    @Test
    void testIsNotAws() {
        assertThat(new EcrExtendedAuth(logger, "jolokia").isAwsRegistry()).isFalse();
    }

    @Test
    void testIsAws() {
        assertThat(new EcrExtendedAuth(logger, "123456789012.dkr.ecr.eu-west-1.amazonaws.com").isAwsRegistry()).isTrue();
    }

    @Test
    void testHeaders() {
        EcrExtendedAuth eea = new EcrExtendedAuth(logger, "123456789012.dkr.ecr.eu-west-1.amazonaws.com");
        AuthConfig localCredentials = AuthConfig.builder()
                .username("username")
                .password("password")
                .build();
        Date signingTime = Date.from(
            ZonedDateTime.of(2016, 12, 17, 21, 10, 58, 0, ZoneId.of("GMT"))
                .toInstant());
        HttpPost request = eea.createSignedRequest(localCredentials, signingTime);
        assertThat(request)
                .returns("api.ecr.eu-west-1.amazonaws.com", r -> r.getFirstHeader("host").getValue())
                .returns("20161217T211058Z", r -> r.getFirstHeader("X-Amz-Date").getValue())
                .returns("AWS4-HMAC-SHA256 Credential=username/20161217/eu-west-1/ecr/aws4_request, SignedHeaders=content-type;host;x-amz-target, Signature=2ae11d499499cc951900aac0fbec96009382ba4f735bd14baa375c3e51d50aa9", r -> r.getFirstHeader("Authorization").getValue());
    }

    @Test
    void testClientClosedAndCredentialsDecoded(@Mocked final CloseableHttpClient closeableHttpClient,
            @Mocked final CloseableHttpResponse closeableHttpResponse,
            @Mocked final StatusLine statusLine)
        throws IOException, IllegalStateException {

        final HttpEntity entity = new StringEntity("{\"authorizationData\": [{"
                                                   + "\"authorizationToken\": \"QVdTOnBhc3N3b3Jk\","
                                                   + "\"expiresAt\": 1448878779.809,"
                                                   + "\"proxyEndpoint\": \"https://012345678910.dkr.ecr.eu-west-1.amazonaws.com\"}]}");

        new Expectations() {{
            statusLine.getStatusCode(); result = 200;
            closeableHttpResponse.getEntity(); result = entity;
        }};
        EcrExtendedAuth eea = new EcrExtendedAuth(logger, "123456789012.dkr.ecr.eu-west-1.amazonaws.com") {
            CloseableHttpClient createClient() {
                return closeableHttpClient;
            }
        };

        AuthConfig localCredentials = AuthConfig.builder()
                .username("username")
                .password("password")
                .build();
        AuthConfig awsCredentials = eea.extendedAuth(localCredentials);
        assertThat(awsCredentials)
                .hasFieldOrPropertyWithValue("username", "AWS")
                .hasFieldOrPropertyWithValue("password", "password");

        new Verifications() {{
             closeableHttpClient.close();
         }};
    }

}
