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
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class BuildPackCliControllerTest {
  private KitLogger kitLogger;
  private BuildPackCliController buildPackCliController;
  private BuildPackBuildOptions buildOptions;
  private File pack;

  @TempDir
  private File temporaryFolder;

  @BeforeEach
  void setUp() throws URISyntaxException {
    kitLogger = spy(new KitLogger.SilentLogger());
    String validPackBinary = EnvUtil.isWindows() ? "pack.bat" : "pack";
    pack = resourceAsFile(String.format("/%s", validPackBinary));
    buildPackCliController = new BuildPackCliController(pack, kitLogger);
    buildOptions = BuildPackBuildOptions.builder()
        .imageName("foo/bar:latest")
        .builderImage("foo/builder:base")
        .creationTime("now")
        .path(temporaryFolder.getAbsolutePath())
        .build();
  }

  private File resourceAsFile(String resourcePath) throws URISyntaxException {
    URL url = Objects.requireNonNull(getClass().getResource(resourcePath));
    return new File(url.toURI());
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
      verify(kitLogger).info("[[s]]%s", "build foo/bar:latest --builder foo/builder:base --creation-time now --path " + temporaryFolder.getAbsolutePath());
    }

    @Test
    @DisplayName("build, with additional BuildPackBuildOptions passes as commandline arguments")
    void build_whenInvokedWithMoreBuildOptions_thenOptionsPassedAsCommandLineArguments() {
      // Given
      buildOptions = buildOptions.toBuilder()
          .volumes(Collections.singletonList("/tmp/volume:/platform/volume:ro"))
          .tags(Arrays.asList("t1", "t2", "t3"))
          .imagePullPolicy("if-not-present")
          .env(Collections.singletonMap("BP_SPRING_CLOUD_BINDINGS_DISABLED", "true"))
          .clearCache(true)
          .build();
      // When
      buildPackCliController.build(buildOptions);
      // Then
      verify(kitLogger).info("[[s]]%s", "build foo/bar:latest --builder foo/builder:base --creation-time now --pull-policy if-not-present --volume /tmp/volume:/platform/volume:ro --tag foo/bar:t1 --tag foo/bar:t2 --tag foo/bar:t3 --env BP_SPRING_CLOUD_BINDINGS_DISABLED=true --clear-cache --path " + temporaryFolder.getAbsolutePath());
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
    void setUp() throws URISyntaxException {
      String invalidPackBinary = EnvUtil.isWindows() ? "invalid-pack.bat" : "invalid-pack";
      pack = resourceAsFile(String.format("/%s", invalidPackBinary));
      buildPackCliController = new BuildPackCliController(pack, kitLogger);
    }

    @Test
    @DisplayName("build, throws exception without logging to error when stderr is blank")
    void build_whenCommandFailed_thenThrowException() {
      // When + Then
      assertThatIllegalStateException()
          .isThrownBy(() -> buildPackCliController.build(buildOptions))
          .withMessageContaining("Process Existed With : 1");
      // No spurious [ERROR] line when pack writes nothing to stderr
      verify(kitLogger, never()).error(anyString(), (Object) any());
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

  @Nested
  @DisplayName("pack command fails with stderr output")
  class CommandFailsWithStderr {
    @BeforeEach
    void setUp() throws URISyntaxException {
      String invalidPackBinaryWithError = EnvUtil.isWindows() ? "invalid-pack-with-error.bat" : "invalid-pack-with-error";
      pack = resourceAsFile(String.format("/%s", invalidPackBinaryWithError));
      buildPackCliController = new BuildPackCliController(pack, kitLogger);
    }

    @Test
    @DisplayName("build, logs stderr via kitLogger.error when pack CLI writes to stderr")
    void build_whenCommandFailedWithStderr_thenStderrLoggedViaKitLogger() {
      // When + Then
      assertThatIllegalStateException()
          .isThrownBy(() -> buildPackCliController.build(buildOptions))
          .withMessageContaining("Process Existed With : 1");

      // Capture the argument passed to kitLogger.error to verify stderr content.
      // Using contains() rather than exact equality keeps the assertion robust across
      // environments (locale warnings, line-separator differences) and locks in the
      // "%s" format-string wrapper that prevents UnknownFormatConversionException on
      // pack output containing literal % characters.
      ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
      verify(kitLogger).error(anyString(), captor.capture());
      String logged = (String) captor.getValue();
      assertThat(logged).contains("builder image not found");
      assertThat(logged).contains("100%");
    }
  }
}
