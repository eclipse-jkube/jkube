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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;

class OCIRegistryEndpointTest {
  private OCIRegistryEndpoint registryEndpoint;

  @BeforeEach
  void setUp() {
    this.registryEndpoint = new OCIRegistryEndpoint("https://r.example.com/myuser");
  }

  @Test
  void getBaseUrl_whenInvoked_shouldReturnBaseUrl() throws MalformedURLException {
    assertThat(registryEndpoint.getBaseUrl()).isEqualTo("https://r.example.com");
  }

  @Test
  void getBaseUrl_whenInvokedWithRegistrySpecifyingPort_thenShouldReturnBaseUrlAsExpected() throws MalformedURLException {
    // Given
    this.registryEndpoint = new OCIRegistryEndpoint("http://localhost:5000/myuser");

    // When
    String url = registryEndpoint.getBaseUrl();

    // Then
    assertThat(url).isEqualTo("http://localhost:5000");
  }

  @Test
  void getV2ApiUrl_whenInvoked_shouldReturnV2Url() throws MalformedURLException {
    assertThat(registryEndpoint.getV2ApiUrl()).isEqualTo("https://r.example.com/v2/myuser");
  }

  @Test
  void getBlobUploadInitUrl_whenInvoked_shouldReturnBlobUploadInitUrl() throws MalformedURLException {
    assertThat(registryEndpoint.getBlobUploadInitUrl("test-chart")).isEqualTo("https://r.example.com/v2/myuser/test-chart/blobs/uploads/");
  }

  @Test
  void getManifestUrl_whenInvoked_shouldReturnManifestUrl() throws MalformedURLException {
    assertThat(registryEndpoint.getManifestUrl("test-chart", "0.0.1")).isEqualTo("https://r.example.com/v2/myuser/test-chart/manifests/0.0.1");
  }

  @Test
  void getBlobUrl_whenInvoked_shouldReturnBlobUrl() throws MalformedURLException {
    assertThat(registryEndpoint.getBlobUrl("test-chart", "7ed393daf1ffc94803c08ffcbecb798fa58e786bebffbab02da5458f68d0ecb0"))
        .isEqualTo("https://r.example.com/v2/myuser/test-chart/blobs/sha256:7ed393daf1ffc94803c08ffcbecb798fa58e786bebffbab02da5458f68d0ecb0");
  }
}
