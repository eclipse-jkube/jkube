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
package org.eclipse.jkube.kit.config.service.portforward;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import org.eclipse.jkube.kit.common.KitLogger;

import io.fabric8.kubernetes.client.LocalPortForward;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PortForwardTaskTest {
  private NamespacedKubernetesClient kubernetesClient;
  private LocalPortForward localPortForward;
  private CountDownLatch mockedCountDownLatch;
  private PortForwardTask portForwardTask;

  @BeforeEach
  void setUp() {
    localPortForward = mock(LocalPortForward.class);
    mockedCountDownLatch = mock(CountDownLatch.class);
    kubernetesClient = mock(NamespacedKubernetesClient.class);
    portForwardTask = new PortForwardTask(
        kubernetesClient, "pod-name", localPortForward, new KitLogger.SilentLogger(), mockedCountDownLatch);
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void run() throws Exception {
    // Given
    MixedOperation mixedOperation = mock(MixedOperation.class);
    Watch watch = mock(Watch.class);
    when(kubernetesClient.pods()).thenReturn(mixedOperation);
    when(mixedOperation.watch(any())).thenReturn(watch);

    // When
    portForwardTask.run();
    // Then
    verify(mockedCountDownLatch).await();
    verify(localPortForward).close();
  }

  @Test
  void close() throws IOException {
    // When
    portForwardTask.close();
    // Then
    verify(localPortForward,times(1)).close();
  }
}
