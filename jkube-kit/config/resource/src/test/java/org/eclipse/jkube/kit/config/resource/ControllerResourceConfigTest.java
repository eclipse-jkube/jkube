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

class ControllerResourceConfigTest {
  @Test
  void rawDeserialization() throws IOException {
    // Given
    final ObjectMapper mapper = new ObjectMapper();
    // When
    final ControllerResourceConfig result = mapper.readValue(
        getClass().getResourceAsStream("/controller-config.json"),
        ControllerResourceConfig.class);
    // Then
    assertControllerResourceConfig(result);
  }

  @Test
  void builder() {
    // Given
    ControllerResourceConfig.ControllerResourceConfigBuilder initContainerConfigBuilder = ControllerResourceConfig.builder()
        .env(Collections.singletonMap("KEY1", "VALUE1"))
        .controllerName("test-controller")
        .containerPrivileged(false)
        .imagePullPolicy("IfNotPresent")
        .replicas(3)
        .restartPolicy("OnFailure")
        .liveness(ProbeConfig.builder()
            .getUrl("http://:8080/q/health")
            .initialDelaySeconds(3)
            .timeoutSeconds(3)
            .build())
        .readiness(ProbeConfig.builder()
            .getUrl("http://:8080/q/health")
            .initialDelaySeconds(3)
            .timeoutSeconds(3)
            .build())
        .startup(ProbeConfig.builder()
            .getUrl("http://:8080/q/health")
            .initialDelaySeconds(3)
            .timeoutSeconds(3)
            .build())
        .initContainer(InitContainerConfig.builder()
            .env(Collections.singletonMap("FOO", "BAR"))
            .name("init1")
            .imageName("busybox:latest")
            .imagePullPolicy("IfNotPresent")
            .cmd(Arguments.builder().exec(Arrays.asList("sleep", "10")).build())
            .volumes(Collections.singletonList(VolumeConfig.builder()
                .name("workdir")
                .path("/work-dir")
                .build())).build())
        .volumes(Collections.singletonList(VolumeConfig.builder()
            .name("workdir")
            .type("emptyDir")
            .path("/work-dir")
            .build()));

    // When
    ControllerResourceConfig controllerResourceConfig = initContainerConfigBuilder.build();

    // Then
    assertControllerResourceConfig(controllerResourceConfig);
  }

  @Test
  void equalsAndHashCodeShouldMatch() {
    // Given
    ControllerResourceConfig  c1 = ControllerResourceConfig.builder().controllerName("test-controller").build();
    ControllerResourceConfig c2 = ControllerResourceConfig.builder().controllerName("test-controller").build();
    // When + Then
    assertThat(c1)
        .isEqualTo(c2)
        .hasSameHashCodeAs(c2);
  }

  private void assertControllerResourceConfig(ControllerResourceConfig controllerResourceConfig) {
    assertThat(controllerResourceConfig)
        .hasFieldOrPropertyWithValue("env", Collections.singletonMap("KEY1", "VALUE1"))
        .hasFieldOrPropertyWithValue("controllerName", "test-controller")
        .hasFieldOrPropertyWithValue("imagePullPolicy", "IfNotPresent")
        .hasFieldOrPropertyWithValue("containerPrivileged", false)
        .hasFieldOrPropertyWithValue("replicas", 3)
        .hasFieldOrPropertyWithValue("restartPolicy", "OnFailure")
        .satisfies(c -> assertProbe(c.getLiveness()))
        .satisfies(c -> assertProbe(c.getReadiness()))
        .satisfies(c -> assertProbe(c.getStartup()))
        .satisfies(c -> assertThat(c.getInitContainers())
            .singleElement(InstanceOfAssertFactories.type(InitContainerConfig.class))
            .hasFieldOrPropertyWithValue("env", Collections.singletonMap("FOO", "BAR"))
            .hasFieldOrPropertyWithValue("name", "init1")
            .hasFieldOrPropertyWithValue("imageName", "busybox:latest")
            .hasFieldOrPropertyWithValue("imagePullPolicy", "IfNotPresent")
            .hasFieldOrPropertyWithValue("cmd", Arguments.builder().exec(Arrays.asList("sleep", "10")).build())
            .extracting(InitContainerConfig::getVolumes)
            .asList()
            .singleElement(InstanceOfAssertFactories.type(VolumeConfig.class))
            .hasFieldOrPropertyWithValue("name", "workdir")
            .hasFieldOrPropertyWithValue("path", "/work-dir"))
        .satisfies(c -> assertThat(c.getVolumes())
            .singleElement(InstanceOfAssertFactories.type(VolumeConfig.class))
            .hasFieldOrPropertyWithValue("name", "workdir")
            .hasFieldOrPropertyWithValue("type", "emptyDir")
            .hasFieldOrPropertyWithValue("path", "/work-dir"));
  }

  private void assertProbe(ProbeConfig probeConfig) {
    assertThat(probeConfig)
        .hasFieldOrPropertyWithValue("getUrl", "http://:8080/q/health")
        .hasFieldOrPropertyWithValue("initialDelaySeconds", 3)
        .hasFieldOrPropertyWithValue("timeoutSeconds", 3);
  }
}
