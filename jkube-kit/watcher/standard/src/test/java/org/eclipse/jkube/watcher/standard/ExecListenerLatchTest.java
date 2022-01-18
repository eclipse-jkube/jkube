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
package org.eclipse.jkube.watcher.standard;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class ExecListenerLatchTest {

  @Test
  public void onOpenWithConsumer() {
    // Given
    final AtomicBoolean consumed = new AtomicBoolean(false);
    final ExecListenerLatch ell = new ExecListenerLatch(() -> consumed.set(true));
    // When
    ell.onOpen();
    // Then
    assertThat(consumed.get()).isTrue();
  }

  @Test
  public void onCloseShouldSetCodeAndReason() throws InterruptedException {
    // Given
    final ExecListenerLatch ell = new ExecListenerLatch();
    // When
    ell.onClose(1337, "Closed");
    // Then
    assertThat(ell)
        .hasFieldOrPropertyWithValue("closeCode", 1337)
        .hasFieldOrPropertyWithValue("closeReason", "Closed");
  }

  @Test
  public void onCloseShouldCountDown() throws InterruptedException {
    // Given
    final ExecListenerLatch ell = new ExecListenerLatch();
    // When
    ell.onClose(-1, null);
    // Then
    assertThat(ell.await(0, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void onFailureShouldCountDown() throws InterruptedException {
    // Given
    final ExecListenerLatch ell = new ExecListenerLatch();
    // When
    ell.onFailure(null, null);
    // Then
    assertThat(ell.await(0, TimeUnit.SECONDS)).isTrue();
  }
}
