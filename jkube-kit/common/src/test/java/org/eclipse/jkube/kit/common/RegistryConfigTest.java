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
package org.eclipse.jkube.kit.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertNotNull;

public class RegistryConfigTest {

  /**
   * Verifies that deserialization works for raw deserialization disregarding annotations.
   */
  @Test
  public void rawDeserialization() throws IOException {
    // Given
    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(MapperFeature.USE_ANNOTATIONS, false);
    // When
    final RegistryConfig result = mapper.readValue(
        RegistryConfigTest.class.getResourceAsStream("/registry-config.json"),
        RegistryConfig.class
    );
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("registry", "the-registry")
        .hasFieldOrPropertyWithValue("skipExtendedAuth", true)
        .hasFieldOrPropertyWithValue("authConfig", null)
        .extracting(RegistryConfig::getSettings).asList().hasSize(1).extracting("id", "username")
        .containsExactly(tuple("server-1", "the-user"));
  }

  @Test
  public void testGetRegistryConfig() {
    // Given
    List<RegistryServerConfiguration> registryServerConfigurationList = new ArrayList<>();
    registryServerConfigurationList.add(RegistryServerConfiguration.builder()
            .username("someuser")
            .password("somepassword")
            .id("quay.io")
            .build());

    // When
    RegistryConfig registryConfig = RegistryConfig.getRegistryConfig("quay.io", registryServerConfigurationList, Collections.emptyMap(), true, "quay.io", s -> s);

    // Then
    assertNotNull(registryConfig);
    assertThat(registryConfig.getRegistry()).isEqualTo("quay.io");
    assertThat(registryConfig.getSettings())
            .hasSize(1).element(0)
            .hasFieldOrPropertyWithValue("password", "somepassword")
            .hasFieldOrPropertyWithValue("username", "someuser")
            .hasFieldOrPropertyWithValue("id", "quay.io");
  }
}
