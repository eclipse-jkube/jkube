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
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.service.buildpacks.controller.BuildPackCliController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class BuildPackCliControllerTest {
  private KitLogger kitLogger;
  private BuildPackCliController buildPackCliController;
  private BuildPackBuildOptions buildOptions;
  private File pack;

  @BeforeEach
  void setUp() {
    kitLogger = spy(new KitLogger.SilentLogger());
    String validPackBinary = EnvUtil.isWindows() ? "pack.bat" : "pack";
    pack = new File(Objects.requireNonNull(getClass().getResource(String.format("/%s", validPackBinary))).getFile());
    buildPackCliController = new BuildPackCliController(pack, kitLogger);
    buildOptions = BuildPackBuildOptions.builder()
        .imageName("foo/bar:latest")
        .builderImage("foo/builder:base")
        .creationTime("now")
        .build();
  }

  @Nested
  @DisplayName("pack command succeeds")
  class CommandSucceeds {
    @Test
    @DisplayName("build, BuildPackBuildOptions passes as commandline arguments")
    void build_whenInvoked_thenBuildPackBuildOptionsPassedAsCommandLineArguments() {
      // When
      buildPackCliController.build(buildOptions);

      // Then
      verify(kitLogger).info("[[s]]%s", "build foo/bar:latest --builder foo/builder:base --creation-time now");
    }

    @Test
    @DisplayName("version, should get pack version")
    void version() {
      // When
      String version = buildPackCliController.version();

      // Then
      assertThat(version).isEqualTo("0.32.1+git-b14250b.build-5241");
    }
  }

  @Nested
  @DisplayName("pack command fails")
  class CommandFails {
    @BeforeEach
    void setUp() {
      String invalidPackBinary = EnvUtil.isWindows() ? "invalid-pack.bat" : "invalid-pack";
      pack = new File(Objects.requireNonNull(BuildPackCliControllerTest.class.getResource(String.format("/%s", invalidPackBinary))).getFile());
      buildPackCliController = new BuildPackCliController(pack, kitLogger);
    }

    @Test
    @DisplayName("build, throws exception")
    void build_whenCommandFailed_thenThrowException() {
      // When + Then
      assertThatIllegalStateException()
          .isThrownBy(() -> buildPackCliController.build(buildOptions))
          .withMessageContaining("Process Existed With : 1");
    }

    @Test
    @DisplayName("version, returns null on failure")
    void version_whenCommandFailed_thenReturnNull() {
      // When
      String version = buildPackCliController.version();

      // Then
      assertThat(version).isNull();
    }
  }
}
