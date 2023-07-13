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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jkube.kit.resource.helm.BadUploadException;
import org.eclipse.jkube.kit.resource.helm.HelmRepository;
import org.eclipse.jkube.kit.resource.helm.TestMockResponseProvider;

import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import io.fabric8.mockwebserver.DefaultMockServer;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class OCIRegistryClientTest {
  private OCIRegistryClient oci;
  private String chartName;
  private String chartVersion;
  private String chartTarballBlobDigest;
  private String chartConfigBlobDigest;
  private long chartConfigPayloadSizeInBytes;
  private File chartFile;
  private HttpClient httpClient;
  private DefaultMockServer server;
  @TempDir
  File temporaryFolder;

  @BeforeEach
  void setUp() throws IOException {
    chartName = "test-chart";
    chartVersion = "0.0.1";
    chartConfigBlobDigest = "f2ab3e153f678e5f01062717a203f4ca47a556159bcbb1e8a3ec5d84b5dd7aef";
    chartTarballBlobDigest = "98c4987b6502c7eb8e29a8844e0e1f1d19a8925594f8271ae70f9a51412e737a";
    chartConfigPayloadSizeInBytes = 10;
    chartFile = new File(temporaryFolder, "test-chart-0.0.1.tar.gz");
    Files.write(chartFile.toPath(), "helm-chart-content".getBytes(StandardCharsets.UTF_8));
    server = new DefaultMockServer();
    server.start();
    httpClient = HttpClientUtils.getHttpClientFactory().newBuilder().build();
    HelmRepository helmRepository = HelmRepository.builder()
        .url(String.format("%s/myuser", getServerUrl()))
        .build();
    oci = new OCIRegistryClient(helmRepository, httpClient);
  }

  @AfterEach
  void tearDown() {
    server.shutdown();
    httpClient.close();
  }

  private String getServerUrl() {
    return String.format("http://%s:%d", server.getHostName(), server.getPort());
  }

  @Test
  void getBaseUrl_whenInvoked_shouldReturnRegistryUrl() throws MalformedURLException {
    assertThat(oci.getBaseUrl()).isEqualTo(getServerUrl());
  }

  @Test
  void initiateUploadProcess_whenRegistryResponseSuccessfulAndContainsLocation_thenReturnUploadUrl() throws IOException {
    // Given
    Map<String,String> uploadResponseHeaders = new HashMap<>();
    String responseLocationHeader = getServerUrl() + "/v2/myuser/test-chart/blobs/uploads/17f1053c-fcd7-47a7-a34b-bbf23bbdf906?_state=uploadstate";
    uploadResponseHeaders.put(HttpHeaders.LOCATION, responseLocationHeader);
    server.expect().post()
            .withPath("/v2/myuser/test-chart/blobs/uploads/")
        .andReply(new TestMockResponseProvider(HTTP_ACCEPTED, uploadResponseHeaders, null))
        .once();

    // When
    String uploadUrl = oci.initiateUploadProcess(chartName);

    // Then
    assertThat(uploadUrl).isEqualTo(responseLocationHeader);
  }

  @Test
  void initiateUploadProcess_whenRegistryResponseSuccessfulButLocationHeaderContainsPathOnly_thenReturnUploadUrl() throws IOException {
    // Given
    Map<String, String> uploadResponseHeaders = new HashMap<>();
    String responseLocationHeader = "/v2/myuser/test-chart/blobs/uploads/17f1053c-fcd7-47a7-a34b-bbf23bbdf906?_state=uploadstate";
    uploadResponseHeaders.put(HttpHeaders.LOCATION, responseLocationHeader);
    server.expect().post()
        .withPath("/v2/myuser/test-chart/blobs/uploads/")
        .andReply(new TestMockResponseProvider(HTTP_ACCEPTED, uploadResponseHeaders, null))
        .once();

    // When
    String uploadUrl = oci.initiateUploadProcess(chartName);

    // Then
    assertThat(uploadUrl).isEqualTo(String.format("http://%s:%d%s", server.getHostName(), server.getPort(), responseLocationHeader));
  }

  @Test
  void initiateUploadProcess_whenRegistryResponseSuccessfulButNoHeader_thenThrowException() {
    // Given
    server.expect().post()
        .withPath("/v2/myuser/test-chart/blobs/uploads/")
        .andReturn(HTTP_ACCEPTED, "")
        .once();

    // When + Then
    assertThatIllegalStateException()
        .isThrownBy(() -> oci.initiateUploadProcess(chartName))
        .withMessage("No Location header found in upload initiation response");
  }

  @Test
  void initiateUploadProcess_whenRegistryResponseFailure_thenThrowException() {
    // Given
    server.expect().post()
        .withPath("/v2/myuser/test-chart/blobs/uploads/")
        .andReturn(HTTP_NOT_FOUND, "")
        .once();

    // When + Then
    assertThatIllegalStateException()
        .isThrownBy(() -> oci.initiateUploadProcess(chartName))
        .withMessage("Failure in initiating upload request: Not Found");
  }

  @Test
  void uploadOCIManifest_whenManifestSuccessfullyPushed_thenReturnDockerContentDigest() throws BadUploadException, IOException {
    // Given
    String responseDockerContentDigestHeader = "sha256:createdmanifestdigest";
    server.expect().put()
        .withPath("/v2/myuser/test-chart/manifests/0.0.1")
        .andReply(new TestMockResponseProvider(HTTP_CREATED, Collections.singletonMap("Docker-Content-Digest", responseDockerContentDigestHeader), null))
        .once();

    // When
    String dockerContentDigest = oci.uploadOCIManifest(chartName, chartVersion, chartConfigBlobDigest, chartTarballBlobDigest, chartConfigPayloadSizeInBytes, chartFile.length());

    // Then
    assertThat(dockerContentDigest).isEqualTo(responseDockerContentDigestHeader);
  }

  @Test
  void uploadOCIManifest_whenRegistryRejectedManifest_thenThrowException() {
    // Given
    server.expect().put()
        .withPath("/v2/myuser/test-chart/manifests/0.0.1")
        .andReply(new TestMockResponseProvider(HTTP_BAD_REQUEST, Collections.emptyMap(), "invalid manifest"))
        .once();

    // When + Then
    assertThatExceptionOfType(BadUploadException.class)
        .isThrownBy(() -> oci.uploadOCIManifest(chartName, chartVersion, chartConfigBlobDigest, chartTarballBlobDigest, chartConfigPayloadSizeInBytes, chartFile.length()))
        .withMessage("invalid manifest");
  }

  @Test
  void uploadOCIManifest_whenManifestSuccessfullyPushedButNoDockerContentDigest_thenThrowException() {
    // Given
    server.expect().put()
        .withPath("/v2/myuser/test-chart/manifests/0.0.1")
        .andReply(new TestMockResponseProvider(HTTP_CREATED, Collections.emptyMap(), null))
        .once();

    // When
    assertThatIllegalStateException()
        .isThrownBy(() -> oci.uploadOCIManifest(chartName, chartVersion, chartConfigBlobDigest, chartTarballBlobDigest, chartConfigPayloadSizeInBytes, chartFile.length()))
        .withMessage("No Docker-Content-Digest header found in upload response");
  }

  @Test
  void uploadBlob_whenBlobSuccessfullyPushedToRegistry_thenReturnDockerContentDigest() throws BadUploadException, IOException {
    // Given
    String blobUploadUrl = getServerUrl() + "/v2/myuser/test-chart/blobs/uploads/17f1053c-fcd7-47a7-a34b-bbf23bbdf906?_state=XZnxHKS";
    Map<String, String> responseHeaders = new HashMap<>();
    responseHeaders.put("Docker-Content-Digest", "sha256:016b77128b6bdf63ce4000e38fc36dcb15dfd6feea2d244a2c797a2d4f75a2de");
    server.expect().put()
        .withPath("/v2/myuser/test-chart/blobs/uploads/17f1053c-fcd7-47a7-a34b-bbf23bbdf906?_state=XZnxHKS&digest=sha256%3A98c4987b6502c7eb8e29a8844e0e1f1d19a8925594f8271ae70f9a51412e737a")
        .andReply(new TestMockResponseProvider(HTTP_CREATED, responseHeaders, null))
        .once();

    // When
    String dockerContentDigest = oci.uploadBlob(blobUploadUrl, chartTarballBlobDigest, chartFile.length(), null, chartFile);

    // Then
    assertThat(dockerContentDigest)
        .isEqualTo("sha256:016b77128b6bdf63ce4000e38fc36dcb15dfd6feea2d244a2c797a2d4f75a2de");
  }

  @Test
  void uploadBlob_whenBlobRejectedByRegistry_thenThrowException() {
    // Given
    String blobUploadUrl = getServerUrl() + "/v2/myuser/test-chart/blobs/uploads/17f1053c-fcd7-47a7-a34b-bbf23bbdf906?_state=XZnxHKS";
    server.expect().put()
        .withPath("/v2/myuser/test-chart/blobs/uploads/17f1053c-fcd7-47a7-a34b-bbf23bbdf906?_state=XZnxHKS&digest=sha256%3A98c4987b6502c7eb8e29a8844e0e1f1d19a8925594f8271ae70f9a51412e737a")
        .andReply(new TestMockResponseProvider(HTTP_BAD_REQUEST, Collections.emptyMap(), "invalid data"))
        .once();

    // When + Then
    assertThatExceptionOfType(BadUploadException.class)
        .isThrownBy(() -> oci.uploadBlob(blobUploadUrl, chartTarballBlobDigest, chartFile.length(), null, chartFile))
        .withMessage("invalid data");
  }

  @Test
  void uploadBlob_whenBlobSuccessfullyPushedToRegistryButNoDockerContentDigest_thenThrowException() throws BadUploadException, IOException {
    // Given
    String blobUploadUrl = getServerUrl() + "/v2/myuser/test-chart/blobs/uploads/17f1053c-fcd7-47a7-a34b-bbf23bbdf906?_state=XZnxHKS";
    server.expect().put()
        .withPath("/v2/myuser/test-chart/blobs/uploads/17f1053c-fcd7-47a7-a34b-bbf23bbdf906?_state=XZnxHKS&digest=sha256%3A98c4987b6502c7eb8e29a8844e0e1f1d19a8925594f8271ae70f9a51412e737a")
        .andReply(new TestMockResponseProvider(HTTP_CREATED, Collections.emptyMap(), null))
        .once();

    // When + Then
    assertThatIllegalStateException()
        .isThrownBy(() -> oci.uploadBlob(blobUploadUrl, chartTarballBlobDigest, chartFile.length(), null, chartFile))
        .withMessage("No Docker-Content-Digest header found in upload response");
  }

  @Test
  void isLayerUploadedAlready_whenRegistryReturns200_thenReturnTrue() throws IOException {
    // Given
    server.expect().get()
        .withPath("/v2/myuser/test-chart/blobs/sha256:" + chartConfigBlobDigest)
        .andReply(new TestMockResponseProvider(HTTP_OK, Collections.emptyMap(), null))
        .once();

    // When
    boolean result = oci.isLayerUploadedAlready(chartName, chartConfigBlobDigest);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isLayerUploadedAlready_whenRegistryReturns404_thenReturnFalse() throws IOException {
    // Given
    server.expect().get()
        .withPath("/v2/myuser/test-chart/blobs/sha256:" + chartConfigBlobDigest)
        .andReply(new TestMockResponseProvider(HTTP_NOT_FOUND, Collections.emptyMap(), null))
        .once();

    // When
    boolean result = oci.isLayerUploadedAlready(chartName, chartConfigBlobDigest);

    // Then
    assertThat(result).isFalse();
  }
}
