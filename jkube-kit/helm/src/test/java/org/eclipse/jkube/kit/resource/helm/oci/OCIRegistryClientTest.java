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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import org.eclipse.jkube.kit.resource.helm.BadUploadException;
import org.eclipse.jkube.kit.resource.helm.Chart;
import org.eclipse.jkube.kit.resource.helm.HelmRepository;
import org.eclipse.jkube.kit.resource.helm.TestMockResponseProvider;

import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import io.fabric8.mockwebserver.DefaultMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OCIRegistryClientTest {
  private OCIRegistryClient oci;
  private Chart chart;
  private OCIManifestLayer chartConfigBlob;
  private OCIManifestLayer chartTarballBlob;
  private InputStream chartTarballStream;
  private HttpClient httpClient;
  private DefaultMockServer server;
  @TempDir
  File temporaryFolder;

  @BeforeEach
  void setUp() throws IOException {
    chart = Chart.builder().name("test-chart").version("0.0.1").build();
    chartConfigBlob = OCIManifestLayer.builder()
      .digest("sha256:f2ab3e153f678e5f01062717a203f4ca47a556159bcbb1e8a3ec5d84b5dd7aef")
      .size(10L)
      .build();
    chartTarballBlob = OCIManifestLayer.builder()
      .digest("sha256:2ede29ddc2914a307eb3402a3db9f65d63bca95a27cd8f300e1c13538a667778")
      .build();
    final File chartTarball = new File(temporaryFolder, "test-chart-0.0.1.tar.gz");
    Files.write(chartTarball.toPath(), "helm-chart-content".getBytes(StandardCharsets.UTF_8));
    chartTarballStream = new BufferedInputStream(Files.newInputStream(chartTarball.toPath()));
    server = new DefaultMockServer();
    server.start();
    httpClient = HttpClientUtils.getHttpClientFactory().newBuilder().build();
    HelmRepository helmRepository = HelmRepository.builder()
        .url(server.url("/my-registry"))
        .build();
    oci = new OCIRegistryClient(helmRepository, httpClient);
  }

  @AfterEach
  void tearDown() {
    server.shutdown();
    httpClient.close();
  }

  @Nested
  @DisplayName("uploadBlobIfNotUploadedYet")
  class UploadBlobIfNotUploadedYet {

    @Test
    void withExistentLayer_skipsUpload() throws Exception {
      // Given
      server.expect().get()
        .withPath("/v2/my-registry/test-chart/blobs/sha256:2ede29ddc2914a307eb3402a3db9f65d63bca95a27cd8f300e1c13538a667778")
        .andReturn(200, null)
        .once();
      // When
      final OCIManifestLayer result = oci.uploadBlobIfNotUploadedYet(chart, chartTarballStream);
      // Then
      assertThat(server.getRequestCount()).isEqualTo(1);
      assertThat(result)
        .hasFieldOrPropertyWithValue("digest", "sha256:2ede29ddc2914a307eb3402a3db9f65d63bca95a27cd8f300e1c13538a667778")
        .hasFieldOrPropertyWithValue("size", 18L);
    }

    @Test
    void withExistentCorruptLayer_performsUpload() throws Exception {
      // Given
      server.expect().get()
        .withPath("/v2/my-registry/test-chart/blobs/sha256:2ede29ddc2914a307eb3402a3db9f65d63bca95a27cd8f300e1c13538a667778")
        .andReply(new TestMockResponseProvider(410, Collections.emptyMap(), null))
        .always();
      server.expect().post()
        .withPath("/v2/my-registry/test-chart/blobs/uploads/")
        .andReply(new TestMockResponseProvider(202,
          Collections.singletonMap("Location", "/v2/my-registry/test-chart/blobs/uploads/location"), null))
        .once();
      server.expect().put()
        .withPath("/v2/my-registry/test-chart/blobs/uploads/location?digest=sha256%3A2ede29ddc2914a307eb3402a3db9f65d63bca95a27cd8f300e1c13538a667778")
        .andReply(new TestMockResponseProvider(201,
          Collections.singletonMap("Docker-Content-Digest", "sha256:2ede29ddc2914a307eb3402a3db9f65d63bca95a27cd8f300e1c13538a667778"), null))
        .once();
      // When
      final OCIManifestLayer result = oci.uploadBlobIfNotUploadedYet(chart, chartTarballStream);
      // Then
      assertThat(server.getRequestCount()).isGreaterThan(1);
      assertThat(result)
        .hasFieldOrPropertyWithValue("digest", "sha256:2ede29ddc2914a307eb3402a3db9f65d63bca95a27cd8f300e1c13538a667778")
        .hasFieldOrPropertyWithValue("size", 18L);
    }

    @Test
    void withUploadLocationMissing_throwsException() {
      // Given
      server.expect().post()
        .withPath("/v2/my-registry/test-chart/blobs/uploads/")
        .andReturn(202, "")
        .once();
      // When + Then
      assertThatIllegalStateException()
        .isThrownBy(() -> oci.uploadBlobIfNotUploadedYet(chart, chartTarballStream))
        .withMessage("No Location header found in upload initiation response");
    }

    @Test
    void withUploadLocationNotAccepted_throwsException() {
      // Given
      server.expect().post()
        .withPath("/v2/my-registry/test-chart/blobs/uploads/")
        .andReturn(418, "")
        .once();
      // When + Then
      assertThatIllegalStateException()
        .isThrownBy(() -> oci.uploadBlobIfNotUploadedYet(chart, chartTarballStream))
        .withMessage("Failure in initiating upload request: I'm a Teapot");
    }

    @Test
    void withUploadFailure_throwsException() {
      // Given
      server.expect().post()
        .withPath("/v2/my-registry/test-chart/blobs/uploads/")
        .andReply(new TestMockResponseProvider(202,
          Collections.singletonMap("Location", "/v2/my-registry/test-chart/blobs/uploads/location"), null))
        .once();
      server.expect().put()
        .withPath("/v2/my-registry/test-chart/blobs/uploads/location?digest=sha256%3A2ede29ddc2914a307eb3402a3db9f65d63bca95a27cd8f300e1c13538a667778")
        .andReturn(400, "invalid data")
        .once();
      // When + Then
      assertThatThrownBy(() -> oci.uploadBlobIfNotUploadedYet(chart, chartTarballStream))
        .isInstanceOf(BadUploadException.class)
        .hasMessage("400: invalid data");
    }

    @Test
    void withDockerDigestMissing_throwsException() {
      // Given
      server.expect().post()
        .withPath("/v2/my-registry/test-chart/blobs/uploads/")
        .andReply(new TestMockResponseProvider(202,
          Collections.singletonMap("Location", "/v2/my-registry/test-chart/blobs/uploads/location"), null))
        .once();
      server.expect().put()
        .withPath("/v2/my-registry/test-chart/blobs/uploads/location?digest=sha256%3A2ede29ddc2914a307eb3402a3db9f65d63bca95a27cd8f300e1c13538a667778")
        .andReturn(201, null)
        .once();
      // When + Then
      assertThatThrownBy(() -> oci.uploadBlobIfNotUploadedYet(chart, chartTarballStream))
        .isInstanceOf(BadUploadException.class)
        .hasMessage("No Docker-Content-Digest header found in upload response");
    }

    @Test
    void withDockerDigestMismatch_throwsException() {
      // Given
      server.expect().post()
        .withPath("/v2/my-registry/test-chart/blobs/uploads/")
        .andReply(new TestMockResponseProvider(202,
          Collections.singletonMap("Location", "/v2/my-registry/test-chart/blobs/uploads/location"), null))
        .once();
      server.expect().put()
        .withPath("/v2/my-registry/test-chart/blobs/uploads/location?digest=sha256%3A2ede29ddc2914a307eb3402a3db9f65d63bca95a27cd8f300e1c13538a667778")
        .andReply(new TestMockResponseProvider(201,
          Collections.singletonMap("Docker-Content-Digest", "sha256:different"), null))
        .once();
      // When + Then
      assertThatThrownBy(() -> oci.uploadBlobIfNotUploadedYet(chart, chartTarballStream))
        .isInstanceOf(BadUploadException.class)
        .hasMessage("Digest mismatch. Expected sha256:2ede29ddc2914a307eb3402a3db9f65d63bca95a27cd8f300e1c13538a667778, got sha256:different");
    }
  }

  @Nested
  @DisplayName("uploadOCIManifest")
  class UploadOCIManifest {
    @Test
    void withSuccessfulResponse() {
      // Given
      String responseDockerContentDigestHeader = "sha256:createdmanifestdigest";
      server.expect().put()
        .withPath("/v2/my-registry/test-chart/manifests/0.0.1")
        .andReply(new TestMockResponseProvider(201, Collections.singletonMap("Docker-Content-Digest", responseDockerContentDigestHeader), null))
        .once();
      // When + Then
      assertThatNoException().isThrownBy(() -> oci.uploadOCIManifest(chart, chartConfigBlob, chartTarballBlob));
    }

    @Test
    void withErrorResponse_throwsException() {
      // Given
      server.expect().put()
        .withPath("/v2/my-registry/test-chart/manifests/0.0.1")
        .andReturn(400, "invalid manifest")
        .once();
      // When + Then
      assertThatExceptionOfType(BadUploadException.class)
        .isThrownBy(() -> oci.uploadOCIManifest(chart, chartConfigBlob, chartTarballBlob))
        .withMessage("400: invalid manifest");
    }

    @Test
    void withDockerDigestMissing_throwsException() {
      // Given
      server.expect().put()
        .withPath("/v2/my-registry/test-chart/manifests/0.0.1")
        .andReturn(201, null)
        .once();
      // When + Then
      assertThatExceptionOfType(BadUploadException.class)
        .isThrownBy(() -> oci.uploadOCIManifest(chart, chartConfigBlob, chartTarballBlob))
        .withMessage("No Docker-Content-Digest header found in upload response");
    }
  }
}
