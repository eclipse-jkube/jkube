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
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.Objects;
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
    assertThat(buildPackCommand.getError()).isEqualTo("Failure in running pack Cli");
  }
}
