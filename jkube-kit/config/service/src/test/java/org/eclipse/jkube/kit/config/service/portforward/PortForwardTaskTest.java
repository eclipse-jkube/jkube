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

import static org.mockito.Mockito.mock;
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
    CountDownLatch cdl = mock(CountDownLatch.class);
    // When
    portForwardTask.run();
    // Then
    verify(cdl,times(1)).await();
    verify(localPortForward,times(1)).close();
  }

  @Test
  void close() throws IOException {
    // When
    portForwardTask.close();
    // Then
    verify(localPortForward,times(1)).close();
  }
}
