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
package org.eclipse.jkube.kit.config.service.ingresscontroller;

import org.eclipse.jkube.kit.common.IngressControllerDetector;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class IngressControllerDetectorManagerTest {
  private KitLogger logger;

  @BeforeEach
  void setUp() {
    logger = spy(new KitLogger.SilentLogger());
  }

  @Test
  void detect_whenIngressControllerDetected_thenReturnTrue() {
    // Given
    IngressControllerDetector ingressControllerDetector = mock(IngressControllerDetector.class);
    when(ingressControllerDetector.hasPermissions()).thenReturn(true);
    when(ingressControllerDetector.isDetected()).thenReturn(true);

    // When
    boolean result = new IngressControllerDetectorManager(logger, Collections.singletonList(ingressControllerDetector))
      .detect();

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void detect_whenIngressControllerNotDetected_thenReturnFalseAndLogWarning() {
    // Given
    IngressControllerDetector ingressControllerDetector = mock(IngressControllerDetector.class);
    when(ingressControllerDetector.hasPermissions()).thenReturn(true);
    when(ingressControllerDetector.isDetected()).thenReturn(false);

    // When
    boolean result = new IngressControllerDetectorManager(logger, Collections.singletonList(ingressControllerDetector))
      .detect();

    // Then
    assertThat(result).isFalse();
    verify(logger).warn("Ingress resources applied. However, no IngressController seems to be running at the moment, your service will most likely be not accessible.");
  }

  @Test
  void detect_whenNoPermissionToCheck_thenReturnFalse() {
    // Given
    IngressControllerDetector ingressControllerDetector = mock(IngressControllerDetector.class);
    when(ingressControllerDetector.hasPermissions()).thenReturn(false);

    // When
    boolean result = new IngressControllerDetectorManager(logger, Collections.singletonList(ingressControllerDetector))
      .detect();

    // Then
    assertThat(result).isFalse();
    verify(logger, times(0)).warn(anyString());
  }
}
