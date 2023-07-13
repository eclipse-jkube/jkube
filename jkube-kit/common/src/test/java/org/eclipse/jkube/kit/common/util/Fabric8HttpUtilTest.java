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
package org.eclipse.jkube.kit.common.util;

import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.http.TestHttpResponse;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

class Fabric8HttpUtilTest {

  @Test
  void toFormData_whenDataProvidedAsMap_thenCreateFormDataPayload() throws UnsupportedEncodingException {
    // Given
    Map<String, String> formDataMap = new HashMap<>();
    formDataMap.put("grant_type", "password");
    formDataMap.put("refresh_token", "secret");
    formDataMap.put("service", "auth.example.com");
    formDataMap.put("scope", "repository=myuser/test-chart:pull");
    formDataMap.put("client id", "Eclipse&JKube");
    formDataMap.put("username", "?myuser");
    formDataMap.put("password", "secret");

    // When
    String formDataPayload = Fabric8HttpUtil.toFormData(formDataMap);

    // Then
    assertThat(formDataPayload)
        .isEqualTo("client+id=Eclipse%26JKube&refresh_token=secret&password=secret&grant_type=password&service=auth.example.com&scope=repository%3Dmyuser%2Ftest-chart%3Apull&username=%3Fmyuser");
  }

  @Test
  void extractAuthenticationChallengeIntoMap_whenWwwHeaderProvided_thenParseDataIntoMap() {
    // Given
    String wwwAuthenticateValue = "Bearer realm=\"https://auth.example.com/token\",service=\"registry.example.com\",scope=\"repository:myuser/test-chart:pull\"";
    Map<String, List<String>> responseHeaders = new HashMap<>();
    responseHeaders.put("WWW-Authenticate", Collections.singletonList(wwwAuthenticateValue));
    HttpResponse<byte[]> response = new TestHttpResponse<byte[]>(responseHeaders).withCode(HTTP_OK);

    // When
    Map<String, String> wwwAuthenticateAsMap = Fabric8HttpUtil.extractAuthenticationChallengeIntoMap(response);

    // Then
    assertThat(wwwAuthenticateAsMap)
        .hasSize(3)
        .containsEntry("Bearer realm", "https://auth.example.com/token")
        .containsEntry("service", "registry.example.com")
        .containsEntry("scope", "repository:myuser/test-chart:pull");
  }
}
