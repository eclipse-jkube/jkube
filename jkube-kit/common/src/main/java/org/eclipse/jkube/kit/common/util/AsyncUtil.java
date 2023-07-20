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

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class AsyncUtil {

  private AsyncUtil() {}

  // Initialization on demand
  private static class ExecutorServiceHolder {
    public static final ExecutorService INSTANCE = Executors.newCachedThreadPool();
  }

  public static <T> CompletableFuture<T> async(Callable<T> callable) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    CompletableFuture.runAsync(() -> {
      try {
        future.complete(callable.call());
      } catch (Exception ex) {
        future.completeExceptionally(ex);
      }
    }, ExecutorServiceHolder.INSTANCE);
    future.whenComplete((result, throwable) -> {
      if (!future.isDone()) {
        future.cancel(true);
      }
    });
    return future;
  }

  public static <T> Function<Predicate<T>, CompletableFuture<T>> await(Supplier<T> supplier) {
    return predicate -> async(() -> {
      T ret;
      while(!predicate.test(ret = supplier.get())) {
        Thread.sleep(100L);
      }
      return ret;
    });
  }

  public static <T> T get(CompletableFuture<T> completableFuture, Duration duration) {
    try {
      return completableFuture.get(duration.toMillis(), TimeUnit.MILLISECONDS);
    } catch (ExecutionException e) {
      throw new IllegalStateException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    } catch (TimeoutException e) {
      throw new IllegalStateException("Failure while waiting to get future ", e);
    }
  }
}
