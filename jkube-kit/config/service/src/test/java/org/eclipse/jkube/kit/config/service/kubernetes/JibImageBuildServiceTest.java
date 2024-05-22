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
package org.eclipse.jkube.kit.config.service.kubernetes;

import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JibImageBuildServiceTest {

  private JKubeServiceHub jKubeServiceHub;

  @BeforeEach
  void setUp() {
    jKubeServiceHub = JKubeServiceHub.builder()
      .log(new KitLogger.SilentLogger())
      .platformMode(RuntimeMode.KUBERNETES)
      .buildServiceConfig(BuildServiceConfig.builder().build())
      .configuration(JKubeConfiguration.builder().build())
      .build();
  }

  @Test
  void isApplicable_withNoBuildStrategy_shouldReturnFalse() {
    // When
    final boolean result = new JibImageBuildService(jKubeServiceHub).isApplicable();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isApplicable_withJibBuildStrategy_shouldReturnTrue() {
    // Given
    jKubeServiceHub = jKubeServiceHub.toBuilder()
      .buildServiceConfig(BuildServiceConfig.builder()
        .jKubeBuildStrategy(JKubeBuildStrategy.jib)
        .build())
      .build();
    // When
    final boolean result = new JibImageBuildService(jKubeServiceHub).isApplicable();
    // Then
    assertThat(result).isTrue();
  }
}
