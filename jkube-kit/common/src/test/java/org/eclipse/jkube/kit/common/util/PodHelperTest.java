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
package org.eclipse.jkube.kit.common.util;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PodHelperTest {

  @Test
  void firstContainerHasEnvVarsWithoutContainedEnvVarsShouldReturnFalse() {
    // Given
    final Pod pod = initPod(
        Collections.singletonList(envVar("VAR1", "VAL1")),
        Arrays.asList(envVar("VAR1", "VAL1"), envVar("VAR2", "VAL2"))
    );
    final Map<String, String> desiredVars = new HashMap<>();
    desiredVars.put("VAR1", "VAL1");
    desiredVars.put("VAR2", "VAL2");
    // When
    final boolean result = PodHelper.firstContainerHasEnvVars(pod, desiredVars);
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void firstContainerHasEnvVarsWithContainedEnvVarsShouldReturnTrue() {
    // Given
    final Pod pod = initPod(
        Arrays.asList(envVar("VAR1", "VAL1"), envVar("VAR2", "VAL2")),
        Arrays.asList(envVar("VAR1", "VAL1"), envVar("VAR3", "VAL3"))
    );
    final Map<String, String> desiredVars = new HashMap<>();
    desiredVars.put("VAR1", "VAL1");
    desiredVars.put("VAR2", "VAL2");
    // When
    final boolean result = PodHelper.firstContainerHasEnvVars(pod, desiredVars);
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void firstContainerHasEnvVarsWithEmptyPodShouldReturnFalse() {
    // Given
    final Pod pod = new Pod();
    final Map<String, String> desiredVars = Collections.singletonMap("VAR1", "VAL1");
    // When
    final boolean result = PodHelper.firstContainerHasEnvVars(pod, desiredVars);
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void firstContainerHasEnvVarsWithEmptyPodSpecShouldReturnFalse() {
    // Given
    final Pod pod = new PodBuilder().withNewSpec().endSpec().build();
    final Map<String, String> desiredVars = Collections.singletonMap("VAR1", "VAL1");
    // When
    final boolean result = PodHelper.firstContainerHasEnvVars(pod, desiredVars);
    // Then
    assertThat(result).isFalse();
  }

  private static EnvVar envVar(String name, String value) {
    return new EnvVarBuilder().withName(name).withValue(value).build();
  }

  private static Pod initPod(
      Collection<EnvVar> firstContainerEnvVars,
      Collection<EnvVar> secondContainerEnvVars
  ) {
    return new PodBuilder()
        .withNewSpec()
        .addToContainers(new ContainerBuilder()
            .addAllToEnv(firstContainerEnvVars)
            .build())
        .addToContainers(new ContainerBuilder()
            .addAllToEnv(secondContainerEnvVars)
            .build())
        .endSpec()
        .build();
  }
}