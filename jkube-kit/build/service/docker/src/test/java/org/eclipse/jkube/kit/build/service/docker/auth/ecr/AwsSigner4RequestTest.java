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

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test aws request signing
 *
 * @author chas
 * @since 2016-12-21
 */
class AwsSigner4RequestTest {

    private static final String TASK1 = "POST\n"
                                        + "/\n"
                                        + "\n"
                                        + "content-type:application/x-amz-json-1.1\n"
                                        + "host:ecr.us-east-1.amazonaws.com\n"
                                        + "x-amz-target:AmazonEC2ContainerRegistry_V20150921.GetAuthorizationToken\n"
                                        + "\n"
                                        + "content-type;host;x-amz-target\n"
                                        + "1531fbe1f1c4e3437223a1583a6d21d404acf9d910262f629d73d6bf55a545bd";

    private static final String TASK2 = "AWS4-HMAC-SHA256\n"
                                        + "20150830T123600Z\n"
                                        + "20150830/us-east-1/service/aws4_request\n"
                                        + "7c945a283983ca83301de46103427b7035344a4b0cfddd886e3d94b4bed7df5e";

    private static final String TASK3 = "89cd649587898a1913ced5c519425905b192c4662212d37e689e6c20e53edbbd";

    private static final String TASK4 = "AWS4-HMAC-SHA256 "
                                        + "Credential=AKIDEXAMPLE/20150830/us-east-1/service/aws4_request, "
                                        + "SignedHeaders=content-type;host;x-amz-target, "
                                        + "Signature=89cd649587898a1913ced5c519425905b192c4662212d37e689e6c20e53edbbd";

    @Test
    void testSign() {
        HttpPost request = new HttpPost("https://ecr.us-east-1.amazonaws.com/");
        request.setHeader("host", "ecr.us-east-1.amazonaws.com");
        request.setHeader("Content-Type", "application/x-amz-json-1.1");
        request.setHeader("X-Amz-Target", "AmazonEC2ContainerRegistry_V20150921.GetAuthorizationToken");
        request.setEntity(new StringEntity("{\"registryIds\":[\"012345678901\"]}", StandardCharsets.UTF_8));

        AwsSigner4 signer = new AwsSigner4("us-east-1", "ecr");

        Date signingTime = Date.from(
            ZonedDateTime.of(2015, 8,30, 12, 36, 0, 0, ZoneId.of("GMT"))
        .toInstant());
        AwsSigner4Request sr = new AwsSigner4Request("us-east-1", "service", request, signingTime);
        AuthConfig credentials = AuthConfig.builder()
                .username("AKIDEXAMPLE")
                .password("wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY")
                .build();

        assertThat(signer)
                .returns(TASK1, t -> t.task1(sr))
                .returns(TASK2, t -> t.task2(sr));

        StringBuilder dst = new StringBuilder();
        AwsSigner4.hexEncode(dst, signer.task3(sr, credentials));
        assertThat(dst).hasToString(TASK3);

        assertThat(signer.task4(sr, credentials)).isEqualTo(TASK4);
    }

    @Test
    void includesAuthTokenAsAwsSecurityToken() {
        HttpUriRequest request = newGet("https://someService.us-east-1.amazonaws.com/");
        request.setHeader("host", request.getURI().getHost());
        String awsSecurityToken = "securityToken";
        AuthConfig credentials = AuthConfig.builder()
                .username("awsAccessKeyId")
                .password( "awsSecretAccessKey")
                .auth(awsSecurityToken)
                .build();

        AwsSigner4 signer = new AwsSigner4("us-east-1", "someService");
        signer.sign(request, credentials, new Date());

        assertThat(awsSecurityToken).isEqualTo(request.getFirstHeader("X-Amz-Security-Token").getValue());
    }

    private HttpUriRequest newGet(String url) {
        HttpUriRequest get = new HttpGet(url);
        get.addHeader("Accept", "*/*");
        return get;
    }

}
