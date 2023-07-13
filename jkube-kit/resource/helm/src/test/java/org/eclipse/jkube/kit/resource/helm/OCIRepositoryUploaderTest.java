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
package org.eclipse.jkube.kit.resource.helm;

import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.resource.helm.oci.OCIRegistryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class OCIRepositoryUploaderTest {
  private OCIRepositoryUploader ociRepositoryUploader;
  private HelmRepository helmRepository;
  @TempDir
  private File tempDir;
  private File chartFile;

  @BeforeEach
  void setUp() throws IOException {
    helmRepository = HelmRepository.builder()
        .url("https://r.example.com/myuser")
        .username("myuser")
        .build();
    ociRepositoryUploader = new OCIRepositoryUploader();
    File chartMetadataFile = new File(tempDir, "Chart.yaml");
    chartFile = new File(tempDir, "test-chart-0.0.1.tar.gz");
    assertThat(chartFile.createNewFile()).isTrue();
    assertThat(chartMetadataFile.createNewFile()).isTrue();
    FileUtils.write(chartMetadataFile, "---\napiVersion: v1\nname: test-chart\nversion: 0.0.1", Charset.defaultCharset());
  }

  @Test
  void uploadSingle_whenChartBlobsAlreadyUploaded_thenLogPushSkip() throws BadUploadException, IOException {
    try (MockedConstruction<OCIRegistryClient> ociMockedConstruction = mockConstruction(OCIRegistryClient.class, (mock, ctx) -> {
      when(mock.getBaseUrl()).thenReturn("https://r.example.com");
      when(mock.isLayerUploadedAlready(anyString(), anyString())).thenReturn(true);
      when(mock.uploadOCIManifest(anyString(), anyString(), anyString(), anyString(), anyLong(), anyLong())).thenReturn("sha256:uploadmanifestdigest");
    })) {
      // When
      ociRepositoryUploader.uploadSingle(chartFile, helmRepository);

      // Then
      assertThat(ociMockedConstruction.constructed()).hasSize(1);
    }
  }

  @Test
  void uploadSingle_whenChartPushFailed_thenThrowException() {
    try (MockedConstruction<OCIRegistryClient> ignore = mockConstruction(OCIRegistryClient.class, (mock, ctx) -> {
      when(mock.getBaseUrl()).thenReturn("https://r.example.com");
      when(mock.isLayerUploadedAlready(anyString(), anyString())).thenReturn(false);
      when(mock.initiateUploadProcess(anyString())).thenReturn("https://r.example.com/v2/myuser/blobs/uploads/random-uuid?state=testing");
      when(mock.uploadBlob(anyString(), anyString(), anyLong(), anyString(), any()))
          .thenThrow(new BadUploadException("invalid upload data"));
    })) {
      // When
      assertThatExceptionOfType(BadUploadException.class)
          .isThrownBy(() -> ociRepositoryUploader.uploadSingle(chartFile, helmRepository))
          .withMessage("invalid upload data");
    }
  }

  @Test
  void uploadSingle_whenChartSuccessfullyPushedToRegistry_thenLogDockerContentManifest() throws BadUploadException, IOException {
    try (MockedConstruction<OCIRegistryClient> ociMockedConstruction = mockConstruction(OCIRegistryClient.class, (mock, ctx) -> {
      when(mock.getBaseUrl()).thenReturn("https://r.example.com");
      when(mock.isLayerUploadedAlready(anyString(), anyString())).thenReturn(false);
      when(mock.initiateUploadProcess(anyString())).thenReturn("https://r.example.com/v2/myuser/blobs/uploads/random-uuid?state=testing");
      when(mock.uploadBlob(anyString(), anyString(), anyLong(), isNull(), any())).thenReturn("sha256:charttarballdigest");
      when(mock.uploadBlob(anyString(), anyString(), anyLong(), anyString(), isNull())).thenReturn("sha256:chartconfigdigest");
      when(mock.uploadOCIManifest(anyString(), anyString(), anyString(), anyString(), anyLong(), anyLong())).thenReturn("sha256:uploadmanifestdigest");
    })) {
      // When
      ociRepositoryUploader.uploadSingle(chartFile, helmRepository);

      // Then
      assertThat(ociMockedConstruction.constructed()).hasSize(1);
    }
  }
}
