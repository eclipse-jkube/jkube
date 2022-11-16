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

import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PortForwardMonitorTest {

  private KitLogger logger;

  @Test
  void eventReceivedDoesNothing() {
    logger = new KitLogger.SilentLogger();
    // Given
    final CountDownLatch cdl = new CountDownLatch(1);
    // When
    new PortForwardMonitor(logger, "pod-name", cdl)
        .eventReceived(null, new PodBuilder().withNewMetadata().endMetadata().build());
    // Then
    assertThat(cdl).hasFieldOrPropertyWithValue("count", 1L);
  }

  @Test
  void eventReceivedForDeletionCloses() {
    logger = mock(KitLogger.class);
    // Given
    final CountDownLatch cdl = new CountDownLatch(1);
    // When
    new PortForwardMonitor(logger, "pod-name", cdl)
        .eventReceived(Watcher.Action.DELETED, new PodBuilder().withNewMetadata().withName("pod-name").endMetadata().build());
    // Then
    assertThat(cdl).hasFieldOrPropertyWithValue("count", 0L);
  }

  @Test
  void eventReceivedForDeletedPodCloses() {
    logger = mock(KitLogger.class);
    // Given
    final CountDownLatch cdl = new CountDownLatch(1);
    // When
    new PortForwardMonitor(logger, "pod-name", cdl)
        .eventReceived(Watcher.Action.MODIFIED, new PodBuilder().withNewMetadata()
            .withName("pod-name").withDeletionTimestamp("2077").endMetadata().build());
    // Then
    assertThat(cdl).hasFieldOrPropertyWithValue("count", 0L);
  }

  @Test
  void closeCountsDown() {
    logger = mock(KitLogger.class);
    // Given
    final CountDownLatch cdl = new CountDownLatch(1);
    // When
    new PortForwardMonitor(logger, "pod-name", cdl).onClose();
    // Then
    assertThat(cdl).hasFieldOrPropertyWithValue("count", 0L);
  }

  @Test
  void closeWithExceptionCountsDown() {
    logger = mock(KitLogger.class);
    // Given
    final CountDownLatch cdl = new CountDownLatch(1);
    // When
    new PortForwardMonitor(logger, "pod-name", cdl).onClose(new WatcherException("Closed for the season"));
    // Then
    assertThat(cdl).hasFieldOrPropertyWithValue("count", 0L);
  }
}
