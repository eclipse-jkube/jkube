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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.http.TestHttpResponse;
import org.junit.jupiter.api.Test;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

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
  void extractAuthenticationChallengeIntoMap_whenEmptyHeaderProvided_thenReturnEmptyMap() throws IOException {
    // Given
    Map<String, List<String>> responseHeaders = new HashMap<>();
    responseHeaders.put("WWW-Authenticate", Collections.singletonList(""));
    HttpResponse<byte[]> response = new TestHttpResponse<byte[]>(responseHeaders).withCode(HTTP_OK);
    // When
    List<Map<String, String>> result = Fabric8HttpUtil.extractAuthenticationChallengeIntoMap(response);
    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void extractAuthenticationChallengeIntoMap_whenBasicAuthenticationRealm_thenParseDataIntoMap() throws IOException {
    // Given
    Map<String, List<String>> responseHeaders = new HashMap<>();
    responseHeaders.put("WWW-Authenticate", Collections.singletonList("Basic realm=\"Access to the staging site\", charset=\"UTF-8\""));
    HttpResponse<byte[]> response = new TestHttpResponse<byte[]>(responseHeaders).withCode(HTTP_OK);
    // When
    List<Map<String, String>> result = Fabric8HttpUtil.extractAuthenticationChallengeIntoMap(response);
    // Then
    assertThat(result)
        .singleElement(MAP)
        .hasSize(3)
        .containsEntry("scheme", "Basic")
        .containsEntry("realm", "Access to the staging site")
        .containsEntry("charset", "UTF-8");
  }

  @Test
  void extractAuthenticationChallengeIntoMap_whenMultipleChallenges_thenParseDataIntoMap() throws IOException {
    // Given
    Map<String, List<String>> responseHeaders = new HashMap<>();
    responseHeaders.put("WWW-Authenticate", Collections.singletonList("Newauth realm=\"apps\", type=1" +
        ", title=\"Login to \\\"apps\\\"\"" +
        ", Basic realm=\"simple\""));
    HttpResponse<byte[]> response = new TestHttpResponse<byte[]>(responseHeaders).withCode(HTTP_OK);
    // When
    List<Map<String, String>> result = Fabric8HttpUtil.extractAuthenticationChallengeIntoMap(response);
    // Then
    assertThat(result)
        .hasSize(2)
        .satisfies(r -> assertThat(r.get(0))
            .containsEntry("scheme", "Newauth")
            .containsEntry("realm", "apps")
            .containsEntry("type", "1")
            .containsEntry("title", "Login to \"apps\""))
        .satisfies(r -> assertThat(r.get(1))
            .containsEntry("scheme", "Basic")
            .containsEntry("realm", "simple"));
  }

  @Test
  void extractAuthenticationChallengeIntoMap_whenDigestAuthenticationChallenge_thenParseDataIntoMap() throws IOException {
    // Given
    Map<String, List<String>> responseHeaders = new HashMap<>();
    responseHeaders.put("WWW-Authenticate", Collections.singletonList("Digest realm=\"http-auth@example.org\"," +
        "    qop=\"auth, auth-int\"," +
        "    algorithm=SHA-256," +
        "    nonce=\"MTQ0NDMyOTA1OTowOTcwOTBhMmU4MGM4OTEyZGY2OGY5NzIyZjMyZTM0MA==\"," +
        "    opaque=\"FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS\""));
    HttpResponse<byte[]> response = new TestHttpResponse<byte[]>(responseHeaders).withCode(HTTP_OK);
    // When
    List<Map<String, String>> result = Fabric8HttpUtil.extractAuthenticationChallengeIntoMap(response);
    // Then
    assertThat(result)
        .singleElement(MAP)
        .hasSize(6)
        .containsEntry("scheme", "Digest")
        .containsEntry("realm", "http-auth@example.org")
        .containsEntry("algorithm", "SHA-256")
        .containsEntry("nonce", "MTQ0NDMyOTA1OTowOTcwOTBhMmU4MGM4OTEyZGY2OGY5NzIyZjMyZTM0MA==")
        .containsEntry("opaque", "FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS")
        .containsEntry("qop", "auth, auth-int");
  }

  @Test
  void extractAuthenticationChallengeIntoMap_whenHOBAAuthenticationChallenge_thenParseDataIntoMap() throws IOException {
    // Given
    Map<String, List<String>> responseHeaders = new HashMap<>();
    responseHeaders.put("WWW-Authenticate", Collections.singletonList("HOBA max-age=\"180\"," +
        " challenge=\"16:MTEyMzEyMzEyMw==1:028:https://www.example.com:80800:3:MTI48:NjgxNDdjOTctNDYxYi00MzEwLWJlOWItNGM3MDcyMzdhYjUz\""));
    HttpResponse<byte[]> response = new TestHttpResponse<byte[]>(responseHeaders).withCode(HTTP_OK);
    // When
    List<Map<String, String>> result = Fabric8HttpUtil.extractAuthenticationChallengeIntoMap(response);
    // Then
    assertThat(result)
        .singleElement(MAP)
        .hasSize(3)
        .containsEntry("scheme", "HOBA")
        .containsEntry("max-age", "180")
        .containsEntry("challenge", "16:MTEyMzEyMzEyMw==1:028:https://www.example.com:80800:3:MTI48:NjgxNDdjOTctNDYxYi00MzEwLWJlOWItNGM3MDcyMzdhYjUz");
  }

  @Test
  void extractAuthenticationChallengeIntoMap_whenWwwHeaderProvided_thenParseDataIntoMap() throws IOException {
    // Given
    String wwwAuthenticateValue = "Bearer realm=\"https://auth.example.com/token\",service=\"registry.example.com\",scope=\"repository:myuser/test-chart:pull\"";
    Map<String, List<String>> responseHeaders = new HashMap<>();
    responseHeaders.put("WWW-Authenticate", Collections.singletonList(wwwAuthenticateValue));
    HttpResponse<byte[]> response = new TestHttpResponse<byte[]>(responseHeaders).withCode(HTTP_OK);

    // When
    List<Map<String, String>> wwwAuthenticateAsMap = Fabric8HttpUtil.extractAuthenticationChallengeIntoMap(response);

    // Then
    assertThat(wwwAuthenticateAsMap)
        .singleElement(MAP)
        .hasSize(4)
        .containsEntry("scheme", "Bearer")
        .containsEntry("realm", "https://auth.example.com/token")
        .containsEntry("service", "registry.example.com")
        .containsEntry("scope", "repository:myuser/test-chart:pull");
  }

  @Test
  void extractAuthenticationChallengeIntoMap_whenWwwHeaderContainsPushScope_thenParseDataIntoMap() throws IOException {
    // Given
    String wwwAuthenticateValue = "Bearer realm=\"https://auth.example.com/token\",service=\"registry.example.com\"," +
        "scope=\"repository:myuser/test-chart:pull,push\",error=\"insufficient_scope\"";
    Map<String, List<String>> responseHeaders = new HashMap<>();
    responseHeaders.put("WWW-Authenticate", Collections.singletonList(wwwAuthenticateValue));
    HttpResponse<byte[]> response = new TestHttpResponse<byte[]>(responseHeaders).withCode(HTTP_OK);

    // When
    List<Map<String, String>> wwwAuthenticateAsMap = Fabric8HttpUtil.extractAuthenticationChallengeIntoMap(response);

    // Then
    assertThat(wwwAuthenticateAsMap)
        .singleElement(MAP)
        .hasSize(5)
        .containsEntry("scheme", "Bearer")
        .containsEntry("realm", "https://auth.example.com/token")
        .containsEntry("service", "registry.example.com")
        .containsEntry("scope", "repository:myuser/test-chart:pull,push")
        .containsEntry("error", "insufficient_scope");
  }
}
