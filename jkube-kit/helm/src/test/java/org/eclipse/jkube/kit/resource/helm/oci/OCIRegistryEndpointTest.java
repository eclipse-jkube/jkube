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

import org.eclipse.jkube.kit.resource.helm.Chart;
import org.eclipse.jkube.kit.resource.helm.HelmRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class OCIRegistryEndpointTest {
  private OCIRegistryEndpoint registryEndpoint;

  @BeforeEach
  void setUp() {
    this.registryEndpoint = new OCIRegistryEndpoint(HelmRepository.builder().url("https://r.example.com/myuser").build());
  }

  @Test
  void getBaseUrl_withOciProtocol() {
    assertThat(new OCIRegistryEndpoint(HelmRepository.builder().url("oci://r.example.com/myuser").build()))
      .extracting(OCIRegistryEndpoint::getBaseUrl)
      .isEqualTo(URI.create("http://r.example.com"));
  }

  @Test
  void getBaseUrl_withOcisProtocol() {
    assertThat(new OCIRegistryEndpoint(HelmRepository.builder().url("ocis://r.example.com/myuser").build()))
      .extracting(OCIRegistryEndpoint::getBaseUrl)
      .isEqualTo(URI.create("https://r.example.com"));
  }

  @Test
  void getBaseUrl_whenInvoked_shouldReturnBaseUrl() {
    assertThat(registryEndpoint.getBaseUrl()).isEqualTo(URI.create("https://r.example.com"));
  }

  @Test
  void getBaseUrl_whenInvokedWithRegistrySpecifyingPort_thenShouldReturnBaseUrlAsExpected() {
    // Given
    this.registryEndpoint = new OCIRegistryEndpoint(HelmRepository.builder().url("http://localhost:5000/myuser").build());

    // When
    String url = registryEndpoint.getBaseUrl().toString();

    // Then
    assertThat(url).isEqualTo("http://localhost:5000");
  }

  @Test
  void getBlobUploadInitUrl_whenInvoked_shouldReturnBlobUploadInitUrl() {
    assertThat(registryEndpoint.getBlobUploadInitUrl(Chart.builder().name("test-chart").build()))
      .isEqualTo("https://r.example.com/v2/myuser/test-chart/blobs/uploads/");
  }

  @Test
  void getManifestUrl_whenInvoked_shouldReturnManifestUrl() {
    assertThat(registryEndpoint.getManifestUrl(Chart.builder().name("test-chart").version("0.0.1").build()))
      .isEqualTo("https://r.example.com/v2/myuser/test-chart/manifests/0.0.1");
  }

  @Test
  void getBlobUrl_whenInvoked_shouldReturnBlobUrl() {
    final String result = registryEndpoint.getBlobUrl(
      Chart.builder().name("test-chart").build(),
      OCIManifestLayer.builder().digest("sha256:7ed393daf1ffc94803c08ffcbecb798fa58e786bebffbab02da5458f68d0ecb0").build());
    assertThat(result)
        .isEqualTo("https://r.example.com/v2/myuser/test-chart/blobs/sha256:7ed393daf1ffc94803c08ffcbecb798fa58e786bebffbab02da5458f68d0ecb0");
  }

  @ParameterizedTest(name = "getOCIRegistryHost with url {0} should return {1}")
  @CsvSource({
      "http://localhost:5000/myuser,localhost:5000",
      "https://r.example.com/myuser,r.example.com",
      "https://r.example.com:443/myuser,r.example.com",
      "http://r.example.com:80/myuser,r.example.com"
  })
  void getOCIRegistryHost_whenBaseUrlContainsHostAndPort_thenReturnHostWithPort(String url, String expectedHost) {
    // Given
    this.registryEndpoint = new OCIRegistryEndpoint(HelmRepository.builder().url(url).build());

    // When
    String host = registryEndpoint.getOCIRegistryHost();

    // Then
    assertThat(host).isEqualTo(expectedHost);
  }
}
