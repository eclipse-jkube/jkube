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
import org.eclipse.jkube.kit.common.JKubeException;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("HelmService.uploadHelmChart")
class HelmServiceLintIT {

  @TempDir
  private Path tempDir;
  private KitLogger kitLogger;
  private Path outputDir;
  private HelmConfig helmConfig;
  private HelmService helmService;

  @BeforeEach
  void setUp() {
    kitLogger = spy(new KitLogger.SilentLogger());
    outputDir = tempDir.resolve("output");
    helmConfig = HelmConfig.builder()
      .chart("helm-test")
      .version("0.1.0")
      .chartExtension("tgz")
      .types(Arrays.asList(HelmConfig.HelmType.KUBERNETES, HelmConfig.HelmType.OPENSHIFT))
      .tarballOutputDir(outputDir.toFile().getAbsolutePath())
      .build();
    helmService = new HelmService(JKubeConfiguration.builder().build(), new ResourceServiceConfig(), kitLogger);
  }

  @Nested
  class Valid {

    @BeforeEach
    void validChartPackage() throws IOException {
      final Helm helm = Helm.create().withName("helm-test").withDir(tempDir).call();
      // Create templates as file (instead of dir) to force a warning
      Files.walk(tempDir.resolve("helm-test").resolve("templates"))
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
      Files.createFile(tempDir.resolve("helm-test").resolve("templates"));
      helm
        .packageIt().withDestination(outputDir.resolve("kubernetes")).call()
        .packageIt().withDestination(outputDir.resolve("openshift")).call();
    }

    @Test
    void genericInfoMessage() {
      helmService.lint(helmConfig);
      verify(kitLogger, atLeastOnce())
        .info("Linting %s %s", "helm-test", "0.1.0");
    }

    @Test
    void kubernetesPriorInfoMessage() {
      helmService.lint(helmConfig);
      verify(kitLogger, times(1))
        .info(eq("Using packaged file: %s"), endsWith("kubernetes" + File.separator + "helm-test-0.1.0.tgz"));
    }

    @Test
    void openshiftPriorInfoMessage() {
      helmService.lint(helmConfig);
      verify(kitLogger, times(1))
        .info(eq("Using packaged file: %s"), endsWith("openshift" + File.separator + "helm-test-0.1.0.tgz"));
    }

    @Test
    void lintInfoMessageInWhite() {
      helmService.lint(helmConfig);
      verify(kitLogger, atLeastOnce())
        .info("[[W]]%s", "[INFO] Chart.yaml: icon is recommended");
    }

    @Test
    void successMessage() {
      helmService.lint(helmConfig);
      verify(kitLogger, atLeastOnce()).info("Linting successful");
    }

    @Nested
    class Strict {
      @BeforeEach
      void setUp() {
        helmConfig = helmConfig.toBuilder().lintStrict(true).build();
        helmService = new HelmService(JKubeConfiguration.builder().build(), new ResourceServiceConfig(), kitLogger);
      }

      @Test
      void lintErrorMessageInWhite() {
        assertThatExceptionOfType(JKubeException.class).isThrownBy(() -> helmService.lint(helmConfig));
        verify(kitLogger, atLeastOnce())
          .error("[[W]]%s", "[WARNING] templates/: not a directory");
      }

      @Test
      void lintingException() {
        assertThatExceptionOfType(JKubeException.class)
          .isThrownBy(() -> helmService.lint(helmConfig))
          .withMessage("Linting failed");
      }
    }

    @Nested
    class Quiet {
      @BeforeEach
      void setUp() {
        helmConfig = helmConfig.toBuilder().lintQuiet(true).build();
        helmService = new HelmService(JKubeConfiguration.builder().build(), new ResourceServiceConfig(), kitLogger);
      }

      @Test
      void lintInfoMessageOmitted() {
        helmService.lint(helmConfig);
        verify(kitLogger, never())
          .info("[[W]]%s", "[INFO] Chart.yaml: icon is recommended");
      }

      @Test
      void lintWarnMessage() {
        helmService.lint(helmConfig);
        verify(kitLogger, atLeastOnce())
          .info("[[W]]%s", "[WARNING] templates/: not a directory");
      }
    }

  }

  @Nested
  class Invalid {

    @BeforeEach
    void invalidChartPackage() throws IOException {
      final Helm chart = Helm.create().withName("helm-test").withDir(tempDir).call();
      Files.write(tempDir.resolve("helm-test").resolve("Chart.yaml"),
        "\nicon: ://invalid-url".getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.APPEND
      );
      chart
        .packageIt().withDestination(outputDir.resolve("kubernetes")).call()
        .packageIt().withDestination(outputDir.resolve("openshift")).call();
    }

    @Test
    void genericInfoMessage() {
      assertThatExceptionOfType(JKubeException.class).isThrownBy(() -> helmService.lint(helmConfig));
      verify(kitLogger, atLeastOnce())
        .info("Linting %s %s", "helm-test", "0.1.0");
    }

    @Test
    void kubernetesPriorInfoMessage() {
      assertThatExceptionOfType(JKubeException.class).isThrownBy(() -> helmService.lint(helmConfig));
      verify(kitLogger, times(1))
        .info(eq("Using packaged file: %s"), endsWith("kubernetes" + File.separator + "helm-test-0.1.0.tgz"));
    }

    @Test
    void openshiftPriorInfoMessageNotThrownDueToPriorExceptionHaltingProcessing() {
      assertThatExceptionOfType(JKubeException.class).isThrownBy(() -> helmService.lint(helmConfig));
      verify(kitLogger, never())
        .info(eq("Using packaged file: %s"), endsWith("openshift" + File.separator + "helm-test-0.1.0.tgz"));
    }

    @Test
    void lintErrorMessageInWhite() {
      assertThatExceptionOfType(JKubeException.class).isThrownBy(() -> helmService.lint(helmConfig));
      verify(kitLogger, atLeastOnce())
        .error("[[W]]%s", "[ERROR] Chart.yaml: invalid icon URL '://invalid-url'");
    }

    @Test
    void lintingException() {
      assertThatExceptionOfType(JKubeException.class)
        .isThrownBy(() -> helmService.lint(helmConfig))
        .withMessage("Linting failed");
    }
  }
}
