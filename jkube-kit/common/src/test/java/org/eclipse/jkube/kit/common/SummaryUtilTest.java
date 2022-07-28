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
package org.eclipse.jkube.kit.common;

import org.eclipse.jkube.kit.common.summary.KubernetesResourceSummary;
import org.eclipse.jkube.kit.common.util.SummaryUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SummaryUtilTest {
  private KitLogger logger;
  @TempDir
  private File temporaryFolder;

  @BeforeEach
  public void setUp() {
    logger = spy(new KitLogger.SilentLogger());
    SummaryUtil.initSummary(temporaryFolder, logger);
  }

  @Test
  void printSummary_whenInvoked_shouldPrintSummary() {
    // Given
    initializeSummary();

    // When
    SummaryUtil.printSummary(temporaryFolder, true);

    // Then
    verifySummaryPrintedOnce();
  }

  @Test
  void printSummary_whenFailure_shouldPrintFailureAndCause() {
    // Given
    SummaryUtil.setSuccessful(false);
    SummaryUtil.setFailureCause("failure in pulling image");

    // When
    SummaryUtil.printSummary(temporaryFolder, true);

    // Then
    verify(logger).error("FAILURE [%s]", "failure in pulling image");
  }

  @Test
  void printSummary_whenSummaryEnabledFalse_shouldNotPrintAnything() {
    // Given + When
    SummaryUtil.printSummary(temporaryFolder, false);

    // Then
    verify(logger, times(0)).info(anyString());
  }

  @Test
  void setFailureIfSummaryEnabledOrThrow_whenSummaryEnabled_shouldLogFailureAndCause() {
    // Given
    SummaryUtil.setFailureIfSummaryEnabledOrThrow(true, "failed to execute", () -> new IllegalStateException("failure"));

    // When
    SummaryUtil.printSummary(temporaryFolder, true);

    // Then
    verify(logger).error("FAILURE [%s]", "failed to execute");
  }

  @Test
  void setFailureIfSummaryEnabledOrThrow_whenSummaryDisabled_shouldLogThrowException() {
    // When + Then
    assertThatIllegalStateException()
        .isThrownBy(() -> SummaryUtil.setFailureIfSummaryEnabledOrThrow(false, "failed", () -> new IllegalStateException("failure")))
        .withMessage("failure");
    verify(logger, times(0)).error(anyString(), anyString());
  }

  @Test
  void clear_whenInvoked_shouldDeleteSummaryFile() {
    // Given
    SummaryUtil.setSuccessful(true);

    // When
    SummaryUtil.clear();

    // Then
    verify(logger, times(0)).info(anyString());
  }

  private void initializeSummary() {
    SummaryUtil.setBuildStrategy("Local Docker");
    SummaryUtil.addToGenerators("java-exec");
    SummaryUtil.setDockerFileImageSummary("quay.io/example/test:latest", new File(temporaryFolder, "src/main/docker/Dockerfile").getAbsolutePath());
    SummaryUtil.setBaseImageNameImageSummary("quay.io/example/test:latest", "quay.io/jkube/java:latest");
    SummaryUtil.setImageShaImageSummary("quay.io/example/test:latest", "def3");
    SummaryUtil.setImageStreamUsedImageSummary("quay.io/example/test:latest", "test");
    SummaryUtil.setPushRegistry("quay.io");
    SummaryUtil.setOpenShiftBuildConfigName("test");
    SummaryUtil.addGeneratedResourceFile(new File(temporaryFolder, "target/classes/META-INF/jkube/kubernetes/test-deployment.yml"));
    SummaryUtil.addGeneratedResourceFile(new File(temporaryFolder, "target/classes/META-INF/jkube/kubernetes/test-service.yml"));
    SummaryUtil.setAggregateResourceFile(new File(temporaryFolder, "target/classes/META-INF/jkube/kubernetes.yml"));
    SummaryUtil.addToEnrichers("jkube-controller");
    SummaryUtil.addToEnrichers("jkube-service");
    SummaryUtil.addAppliedKubernetesResource(KubernetesResourceSummary.builder()
        .kind("Deployment")
        .group("apps")
        .version("v1")
        .namespace("test-ns")
        .resourceName("test")
        .build());
    SummaryUtil.addAppliedKubernetesResource(KubernetesResourceSummary.builder()
        .kind("Service")
        .group("v1")
        .version("v1")
        .namespace("test-ns")
        .resourceName("test")
        .build());
    SummaryUtil.setHelmChartName("test");
    SummaryUtil.setHelmChartCompressedLocation(new File(temporaryFolder, "target/test.tar.gz"));
    SummaryUtil.setHelmChartLocation(new File(temporaryFolder, "target/jkube/helm/test/kubernetes"));
    SummaryUtil.setHelmRepository("localhost:8001/api/charts");
    SummaryUtil.addDeletedKubernetesResource(KubernetesResourceSummary.builder()
        .kind("Deployment")
        .group("apps")
        .version("v1")
        .namespace("test-ns")
        .resourceName("test")
        .build());
    SummaryUtil.addDeletedKubernetesResource(KubernetesResourceSummary.builder()
        .kind("Service")
        .group("v1")
        .version("v1")
        .namespace("test-ns")
        .resourceName("test")
        .build());
    SummaryUtil.setAppliedClusterUrl("https://192.168.39.75:8443/");
    SummaryUtil.setUndeployedClusterUrl("https://192.168.39.75:8443/");
    SummaryUtil.setSuccessful(true);
  }

  private void verifySummaryPrintedOnce() {
    verifySummaryBannerPrinted();
    verifyCommonSummaryPrinted();
    verifyBuildSummaryPrinted();
    verifyPushSummaryPrinted();
    verifyResourceSummaryPrinted();
    verifyHelmSummaryPrinted();
    verifyApplyUndeploySummaryPrinted();
    verify(logger).info("      SUMMARY");
  }

  private void verifyApplyUndeploySummaryPrinted() {
    verify(logger).info("Undeployed resources from %s", "https://192.168.39.75:8443/");
    verify(logger, times(4)).info(" - %s", "test");
    verify(logger, times(2)).info("   * %s/%s %s", "apps", "v1", "Deployment");
    verify(logger, times(4)).info("   * Namespace: %s", "test-ns");
    verify(logger, times(2)).info("   * %s %s", "v1", "Service");
  }

  private void verifyPushSummaryPrinted() {
    verify(logger).info("Registry: %s", "quay.io");
  }

  private void verifyBuildSummaryPrinted() {
    verify(logger).info("Build Strategy : %s", "Local Docker");
    verify(logger).info("Generators applied: [%s]", "java-exec");
    verify(logger).info("Build config: %s", "test");
  }

  private void verifyCommonSummaryPrinted() {
    verify(logger).info("Container images:");
    verify(logger).info(" - %s", "quay.io/example/test:latest");
    verify(logger).info("    * Base image: %s", "quay.io/jkube/java:latest");
    verify(logger).info("    * Dockerfile image: %s", "src/main/docker/Dockerfile");
    verify(logger).info("    * SHA: %s", "def3");
    verify(logger).info("    * ImageStream: %s", "test");
  }

  private void verifySummaryBannerPrinted() {
    verify(logger, times(4)).info("-------------------------------");
    verify(logger).info("      SUMMARY");
    verify(logger).info(" __ / / //_/ / / / _ )/ __/");
    verify(logger).info("/ // / ,< / /_/ / _  / _/  ");
    verify(logger).info("\\___/_/|_|\\____/____/___/  \n");
  }

  private void verifyResourceSummaryPrinted() {
    verify(logger).info("Enrichers applied: [%s]", "jkube-controller,jkube-service");
    verify(logger).info("Generated resources:");
    verify(logger).info(" - %s", "target/classes/META-INF/jkube/kubernetes/test-deployment.yml");
    verify(logger).info(" - %s", "target/classes/META-INF/jkube/kubernetes/test-service.yml");
    verify(logger).info(" - %s", "target/classes/META-INF/jkube/kubernetes.yml");
  }

  private void verifyHelmSummaryPrinted() {
    verify(logger).info("Chart : %s", "test");
    verify(logger).info("Location : %s", "target/jkube/helm/test/kubernetes");
    verify(logger).info("Compressed : %s", "target/test.tar.gz");
  }
}
