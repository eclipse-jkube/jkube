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
package org.eclipse.jkube.kit.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class LazyBuilderTest {

  @Test
  void getShouldInvokeSupplierOnce() {
    // Given
    final AtomicInteger count = new AtomicInteger(0);
    final Supplier<Integer> build = () -> {
      count.incrementAndGet();
      return 1;
    };
    final LazyBuilder<Integer> lazyBuilder = new LazyBuilder<>(build);
    // When
    final int result = IntStream.rangeClosed(1, 10).map(t -> lazyBuilder.get()).sum();
    // Then
    assertThat(result).isEqualTo(10);
    assertThat(count.get()).isEqualTo(1);
  }

  @Test
  void getConcurrentShouldInvokeSupplierTwice() throws Exception {
    // Given
    final AtomicInteger count = new AtomicInteger(0);
    final CountDownLatch cdl = new CountDownLatch(1);
    final Supplier<Integer> build = () -> {
      try {
        if (count.incrementAndGet() == 1) {
          cdl.await(100, TimeUnit.MILLISECONDS);
          return 1337; // This value should be ignored, value set by main thread should be preferred in LazyBuilder
        }
      } catch (InterruptedException ignored) {}
      return 1;
    };
    final LazyBuilder<Integer> lazyBuilder = new LazyBuilder<>(build);
    final ExecutorService es = Executors.newSingleThreadExecutor();
    final Future<Integer> concurrentResult = es.submit(lazyBuilder::get);
    // When
    final int result = IntStream.rangeClosed(1, 10).map(t -> lazyBuilder.get()).sum();
    cdl.countDown();
    // Then
    assertThat(count.get()).isEqualTo(2);
    assertThat(result).isEqualTo(10);
    assertThat(concurrentResult.get(100, TimeUnit.MILLISECONDS)).isEqualTo(1);
  }


  @Nested
  @DisplayName("hasInstance")
  class HasInstance {

    @Test
    @DisplayName("with get never called should return false")
    void noGetShouldReturnFalse() {
      // When
      final LazyBuilder<Boolean> builder = new LazyBuilder<>(() -> true);
      // Then
      assertThat(builder).returns(false, LazyBuilder::hasInstance);
    }

    @Test
    @DisplayName("with get called should return true")
    void withGetShouldReturnTrue() {
      // When
      final LazyBuilder<Boolean> builder = new LazyBuilder<>(() -> true);
      builder.get();
      // Then
      assertThat(builder).returns(true, LazyBuilder::hasInstance);
    }
  }
}
