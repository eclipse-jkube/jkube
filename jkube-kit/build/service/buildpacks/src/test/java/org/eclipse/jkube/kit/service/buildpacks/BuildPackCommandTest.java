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
package org.eclipse.jkube.kit.service.buildpacks;

import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class BuildPackCommandTest {
  private KitLogger kitLogger;
  private File packCli;

  @BeforeEach
  void setUp() {
    kitLogger = new KitLogger.SilentLogger();
    packCli = new File(Objects.requireNonNull(getClass().getResource("/pack")).getFile());
  }

  @Test
  void getArgs() {
    // Given
    BuildPackCommand buildPackCommand = new BuildPackCommand(kitLogger, packCli, Collections.singletonList("--version"), s -> {});

    // When + Then
    assertThat(buildPackCommand.getArgs())
        .containsExactly(packCli.getAbsolutePath(), "--version");
  }

  @Test
  void processLine_whenInvoked_shouldSetVersion() {
    // Given
    AtomicReference<String> version = new AtomicReference<>();
    BuildPackCommand buildPackCommand = new BuildPackCommand(kitLogger, packCli, Collections.singletonList("--version"), version::set);
    // When
    buildPackCommand.processLine("0.30.0");
    // Then
    assertThat(version.get()).isEqualTo("0.30.0");
  }

  @Test
  void processError_whenInvoked_shouldSetError() {
    // Given
    AtomicReference<String> version = new AtomicReference<>();
    BuildPackCommand buildPackCommand = new BuildPackCommand(kitLogger, packCli, Collections.singletonList("--version"), version::set);
    // When
    buildPackCommand.processError("Failure in running pack Cli");
    // Then
    assertThat(buildPackCommand.getError()).isEqualTo("Failure in running pack Cli" + System.lineSeparator());
  }

  @Test
  @DisplayName("processError is invoked from the stderr pump thread while the caller thread may "
      + "read getError(), so the accumulated error must not lose or interleave lines")
  void processError_whenInvokedConcurrently_shouldNotLoseOrCorruptLines() throws InterruptedException {
    // Given
    AtomicReference<String> version = new AtomicReference<>();
    BuildPackCommand buildPackCommand = new BuildPackCommand(kitLogger, packCli, Collections.singletonList("--version"), version::set);
    String line = "ERROR: failed with status code: 2";
    int threads = 8;
    int linesPerThread = 500;
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(threads);
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    // When
    try {
      for (int t = 0; t < threads; t++) {
        executor.submit(() -> {
          start.await();
          for (int i = 0; i < linesPerThread; i++) {
            buildPackCommand.processError(line);
          }
          done.countDown();
          return null;
        });
      }
      start.countDown();
      assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
    } finally {
      executor.shutdownNow();
    }
    // Then
    assertThat(buildPackCommand.getError().split(System.lineSeparator()))
        .hasSize(threads * linesPerThread)
        .containsOnly(line);
  }

  @Test
  void processError_whenInvokedForEachLine_shouldPreserveLineBoundaries() {
    // Given
    AtomicReference<String> version = new AtomicReference<>();
    BuildPackCommand buildPackCommand = new BuildPackCommand(kitLogger, packCli, Collections.singletonList("--version"), version::set);
    // When
    buildPackCommand.processError("ERROR: failed to build: executing lifecycle");
    buildPackCommand.processError("ERROR: failed with status code: 2");
    // Then
    assertThat(buildPackCommand.getError())
        .isEqualTo("ERROR: failed to build: executing lifecycle" + System.lineSeparator()
            + "ERROR: failed with status code: 2" + System.lineSeparator());
  }
}
