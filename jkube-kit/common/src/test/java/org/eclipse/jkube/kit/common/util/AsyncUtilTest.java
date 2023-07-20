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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class AsyncUtilTest {
  @Test
  void get_whenFuturePresent_thenReturnValue() {
    // Given
    CompletableFuture<String> completableFuture = CompletableFuture.completedFuture("foo");

    // When
    String result = AsyncUtil.get(completableFuture, Duration.ofMinutes(1));

    // Then
    assertThat(result).isEqualTo("foo");
  }

  @Test
  void get_whenFutureCompletedExceptionally_then() {
    // Given
    CompletableFuture<String> completableFuture = new CompletableFuture<>();
    completableFuture.completeExceptionally(new IOException("io exception"));

    // When
    // Then
    assertThatIllegalStateException()
        .isThrownBy(() -> AsyncUtil.get(completableFuture, Duration.ofMinutes(1)))
        .withMessageContaining("io exception");
  }
}
