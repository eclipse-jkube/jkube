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
package org.eclipse.jkube.kit.config.service.portforward;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodConditionBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;
import io.fabric8.kubernetes.client.Watcher;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PortForwardPodWatcherTest {

  @Mock
  private KitLogger logger;
  private PortForwardPodWatcher portForwardPodWatcher;

  @BeforeEach
  void setUp() {
    portForwardPodWatcher = new PortForwardPodWatcher(logger, Collections.singletonMap("SIMPLE_KEY", "1337"));
  }

  @Test
  void eventReceivedWithNotMatchingAction() {
    // When
    portForwardPodWatcher.eventReceived(Watcher.Action.DELETED, new Pod());
    // Then
    assertThat(portForwardPodWatcher)
        .hasFieldOrPropertyWithValue("foundPod.get", null)
        .hasFieldOrPropertyWithValue("podReadyLatch.count", 1L);
  }

  @Test
  void eventReceivedWithNotMatchingPod() {
    // When
    portForwardPodWatcher.eventReceived(Watcher.Action.ADDED, new Pod());
    // Then
    assertThat(portForwardPodWatcher)
        .hasFieldOrPropertyWithValue("foundPod.get", null)
        .hasFieldOrPropertyWithValue("podReadyLatch.count", 1L);
  }

  @Test
  void eventReceivedWithNotMatchingLabels() {
    // When
    portForwardPodWatcher.eventReceived(Watcher.Action.ADDED, initPod(Collections.singletonMap("NOT", "MATCHING")));
    // Then
    assertThat(portForwardPodWatcher)
        .hasFieldOrPropertyWithValue("podReadyLatch.count", 1L)
        .extracting(PortForwardPodWatcher::getFoundPod)
        .isNull();
  }

  @Test
  void eventReceivedWithMatchingLabels() {
    // When
    portForwardPodWatcher.eventReceived(Watcher.Action.ADDED, initPod(Collections.singletonMap("SIMPLE_KEY", "1337")));
    // Then
    assertThat(portForwardPodWatcher)
        .hasFieldOrPropertyWithValue("podReadyLatch.count", 0L)
        .extracting(PortForwardPodWatcher::getFoundPod)
        .isNotNull();
  }

  private static Pod initPod(Map<String, String> envVars) {
    return new PodBuilder()
        .withNewMetadata().withName("test-pod").endMetadata()
        .withSpec(new PodSpecBuilder()
            .addToContainers(new ContainerBuilder()
                .withEnv(toEnvVar(envVars))
                .build())
            .build())
        .withStatus(new PodStatusBuilder()
            .withPhase("Running")
            .addToConditions(new PodConditionBuilder()
                .withType("ready")
                .withStatus("true")
                .build())
            .build())
        .build();
  }

  private static List<EnvVar> toEnvVar(Map<String, String> envVars) {
    return envVars.entrySet().stream()
        .map(e -> new EnvVarBuilder().withName(e.getKey()).withValue(e.getValue()).build())
        .collect(Collectors.toList());
  }
}