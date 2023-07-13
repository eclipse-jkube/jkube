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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class OCIManifestLayerTest {
  @Test
  void rawDeserialization() throws IOException {
    // Given
    final ObjectMapper mapper = new ObjectMapper();
    // When
    final OCIManifestLayer layer = mapper.readValue(
        "{\"size\":312,\"mediaType\": \"application/vnd.cncf.helm.config.v1+json\",\"digest\": \"sha256:0adb1aa9fee705698ee731ed26da27b0c2e15961d81d0616cfa38f134575f4e8\"}",
        OCIManifestLayer.class);
    // Then
    assertOCIManifestLayer(layer);
  }

  @Test
  void builder() {
    // Given
    OCIManifestLayer.OCIManifestLayerBuilder layerBuilder = OCIManifestLayer.builder()
        .size(312)
        .mediaType("application/vnd.cncf.helm.config.v1+json")
        .digest("sha256:0adb1aa9fee705698ee731ed26da27b0c2e15961d81d0616cfa38f134575f4e8");

    // When
    OCIManifestLayer layer = layerBuilder.build();

    // Then
    assertOCIManifestLayer(layer);
  }

  private void assertOCIManifestLayer(OCIManifestLayer layer) {
    assertThat(layer)
        .hasFieldOrPropertyWithValue("size", 312L)
        .hasFieldOrPropertyWithValue("mediaType", "application/vnd.cncf.helm.config.v1+json")
        .hasFieldOrPropertyWithValue("digest", "sha256:0adb1aa9fee705698ee731ed26da27b0c2e15961d81d0616cfa38f134575f4e8");
  }

  @Test
  void equalsAndHashCodeShouldMatch() {
    // Given
    OCIManifestLayer  l1 = OCIManifestLayer.builder().digest("sha256:016b77128b6bdf63ce4000e38fc36dcb15dfd6feea2d244a2c797a2d4f75a2de").build();
    OCIManifestLayer l2 = OCIManifestLayer.builder().digest("sha256:016b77128b6bdf63ce4000e38fc36dcb15dfd6feea2d244a2c797a2d4f75a2de").build();
    // When + Then
    assertThat(l1)
        .isEqualTo(l2)
        .hasSameHashCodeAs(l2);
  }
}
