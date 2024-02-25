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
package org.eclipse.jkube.kit.resource.helm.oci;

import io.fabric8.kubernetes.client.http.BasicBuilder;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.http.Interceptor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.util.Base64Util;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.resource.helm.HelmRepository;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.eclipse.jkube.kit.common.util.AsyncUtil.get;
import static org.eclipse.jkube.kit.common.util.Fabric8HttpUtil.extractAuthenticationChallengeIntoMap;
import static org.eclipse.jkube.kit.common.util.Fabric8HttpUtil.toFormData;

public class OCIRegistryInterceptor implements Interceptor {
  private static final String TOKEN_KEY = "token";
  private static final String ACCESS_TOKEN_KEY = "access_token";
  private static final long OCI_REGISTRY_AUTH_REQUEST_TIMEOUT_MINUTES = 1;
  private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
  public static final String NAME = "OCI_TOKEN";
  private static final String AUTHORIZATION = "Authorization";
  private static final String BEARER = "Bearer ";

  private final HelmRepository repository;
  private final HttpClient httpClient;
  private String accessToken;

  public OCIRegistryInterceptor(HttpClient.Factory httpClientFactory, HelmRepository helmRepository) {
    this(httpClientFactory, helmRepository, null);
  }

  OCIRegistryInterceptor(HttpClient.Factory httpClientFactory, HelmRepository helmRepository, String token) {
    repository = helmRepository;
    httpClient = httpClientFactory.newBuilder().build();
    accessToken = token;
  }

  @Override
  public void before(BasicBuilder headerBuilder, HttpRequest request, RequestTags tags) {
    if (StringUtils.isNotBlank(accessToken)) {
      headerBuilder.setHeader(AUTHORIZATION, BEARER + accessToken);
    }
  }

  @Override
  public CompletableFuture<Boolean> afterFailure(BasicBuilder headerBuilder, HttpResponse<?> response, RequestTags tags) {
    if (response.code() == HTTP_UNAUTHORIZED) {
      if (StringUtils.isBlank(response.header(WWW_AUTHENTICATE))) {
        throw new IllegalStateException("Got 401 but no " + WWW_AUTHENTICATE + " found in response headers ");
      }
      return refreshToken(headerBuilder, response);
    }
    return CompletableFuture.completedFuture(false);
  }

  private CompletableFuture<Boolean> refreshToken(BasicBuilder headerBuilder, HttpResponse<?> response) {
    try {
      String updatedAccessToken = submitHttpRequestForAuthenticationChallenge(response);
      if (StringUtils.isNotBlank(updatedAccessToken)) {
        accessToken = updatedAccessToken;
        headerBuilder.setHeader(AUTHORIZATION, BEARER + accessToken);
        return CompletableFuture.completedFuture(true);
      }
      return CompletableFuture.completedFuture(false);
    } catch (IOException e) {
      throw new IllegalStateException("Failure while refreshing token from OCI registry: ", e);
    }
  }

  private String submitHttpRequestForAuthenticationChallenge(HttpResponse<?> response) throws IOException {
    Map<String, String> authChallengeHeader = extractAuthenticationChallengeIntoMap(response).stream()
        .filter(c -> c.get("scheme").equals("Bearer"))
        .findFirst()
        .orElse(Collections.emptyMap());
    String authenticationUrl = authChallengeHeader.get("realm");
    String scope = authChallengeHeader.get("scope");
    if (!scope.contains("push")) {
      scope += ",push";
    }
    String service = authChallengeHeader.get("service");

    return submitGetRequest(authenticationUrl, scope, service);
  }

  private String submitGetRequest(String url, String scope, String service) throws IOException {
    String authUrlWithQueryParams = String.format("%s?service=%s&scope=%s", url, service, scope);
    HttpRequest httpRequest = httpClient.newHttpRequestBuilder()
        .header(AUTHORIZATION, String.format("Basic %s", Base64Util.encodeToString(repository.getUsername() + ":" + repository.getPassword())))
        .uri(authUrlWithQueryParams)
        .build();
    HttpResponse<byte[]> response = get(httpClient.sendAsync(httpRequest, byte[].class), Duration.ofMinutes(OCI_REGISTRY_AUTH_REQUEST_TIMEOUT_MINUTES));

    int responseCode = response.code();
    if (responseCode == HttpURLConnection.HTTP_OK) {
      return parseAccessTokenFromResponse(new String(response.body()));
    } else if (responseCode == HttpURLConnection.HTTP_BAD_METHOD) { // DockerHub uses post for authentication
      return submitPostRequest(url, scope, service);
    }
    return null;
  }

  private String submitPostRequest(String url, String scope, String service) throws IOException {
    String postDataString = createPostFormDataForDockerAuth(scope, service);
    HttpRequest httpRequest = httpClient.newHttpRequestBuilder()
        .header("Content-Length", Integer.toString(postDataString.getBytes().length))
        .method("POST", "application/x-www-form-urlencoded", postDataString)
        .uri(url)
        .build();

    HttpResponse<byte[]> response = get(httpClient.sendAsync(httpRequest, byte[].class), Duration.ofMinutes(OCI_REGISTRY_AUTH_REQUEST_TIMEOUT_MINUTES));
    if (response.isSuccessful()) {
      return parseAccessTokenFromResponse(response.bodyString());
    }
    return null;
  }

  private String parseAccessTokenFromResponse(String responseBody) {
    Map<String, Object> responseBodyObj = Serialization.unmarshal(responseBody, Map.class);
    String tokenFound = null;
    if (responseBodyObj.containsKey(TOKEN_KEY)) {
      tokenFound = (String) responseBodyObj.get(TOKEN_KEY);
    }
    if (responseBodyObj.containsKey(ACCESS_TOKEN_KEY)) {
      tokenFound = (String) responseBodyObj.get(ACCESS_TOKEN_KEY);
    }

    if (StringUtils.isNotBlank(tokenFound)) {
      return tokenFound;
    }
    return null;
  }

  private String createPostFormDataForDockerAuth(String scope, String service) throws UnsupportedEncodingException {
    Map<String, String> postFormData = new HashMap<>();
    postFormData.put("grant_type", "password");
    postFormData.put("refresh_token", repository.getPassword());
    postFormData.put("service", service);
    postFormData.put("scope", scope);
    postFormData.put("client_id", "EclipseJKube");
    postFormData.put("username", repository.getUsername());
    postFormData.put("password", repository.getPassword());

    return toFormData(postFormData);
  }
}
