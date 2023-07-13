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

import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.kubernetes.client.utils.URLUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.resource.helm.BadUploadException;
import org.eclipse.jkube.kit.resource.helm.HelmRepository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Optional;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.eclipse.jkube.kit.common.util.AsyncUtil.get;

public class OCIRegistryClient {
  private static final String DOCKER_CONTENT_DIGEST = "Docker-Content-Digest";
  private static final String USER_AGENT = "EclipseJKube";
  private static final String OCI_IMAGE_MANIFEST_MEDIA_TYPE = "application/vnd.oci.image.manifest.v1+json";
  private static final String HELM_CONFIG_MEDIA_TYPE = "application/vnd.cncf.helm.config.v1+json";
  private static final String HELM_CHART_CONTENT_MEDIA_TYPE = "application/vnd.cncf.helm.chart.content.v1.tar+gzip";
  private static final String LOCATION_HEADER = "Location";
  private static final long OCI_UPLOAD_HTTP_REQUEST_TIMEOUT = 30;
  private final HelmRepository repository;
  private final OCIRegistryEndpoint ociRegistryEndpoint;
  private final HttpClient httpClient;

  public OCIRegistryClient(HelmRepository repository, HttpClient httpClient) {
    this.repository = repository;
    this.ociRegistryEndpoint = new OCIRegistryEndpoint(repository.getUrl());
    this.httpClient = httpClient;
  }

  public String getBaseUrl() throws MalformedURLException {
    return ociRegistryEndpoint.getBaseUrl();
  }

  public String initiateUploadProcess(String chartName) throws IOException {
    String uploadProcessInitiateUrl = ociRegistryEndpoint.getBlobUploadInitUrl(chartName);
    HttpRequest httpRequest = createBaseOCIHttpRequest()
        .post("application/json", EMPTY)
        .uri(uploadProcessInitiateUrl)
        .build();
    HttpResponse<byte[]> response = get(httpClient.sendAsync(httpRequest, byte[].class), Duration.ofMinutes(OCI_UPLOAD_HTTP_REQUEST_TIMEOUT));

    int responseCode = response.code();
    if (responseCode != HTTP_ACCEPTED) {
      throw new IllegalStateException("Failure in initiating upload request: " + response.message());
    } else {
      String locationHeader = parseLocationHeaderFromResponse(response, ociRegistryEndpoint.getBaseUrl());
      if (StringUtils.isBlank(locationHeader)) {
        throw new IllegalStateException(String.format("No %s header found in upload initiation response", LOCATION_HEADER));
      }
      return locationHeader;
    }
  }

  public String uploadOCIManifest(String chartName, String chartVersion, String chartConfigDigest, String chartTarballDigest, long chartConfigPayloadSize, long chartTarballContentSize) throws IOException, BadUploadException {
    String manifestUrl = ociRegistryEndpoint.getManifestUrl(chartName, chartVersion);
    String manifestPayload = createChartManifestPayload(chartConfigDigest, chartTarballDigest, chartConfigPayloadSize, chartTarballContentSize);
    InputStream requestBodyInputStream = new ByteArrayInputStream(manifestPayload.getBytes(StandardCharsets.UTF_8));
    long contentLength = manifestPayload.getBytes().length;
    HttpRequest httpRequest = createBaseOCIHttpRequest()
        .header("Host", new URL(repository.getUrl()).getHost())
        .uri(manifestUrl)
        .method("PUT", OCI_IMAGE_MANIFEST_MEDIA_TYPE, requestBodyInputStream, contentLength)
        .build();

    HttpResponse<byte[]> response = get(httpClient.sendAsync(httpRequest, byte[].class), Duration.ofMinutes(OCI_UPLOAD_HTTP_REQUEST_TIMEOUT));

    if (!response.isSuccessful()) {
      handleFailure(response);
    }
    return extractDockerContentDigestFromResponseHeaders(response);
  }

  public String uploadBlob(String uploadUrl, String blobDigest, long blobSize, String blobContentStr, File blobFile) throws IOException, BadUploadException {
    uploadUrl = new URLUtils.URLBuilder(uploadUrl).addQueryParameter("digest", String.format("sha256:%s", blobDigest)).toString();
    InputStream blobContentInputStream = blobFile != null ? Files.newInputStream(blobFile.toPath()) : new ByteArrayInputStream(blobContentStr.getBytes());
    HttpRequest httpRequest = createBaseOCIHttpRequest()
        .uri(uploadUrl)
        .method("PUT", "application/octet-stream", blobContentInputStream, blobSize)
        .build();
    HttpResponse<byte[]> response = get(httpClient.sendAsync(httpRequest, byte[].class), Duration.ofMinutes(OCI_UPLOAD_HTTP_REQUEST_TIMEOUT));

    if (!response.isSuccessful()) {
      handleFailure(response);
    }
    return extractDockerContentDigestFromResponseHeaders(response);
  }

  public boolean isLayerUploadedAlready(String chartName, String digest) throws IOException {
    String blobExistenceCheckUrl = ociRegistryEndpoint.getBlobUrl(chartName, digest);
    HttpRequest httpRequest = createBaseOCIHttpRequest().uri(blobExistenceCheckUrl).build();
    HttpResponse<byte[]> response = get(httpClient.sendAsync(httpRequest, byte[].class), Duration.ofMinutes(OCI_UPLOAD_HTTP_REQUEST_TIMEOUT));

    int responseCode = response.code();
    if (responseCode == HTTP_NOT_FOUND) {
      return false;
    }
    return responseCode == HTTP_OK;
  }

  private void handleFailure(HttpResponse<byte[]> response) throws BadUploadException {
    int responseCode = response.code();
    String responseStr = Optional.ofNullable(response.body())
        .map(String::new)
        .orElse(Optional.ofNullable(response.message()).orElse("No details provided"));
    if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
      throw new BadUploadException(responseStr);
    } else {
      throw new IllegalStateException("Received " + responseCode + " : " + responseStr);
    }
  }

  private String extractDockerContentDigestFromResponseHeaders(HttpResponse<byte[]> response) {
    String dockerContentDigest = response.header(DOCKER_CONTENT_DIGEST);
    if (StringUtils.isNotBlank(dockerContentDigest)) {
      return dockerContentDigest;
    }
    throw new IllegalStateException("No " + DOCKER_CONTENT_DIGEST + " header found in upload response");
  }

  private HttpRequest.Builder createBaseOCIHttpRequest() {
    HttpRequest.Builder httpRequestBuilder = httpClient.newHttpRequestBuilder();
    httpRequestBuilder.header("User-Agent", USER_AGENT);
    return httpRequestBuilder;
  }

  private String parseLocationHeaderFromResponse(HttpResponse<byte[]> response, String baseUrl) {
    String locationHeader = response.header(LOCATION_HEADER);

    // Only path is returned via GitHub Container Registry
    if (locationHeader != null && locationHeader.startsWith("/")) {
      locationHeader = baseUrl + locationHeader;
    }
    return locationHeader;
  }

  private String createChartManifestPayload(String chartConfigDigest, String chartTarballDigest, long chartConfigPayloadSize,
      long chartTarballContentSize) {
    OCIManifest manifest = createChartManifest(chartConfigDigest, chartTarballDigest, chartConfigPayloadSize, chartTarballContentSize);
    return Serialization.asJson(manifest);
  }

  private OCIManifest createChartManifest(String digest, String layerDigest, long chartConfigPayloadSize, long chartTarballContentSize) {
    return OCIManifest.builder()
        .schemaVersion(2)
        .config(OCIManifestLayer.builder()
            .mediaType(HELM_CONFIG_MEDIA_TYPE)
            .digest(digest)
            .size(chartConfigPayloadSize)
            .build())
        .layer(OCIManifestLayer.builder()
            .mediaType(HELM_CHART_CONTENT_MEDIA_TYPE)
            .digest(layerDigest)
            .size(chartTarballContentSize)
            .build())
        .build();
  }
}