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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class OCIManifestTest {
  @Test
  void rawDeserialization() throws IOException {
    // Given
    final ObjectMapper mapper = new ObjectMapper();
    // When
    final OCIManifest result = mapper.readValue(
        getClass().getResourceAsStream("/test-oci-manifest.json"),
        OCIManifest.class);
    // Then
    assertOCIManifest(result);
  }

  @Test
  void builder() {
    // Given
    OCIManifest.OCIManifestBuilder manifestBuilder = OCIManifest.builder()
        .schemaVersion(2)
        .config(OCIManifestLayer.builder()
            .size(312)
            .mediaType("application/vnd.cncf.helm.config.v1+json")
            .digest("sha256:fe8b2f27ce12b302342d4a5da2b2945ab869c7acb9e1b718c5426d91ce38cfc4")
            .build())
        .layer(OCIManifestLayer.builder()
            .size(9272)
            .mediaType("application/vnd.cncf.helm.chart.content.v1.tar+gzip")
            .digest("sha256:fe8b2f27ce12b302342d4a5da2b2945ab869c7acb9e1b718c5426d91ce38cfc4")
            .build());

    // When
    OCIManifest manifest = manifestBuilder.build();

    // Then
    assertOCIManifest(manifest);
  }

  private void assertOCIManifest(OCIManifest manifest) {
    assertThat(manifest)
        .hasFieldOrPropertyWithValue("schemaVersion", 2)
        .hasFieldOrPropertyWithValue("config.mediaType", "application/vnd.cncf.helm.config.v1+json")
        .hasFieldOrPropertyWithValue("config.digest", "sha256:fe8b2f27ce12b302342d4a5da2b2945ab869c7acb9e1b718c5426d91ce38cfc4")
        .hasFieldOrPropertyWithValue("config.size", 312L)
        .extracting(OCIManifest::getLayers)
        .asList()
        .singleElement(InstanceOfAssertFactories.type(OCIManifestLayer.class))
        .hasFieldOrPropertyWithValue("size", 9272L)
        .hasFieldOrPropertyWithValue("mediaType", "application/vnd.cncf.helm.chart.content.v1.tar+gzip")
        .hasFieldOrPropertyWithValue("digest", "sha256:fe8b2f27ce12b302342d4a5da2b2945ab869c7acb9e1b718c5426d91ce38cfc4");
  }

  @Test
  void equalsAndHashCodeShouldMatch() {
    // Given
    OCIManifest m1 = OCIManifest.builder().schemaVersion(2).build();
    OCIManifest m2 = OCIManifest.builder().schemaVersion(2).build();
    // When + Then
    assertThat(m1)
        .isEqualTo(m2)
        .hasSameHashCodeAs(m2);
  }
}
