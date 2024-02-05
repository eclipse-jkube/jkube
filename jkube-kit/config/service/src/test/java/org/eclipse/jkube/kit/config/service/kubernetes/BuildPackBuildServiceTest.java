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
package org.eclipse.jkube.kit.config.service.kubernetes;

import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.TestHttpBuildPacksArtifactsServer;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class BuildPackBuildServiceTest {

  private static final String TEST_PACK_VERSION = "v0.32.1";
  private KitLogger kitLogger;
  private JKubeServiceHub jKubeServiceHub;
  private ImageConfiguration imageConfiguration;
  private BuildServiceConfig buildServiceConfig;

  @TempDir
  private File temporaryFolder;

  @BeforeEach
  void setUp() {
    kitLogger = spy(new KitLogger.SilentLogger());
    buildServiceConfig = BuildServiceConfig.builder()
        .jKubeBuildStrategy(JKubeBuildStrategy.buildpacks)
        .build();
    jKubeServiceHub = JKubeServiceHub.builder()
        .log(kitLogger)
        .platformMode(RuntimeMode.KUBERNETES)
        .buildServiceConfig(buildServiceConfig)
        .configuration(JKubeConfiguration.builder().build())
        .build();
    imageConfiguration = ImageConfiguration.builder()
        .name("foo/bar:latest")
        .build(BuildConfiguration.builder()
            .from("foo/base:latest")
            .build())
        .build();
    Map<String, String> properties = new HashMap<>();
    properties.put("user.home", temporaryFolder.getAbsolutePath());
    properties.put("os.name", System.getProperty("os.name"));
    properties.put("os.arch", System.getProperty("os.arch"));
    Map<String, String> env = new HashMap<>();
    env.put("HOME", temporaryFolder.getAbsolutePath());
    env.put("PATH", temporaryFolder.toPath().resolve("bin").toFile().getAbsolutePath());
    EnvUtil.overrideEnvGetter(env::get);
    EnvUtil.overridePropertyGetter(properties::get);
  }

  @AfterEach
  void tearDown() {
    EnvUtil.overrideEnvGetter(System::getenv);
    EnvUtil.overridePropertyGetter(System::getProperty);
  }

  @ParameterizedTest
  @CsvSource({
      "s2i,false", "jib,false", "docker,false", "buildpacks,true"
  })
  void isApplicable_withGivenStrategy_shouldReturnTrueOnlyForBuildPackStrategy(String buildStrategyValue, boolean expectedResult) {
    // Given
    jKubeServiceHub = jKubeServiceHub.toBuilder()
        .buildServiceConfig(buildServiceConfig.toBuilder()
            .jKubeBuildStrategy(JKubeBuildStrategy.valueOf(buildStrategyValue))
            .build())
        .build();
    // When
    final boolean result = new BuildPackBuildService(jKubeServiceHub).isApplicable();
    // Then
    assertThat(result).isEqualTo(expectedResult);
  }


  @Nested
  @DisplayName("buildImage")
  class BuildImage {
    private TestHttpBuildPacksArtifactsServer server;
    private BuildPackBuildService buildPackBuildService;

    @BeforeEach
    void setUp() {
      server = new TestHttpBuildPacksArtifactsServer();
      Properties packProperties = new Properties();
      packProperties.put("version", TEST_PACK_VERSION);
      packProperties.put("windows.binary-extension", "bat");
      buildPackBuildService = new BuildPackBuildService(jKubeServiceHub, packProperties);
      packProperties.put("linux.artifact", server.getLinuxArtifactUrl());
      packProperties.put("linux-arm64.artifact", server.getLinuxArm64ArtifactUrl());
      packProperties.put("macos.artifact", server.getMacosArtifactUrl());
      packProperties.put("macos-arm64.artifact", server.getMacosArm64ArtifactUrl());
      packProperties.put("windows.artifact", server.getWindowsArtifactUrl());
      packProperties.put("windows.binary-extension", "bat");
    }

    @AfterEach
    void tearDown() throws IOException {
      server.close();
    }

    @Nested
    @DisplayName("local .pack/config.toml exists")
    class LocalPackConfigExists {
      private File localPackConfig;

      @BeforeEach
      void setUp() throws IOException {
        File packHome = new File(temporaryFolder, ".pack");
        Files.createDirectory(packHome.toPath());
        localPackConfig = new File(packHome, "config.toml");
      }

      @Test
      @DisplayName("When default builder configured in .pack/config.toml, then use that builder image")
      void whenLocalPackConfigHasDefaultBuilderSet_thenUseThatBuilder() throws IOException {
        // Given
        Files.write(localPackConfig.toPath(), String.format("default-builder-image=\"%s\"", "cnbs/sample-builder:bionic").getBytes());

        // When
        buildPackBuildService.buildSingleImage(imageConfiguration);

        // Then
        verify(kitLogger).info("[[s]]%s","build foo/bar:latest --builder cnbs/sample-builder:bionic --creation-time now");
      }

      @Test
      @DisplayName("When .pack/config.toml invalid, then use opinionated builder image")
      void whenLocalPackConfigInvalid_thenUseOpinionatedBuilderImage() throws IOException {
        // Given
        Files.write(localPackConfig.toPath(), "default-builder-image@@=".getBytes());

        // When
        buildPackBuildService.buildSingleImage(imageConfiguration);

        // Then
        verify(kitLogger).info("[[s]]%s","build foo/bar:latest --builder paketobuildpacks/builder:base --creation-time now");
      }
    }

    @Nested
    @DisplayName("Local .pack/config.toml absent")
    class LocalPackConfigAbsent {
      @Test
      @DisplayName("use opinionated builder image")
      void whenLocalPackCLIAndNoDefaultBuilderInPackConfig_thenUseOpinionatedBuilderImage() {
        // When
        buildPackBuildService.buildSingleImage(imageConfiguration);

        // Then
        verify(kitLogger).info("[[s]]%s", "build foo/bar:latest --builder paketobuildpacks/builder:base --creation-time now");
      }
    }
  }
}
