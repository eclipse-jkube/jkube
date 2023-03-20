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

class RequestsLimitsConfigTest {
  @Test
  void rawDeserialization() throws IOException {
    // Given
    final ObjectMapper mapper = new ObjectMapper();
    // When
    final RequestsLimitsConfig result = mapper.readValue(
        getClass().getResourceAsStream("/requests-limits-config.json"),
        RequestsLimitsConfig.class);
    // Then
    assertRequestsLimitsConfig(result);
  }

  @Test
  void builder() {
    // Given
    RequestsLimitsConfig.RequestsLimitsConfigBuilder requestsLimitsConfigBuilder = RequestsLimitsConfig.builder()
        .request("cpu", "250m")
        .request("memory", "64Mi")
        .limit("cpu", "500m")
        .limit("memory", "128Mi");

    // When
    RequestsLimitsConfig requestsLimitsConfig = requestsLimitsConfigBuilder.build();

    // Then
    assertRequestsLimitsConfig(requestsLimitsConfig);
  }

  @Test
  void equalsAndHashCodeShouldMatch() {
    // Given
    RequestsLimitsConfig  rlc1 = RequestsLimitsConfig.builder().request("cpu", "500m").build();
    RequestsLimitsConfig rlc2 = RequestsLimitsConfig.builder().request("cpu", "500m").build();
    // When + Then
    assertThat(rlc1)
        .isEqualTo(rlc2)
        .hasSameHashCodeAs(rlc2);
  }

  private void assertRequestsLimitsConfig(RequestsLimitsConfig requestsLimitsConfig) {
    assertThat(requestsLimitsConfig)
        .hasFieldOrPropertyWithValue("requests.cpu", "250m")
        .hasFieldOrPropertyWithValue("requests.memory", "64Mi")
        .hasFieldOrPropertyWithValue("limits.memory", "128Mi")
        .hasFieldOrPropertyWithValue("limits.cpu", "500m");
  }
}
