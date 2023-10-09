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

import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.utils.URLUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.resource.helm.BadUploadException;
import org.eclipse.jkube.kit.resource.helm.Chart;
import org.eclipse.jkube.kit.resource.helm.HelmRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
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
  private final OCIRegistryEndpoint ociRegistryEndpoint;
  private final HttpClient httpClient;

  public OCIRegistryClient(HelmRepository repository, HttpClient httpClient) {
    this.ociRegistryEndpoint = new OCIRegistryEndpoint(repository);
    this.httpClient = httpClient;
  }

  public void uploadOCIManifest(Chart chart, OCIManifestLayer chartConfig, OCIManifestLayer chartTarball) throws IOException, BadUploadException {
    final byte[] manifestPayload = createChartManifestPayload(chartConfig, chartTarball)
      .getBytes(StandardCharsets.UTF_8);
    try (InputStream requestBodyInputStream = new ByteArrayInputStream(manifestPayload)) {
      HttpRequest httpRequest = newRequest()
        .uri(ociRegistryEndpoint.getManifestUrl(chart))
        .method("PUT", OCI_IMAGE_MANIFEST_MEDIA_TYPE, requestBodyInputStream, manifestPayload.length)
        .build();
      HttpResponse<byte[]> response = get(httpClient.sendAsync(httpRequest, byte[].class), Duration.ofMinutes(OCI_UPLOAD_HTTP_REQUEST_TIMEOUT));
      if (!response.isSuccessful()) {
        handleFailure(response);
      }
      extractDockerContentDigestFromResponseHeaders(response);
    }
  }

  public OCIManifestLayer uploadBlobIfNotUploadedYet(Chart chart, InputStream inputStream) throws IOException, BadUploadException {
    try (CountingInputStream blobStream = new CountingInputStream(inputStream)) {
      final OCIManifestLayer ociBlob = OCIManifestLayer.from(blobStream);
      if (isLayerUploadedAlready(chart, ociBlob)) {
        return ociBlob;
      }
      final String uploadUrl = initiateUploadProcess(chart);
      return uploadBlob(uploadUrl, blobStream);
    }
  }

  private boolean isLayerUploadedAlready(Chart chart, OCIManifestLayer blob) {
    HttpRequest httpRequest = newRequest()
      .uri(ociRegistryEndpoint.getBlobUrl(chart, blob)).build();
    HttpResponse<byte[]> response = get(httpClient.sendAsync(httpRequest, byte[].class), Duration.ofMinutes(OCI_UPLOAD_HTTP_REQUEST_TIMEOUT));
    return response.code() == HTTP_OK;
  }

  private String initiateUploadProcess(Chart chart) {
    HttpRequest httpRequest = newRequest()
      .uri(ociRegistryEndpoint.getBlobUploadInitUrl(chart))
      .post("application/json", EMPTY)
      .build();
    HttpResponse<byte[]> response = get(httpClient.sendAsync(httpRequest, byte[].class), Duration.ofMinutes(OCI_UPLOAD_HTTP_REQUEST_TIMEOUT));

    final int responseCode = response.code();
    if (responseCode != HTTP_ACCEPTED) {
      throw new IllegalStateException("Failure in initiating upload request: " + response.message());
    } else {
      String locationHeader = parseLocationHeaderFromResponse(response);
      if (StringUtils.isBlank(locationHeader)) {
        throw new IllegalStateException(String.format("No %s header found in upload initiation response", LOCATION_HEADER));
      }
      return locationHeader;
    }
  }

  private OCIManifestLayer uploadBlob(String uploadUrl, CountingInputStream blobStream) throws IOException, BadUploadException {
    final OCIManifestLayer ociBlob = OCIManifestLayer.from(blobStream);
    HttpRequest httpRequest = newRequest()
        .url(new URLUtils.URLBuilder(uploadUrl).addQueryParameter("digest", ociBlob.getDigest()).build())
        .method("PUT", "application/octet-stream", blobStream, ociBlob.getSize())
        .build();
    HttpResponse<byte[]> response = get(httpClient.sendAsync(httpRequest, byte[].class), Duration.ofMinutes(OCI_UPLOAD_HTTP_REQUEST_TIMEOUT));

    if (!response.isSuccessful()) {
      handleFailure(response);
    }
    final String dockerContentDigest = extractDockerContentDigestFromResponseHeaders(response);
    if (!ociBlob.getDigest().equals(dockerContentDigest)) {
      throw new BadUploadException(String.format("Digest mismatch. Expected %s, got %s", ociBlob.getDigest(), dockerContentDigest));
    }
    return ociBlob;
  }

  private HttpRequest.Builder newRequest() {
    return httpClient.newHttpRequestBuilder()
      .header("Host", ociRegistryEndpoint.getOCIRegistryHost())
      .header("User-Agent", USER_AGENT);
  }

  private String parseLocationHeaderFromResponse(HttpResponse<byte[]> response) {
    String locationHeader = response.header(LOCATION_HEADER);

    // Only path is returned via GitHub Container Registry
    if (locationHeader != null && locationHeader.startsWith("/")) {
      locationHeader = ociRegistryEndpoint.getBaseUrl() + locationHeader;
    }
    return locationHeader;
  }

  private static void handleFailure(HttpResponse<byte[]> response) throws BadUploadException {
    String responseStr = Optional.ofNullable(response.body())
      .map(String::new)
      .orElse(Optional.ofNullable(response.message()).orElse("No details provided"));
    throw new BadUploadException(response.code() + ": " + responseStr);
  }

  private static String extractDockerContentDigestFromResponseHeaders(HttpResponse<byte[]> response) throws BadUploadException {
    String dockerContentDigest = response.header(DOCKER_CONTENT_DIGEST);
    if (StringUtils.isNotBlank(dockerContentDigest)) {
      return dockerContentDigest;
    }
    throw new BadUploadException("No " + DOCKER_CONTENT_DIGEST + " header found in upload response");
  }

  private static String createChartManifestPayload(OCIManifestLayer chartConfig, OCIManifestLayer chartTarball) {
    return Serialization.asJson(OCIManifest.builder()
      .schemaVersion(2)
      .config(chartConfig.toBuilder().mediaType(HELM_CONFIG_MEDIA_TYPE).build())
      .layer(chartTarball.toBuilder().mediaType(HELM_CHART_CONTENT_MEDIA_TYPE).build())
      .build());
  }
}
