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
package org.eclipse.jkube.kit.resource.helm;

import com.marcnuri.helm.Helm;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("HelmService.dependencyUpdate")
class HelmServiceDependencyUpdateIT {
  @TempDir
  private Path tempDir;
  private HelmConfig helmConfig;
  private HelmService helmService;
  private KitLogger kitLogger;

  @BeforeEach
  void setUp() throws URISyntaxException {
    kitLogger = spy(new KitLogger.SilentLogger());
    Path outputDir = tempDir.resolve("output");
    helmConfig = HelmConfig.builder()
        .chart("helm-test")
        .version("0.1.0")
        .chartExtension("tgz")
        .types(Collections.singletonList(HelmConfig.HelmType.KUBERNETES))
        .tarballOutputDir(outputDir.toFile().getAbsolutePath())
        .dependencies(Collections.singletonList(HelmDependency.builder()
            .name("the-dependency")
            .version("0.1.0")
            .repository("file://../../the-dependency")
            .build()))
        .outputDir(outputDir.toString())
        .sourceDir(new File(HelmServiceDependencyUpdateIT.class.getResource("/it/sources").toURI()).getAbsolutePath())
        .build();
    helmService = new HelmService(JKubeConfiguration.builder().build(), new ResourceServiceConfig(), kitLogger);
  }

  @Nested
  @DisplayName("valid helm chart provided")
  class ValidChart {
    @BeforeEach
    void validChartPackage() throws IOException {
      helmService.generateHelmCharts(helmConfig);
    }

    @Test
    @DisplayName("valid dependency provided, then dependency pulled")
    void whenNoExplicitDependencyConfigProvided_thenDependencyDownloaded() {
      // Given
      Helm.create().withName("the-dependency").withDir(tempDir).call();
      // When
      helmService.dependencyUpdate(helmConfig);
      // Then
      verifyHelmDependencyDownloaded();
    }

    @Test
    void whenConfigurationOptionsProvided_thenDependencyDownloaded() {
      // Given
      helmConfig = helmConfig.toBuilder()
        .debug(true)
        .dependencySkipRefresh(true)
        .dependencyVerify(true)
        .build();
      Helm.create().withName("the-dependency").withDir(tempDir).call();
      // When
      helmService.dependencyUpdate(helmConfig);
      // Then
      verifyHelmDependencyDownloaded();
    }

    @Test
    @DisplayName("non existing dependency provided, then throw exception")
    void whenInvalidDependencyProvided_thenThrowException() {
      // When + Then
      assertThatIllegalStateException()
          .isThrownBy(() -> helmService.dependencyUpdate(helmConfig))
          .withMessageContaining("not found");
    }

    private void verifyHelmDependencyDownloaded() {
      verify(kitLogger, times(1))
        .info("Running Helm Dependency Upgrade %s %s", "helm-test", "0.1.0");
      verify(kitLogger, times(1))
        .info("[[W]]%s", "Saving 1 charts");
      verify(kitLogger, times(1))
        .info("[[W]]%s", "Deleting outdated charts");
    }
  }

  @Test
  @DisplayName("invalid chart provided, then throw exception")
  @EnabledOnOs(OS.LINUX)
  void whenInvalidChartDirProvided_thenThrowException() {
    // When + Then
    assertThatIllegalStateException()
        .isThrownBy(() -> helmService.dependencyUpdate(helmConfig))
        .withMessageContaining("no such file or directory");
  }

  @Test
  @DisplayName("invalid chart provided, then throw exception for Windows")
  @EnabledOnOs(OS.WINDOWS)
  void whenInvalidChartDirProvided_thenThrowException_Windows() {
      // When + Then
      assertThatIllegalStateException()
          .isThrownBy(() -> helmService.dependencyUpdate(helmConfig))
          .withMessageContaining("The system cannot find the path specified");
  }
}
