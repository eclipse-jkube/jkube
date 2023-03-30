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
package org.eclipse.jkube.kit.config.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ContainerResourcesConfigTest {
  @Test
  void rawDeserialization() throws IOException {
    // Given
    final ObjectMapper mapper = new ObjectMapper();
    // When
    final ContainerResourcesConfig result = mapper.readValue(
        getClass().getResourceAsStream("/container-resources-config.json"),
        ContainerResourcesConfig.class);
    // Then
    assertRequestsLimitsConfig(result);
  }

  @Test
  void builder() {
    // Given
    ContainerResourcesConfig.ContainerResourcesConfigBuilder containerResourcesBuilder = ContainerResourcesConfig.builder()
        .request("cpu", "250m")
        .request("memory", "64Mi")
        .limit("cpu", "500m")
        .limit("memory", "128Mi");

    // When
    ContainerResourcesConfig containerResources = containerResourcesBuilder.build();

    // Then
    assertRequestsLimitsConfig(containerResources);
  }

  @Test
  void equalsAndHashCodeShouldMatch() {
    // Given
    ContainerResourcesConfig crc1 = ContainerResourcesConfig.builder().request("cpu", "500m").build();
    ContainerResourcesConfig crc2 = ContainerResourcesConfig.builder().request("cpu", "500m").build();
    // When + Then
    assertThat(crc1)
        .isEqualTo(crc2)
        .hasSameHashCodeAs(crc2);
  }

  private void assertRequestsLimitsConfig(ContainerResourcesConfig containerResources) {
    assertThat(containerResources)
        .hasFieldOrPropertyWithValue("requests.cpu", "250m")
        .hasFieldOrPropertyWithValue("requests.memory", "64Mi")
        .hasFieldOrPropertyWithValue("limits.memory", "128Mi")
        .hasFieldOrPropertyWithValue("limits.cpu", "500m");
  }
}
