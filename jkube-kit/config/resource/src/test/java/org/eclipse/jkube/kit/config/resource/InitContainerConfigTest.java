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
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.Arguments;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class InitContainerConfigTest {
  @Test
  void rawDeserialization() throws IOException {
    // Given
    final ObjectMapper mapper = new ObjectMapper();
    // When
    final InitContainerConfig result = mapper.readValue(
        getClass().getResourceAsStream("/initcontainer-config.json"),
        InitContainerConfig.class);
    // Then
    assertInitContainerConfig(result);
  }

  @Test
  void builder() {
    // Given
    InitContainerConfig.InitContainerConfigBuilder initContainerConfigBuilder = InitContainerConfig.builder()
        .env(Collections.singletonMap("FOO", "BAR"))
        .name("init1")
        .imageName("busybox:latest")
        .imagePullPolicy("IfNotPresent")
        .cmd(Arguments.builder().exec(Arrays.asList("sleep", "10")).build())
        .volumes(Collections.singletonList(VolumeConfig.builder()
                .name("workdir")
                .path("/work-dir")
            .build()));

    // When
    InitContainerConfig initContainerConfig = initContainerConfigBuilder.build();

    // Then
    assertInitContainerConfig(initContainerConfig);
  }

  @Test
  void equalsAndHashCodeShouldMatch() {
    // Given
    InitContainerConfig  ic1 = InitContainerConfig.builder().name("init1").build();
    InitContainerConfig ic2 = InitContainerConfig.builder().name("init1").build();
    // When + Then
    assertThat(ic1)
        .isEqualTo(ic2)
        .hasSameHashCodeAs(ic2);
  }

  private void assertInitContainerConfig(InitContainerConfig initContainerConfig) {
    assertThat(initContainerConfig)
        .hasFieldOrPropertyWithValue("env", Collections.singletonMap("FOO", "BAR"))
        .hasFieldOrPropertyWithValue("name", "init1")
        .hasFieldOrPropertyWithValue("imageName", "busybox:latest")
        .hasFieldOrPropertyWithValue("imagePullPolicy", "IfNotPresent")
        .hasFieldOrPropertyWithValue("cmd", Arguments.builder().exec(Arrays.asList("sleep", "10")).build())
        .extracting(InitContainerConfig::getVolumes)
        .asList()
        .singleElement(InstanceOfAssertFactories.type(VolumeConfig.class))
        .hasFieldOrPropertyWithValue("name", "workdir")
        .hasFieldOrPropertyWithValue("path", "/work-dir");
  }
}
