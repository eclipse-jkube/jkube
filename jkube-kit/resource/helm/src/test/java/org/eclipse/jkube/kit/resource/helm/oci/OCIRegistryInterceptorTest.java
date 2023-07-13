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
package org.eclipse.jkube.kit.resource.helm.oci;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.fabric8.kubernetes.client.http.HttpClient;
import org.eclipse.jkube.kit.resource.helm.HelmRepository;

import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.http.TestHttpResponse;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import io.fabric8.mockwebserver.DefaultMockServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class OCIRegistryInterceptorTest {
  private OCIRegistryInterceptor ociRegistryInterceptor;
  private DefaultMockServer server;
  private String authUrl;
  private HttpClient.Factory httpClientFactory;
  private HelmRepository helmRepository;

  @BeforeEach
  void setUp() {
    server = new DefaultMockServer();
    server.start();
    httpClientFactory = HttpClientUtils.getHttpClientFactory();
    helmRepository = HelmRepository.builder()
        .username("myuser")
        .password("secret")
        .build();
    ociRegistryInterceptor = new OCIRegistryInterceptor(httpClientFactory, helmRepository);
    authUrl = String.format("http://%s:%d/token", server.getHostName(), server.getPort());
  }

  @AfterEach
  void tearDown() {
    server.shutdown();
  }

  @Test
  void before_whenAccessTokenSetAndResponseDoesNotContainAuthorizationHeader_thenAddHeader() {
    // Given
    HttpRequest.Builder builder = mock(HttpRequest.Builder.class, Mockito.RETURNS_SELF);
    ociRegistryInterceptor = new OCIRegistryInterceptor(httpClientFactory, helmRepository, "some-access-token");

    // When
    ociRegistryInterceptor.before(builder, null, null);

    // Then
    verify(builder).setHeader("Authorization", "Bearer some-access-token");
  }

  @Test
  void afterFailure_whenResponseCodeNot401_thenReturnFalse() {
    // Given
    HttpRequest.Builder builder = mock(HttpRequest.Builder.class, Mockito.RETURNS_SELF);
    HttpResponse<byte[]> response = new TestHttpResponse<byte[]>().withCode(HTTP_NOT_FOUND);

    // When + Then
    assertThat(ociRegistryInterceptor.afterFailure(builder, response, null)).isCompletedWithValue(false);
  }

  @Test
  void afterFailure_whenResponseHasNoWwwHeader_thenThrowException() {
    // Given
    HttpRequest.Builder builder = mock(HttpRequest.Builder.class, Mockito.RETURNS_SELF);
    HttpResponse<byte[]> response = new TestHttpResponse<byte[]>().withCode(HTTP_UNAUTHORIZED);

    // When
    assertThatIllegalStateException()
        .isThrownBy(() -> ociRegistryInterceptor.afterFailure(builder, response, null))
        .withMessage("Got 401 but no WWW-Authenticate found in response headers ");
  }

  @Test
  void afterFailure_whenAuthCallFails_thenReturnFalse() {
    server.expect().get()
        .withPath("/token?service=localhost&scope=repository:myuser/test-chart:pull,push")
        .andReturn(HTTP_UNAUTHORIZED, "{\"message\":\"unauthorized\"}")
        .once();
    String service = "localhost";
    String wwwHeader = createWwwHeader(authUrl, service);
    Map<String, List<String>> unAuthorizedResponseHeaders = Collections.singletonMap(HttpHeaders.WWW_AUTHENTICATE, Collections.singletonList(wwwHeader));
    HttpRequest.Builder builder = mock(HttpRequest.Builder.class, Mockito.RETURNS_SELF);
    HttpResponse<byte[]> response = new TestHttpResponse<byte[]>(unAuthorizedResponseHeaders).withCode(HTTP_UNAUTHORIZED);

    // When
    CompletableFuture<Boolean> result = ociRegistryInterceptor.afterFailure(builder, response, null);

    // Then
    assertThat(result).isCompletedWithValue(false);
    verify(builder, times(0)).setHeader(anyString(), anyString());
  }

  @Test
  void afterFailure_whenUnauthenticated_thenShouldAuthenticateWithGetAndFetchAccessToken() throws IOException {
    server.expect().get()
        .withPath("/token?service=localhost&scope=repository:myuser/test-chart:pull,push")
        .andReturn(HTTP_OK, "{\"token\":\"mytoken\"}")
        .once();
    String service = "localhost";
    String wwwHeader = createWwwHeader(authUrl, service);
    Map<String, List<String>> unAuthorizedResponseHeaders = Collections.singletonMap(HttpHeaders.WWW_AUTHENTICATE, Collections.singletonList(wwwHeader));
    HttpRequest.Builder builder = mock(HttpRequest.Builder.class, Mockito.RETURNS_SELF);
    HttpResponse<byte[]> response = new TestHttpResponse<byte[]>(unAuthorizedResponseHeaders).withCode(HTTP_UNAUTHORIZED);

    // When
    CompletableFuture<Boolean> result = ociRegistryInterceptor.afterFailure(builder, response, null);

    // Then
    assertThat(result).isCompletedWithValue(true);
    verify(builder).setHeader("Authorization", "Bearer mytoken");
  }

  @Test
  void afterFailure_whenGetNotAllowedAndPostAlsoFails_thenReturnFalse() {
    server.expect().get()
        .withPath("/token?service=localhost&scope=repository:myuser/test-chart:pull,push")
        .andReturn(HTTP_BAD_METHOD, "")
        .once();
    server.expect().post()
        .withPath("/token")
        .andReturn(HTTP_OK, "{\"message\":\"unauthorized\"}")
        .once();
    String service = "localhost";
    String wwwHeader = createWwwHeader(authUrl, service);
    Map<String, List<String>> unAuthorizedResponseHeaders = Collections.singletonMap(HttpHeaders.WWW_AUTHENTICATE, Collections.singletonList(wwwHeader));
    HttpRequest.Builder builder = mock(HttpRequest.Builder.class, Mockito.RETURNS_SELF);
    HttpResponse<byte[]> response = new TestHttpResponse<byte[]>(unAuthorizedResponseHeaders).withCode(HTTP_UNAUTHORIZED);

    // When
    CompletableFuture<Boolean> result = ociRegistryInterceptor.afterFailure(builder, response, null);

    // Then
    assertThat(result).isCompletedWithValue(false);
    verify(builder, times(0)).setHeader(anyString(), anyString());
  }

  @Test
  void afterFailure_whenGetNotAllowed_thenShouldAuthenticateWithPostAndFetchAccessToken() throws InterruptedException {
    server.expect().get()
        .withPath("/token?service=localhost&scope=repository:myuser/test-chart:pull,push")
        .andReturn(HTTP_BAD_METHOD, "")
        .once();
    server.expect().post()
        .withPath("/token")
        .andReturn(HTTP_OK, "{\"token\":\"mytoken\"}")
        .once();
    String service = "localhost";
    String wwwHeader = createWwwHeader(authUrl, service);
    Map<String, List<String>> unAuthorizedResponseHeaders = Collections.singletonMap(HttpHeaders.WWW_AUTHENTICATE, Collections.singletonList(wwwHeader));
    HttpRequest.Builder builder = mock(HttpRequest.Builder.class, Mockito.RETURNS_SELF);
    HttpResponse<byte[]> response = new TestHttpResponse<byte[]>(unAuthorizedResponseHeaders).withCode(HTTP_UNAUTHORIZED);

    // When
    CompletableFuture<Boolean> result = ociRegistryInterceptor.afterFailure(builder, response, null);

    // Then
    assertThat(result).isCompletedWithValue(true);
    verify(builder).setHeader("Authorization", "Bearer mytoken");
    RecordedRequest request = server.getLastRequest();
    assertThat(request.getBody().readUtf8())
        .isEqualTo("refresh_token=secret&password=secret&grant_type=password&service=localhost&scope=repository%3Amyuser%2Ftest-chart%3Apull%2Cpush&client_id=EclipseJKube&username=myuser");
  }

  private String createWwwHeader(String authUrl, String service) {
    return String.format("Bearer realm=\"%s\",service=\"%s\",scope=\"repository:%s/%s:pull\"", authUrl, service, "myuser", "test-chart");
  }
}
