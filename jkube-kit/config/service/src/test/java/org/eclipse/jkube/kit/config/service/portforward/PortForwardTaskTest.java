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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import org.eclipse.jkube.kit.common.KitLogger;

import io.fabric8.kubernetes.client.LocalPortForward;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PortForwardTaskTest {
  @Mock
  private NamespacedKubernetesClient kubernetesClient;
  @Mock
  private LocalPortForward localPortForward;
  @Mock
  private KitLogger logger;
  private PortForwardTask portForwardTask;

  @BeforeEach
  void setUp() {
    portForwardTask = new PortForwardTask(
        kubernetesClient, "pod-name", localPortForward, logger);
  }

  @Test
  void run() throws Exception {
    try (MockedConstruction<CountDownLatch> mc = mockConstruction(CountDownLatch.class)) {
      // Given
      // When
      // Then
      assertThat(mc.constructed()).hasSize(1);
      verify(mc.constructed().iterator().next(), times(1)).await();
      verify(localPortForward, times(1)).close();
    }
  }

  @Test
  public void close() throws IOException {
    // Given
    // When
    portForwardTask.close();
    // Then
    verify(localPortForward,times(1)).close();
  }
}
