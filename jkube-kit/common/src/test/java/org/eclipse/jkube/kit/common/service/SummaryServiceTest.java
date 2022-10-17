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
package org.eclipse.jkube.kit.common.service;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.summary.KubernetesResourceSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SummaryServiceTest {
  private KitLogger logger;
  @TempDir
  private File temporaryFolder;
  private SummaryService summaryService;

  @BeforeEach
  public void setUp() {
    logger = spy(new KitLogger.SilentLogger());
    summaryService = new SummaryService(temporaryFolder, logger, true);
  }

  @Test
  void printSummary_whenInvoked_shouldPrintSummary() {
    // Given
    initializeSummary();

    // When
    summaryService.printSummary(temporaryFolder);

    // Then
    verifySummaryPrintedOnce();
  }

  @Test
  void printSummary_whenFailure_shouldPrintFailureAndCause() {
    // Given
    summaryService.setSuccessful(false);
    summaryService.setFailureCause("failure in pulling image");

    // When
    summaryService.printSummary(temporaryFolder);

    // Then
    verify(logger).error("FAILURE [%s]", "failure in pulling image");
  }

  @Test
  void printSummary_whenSummaryEnabledFalse_shouldNotPrintAnything() {
    // Given
    summaryService = new SummaryService(temporaryFolder, logger, false);
    // When
    summaryService.printSummary(temporaryFolder);

    // Then
    verify(logger, times(0)).info(anyString());
  }

  @Test
  void setFailureAndCause_whenSummaryEnabled_shouldLogFailureAndCause() {
    // Given
    summaryService.setFailureAndCause("failed to execute");

    // When
    summaryService.printSummary(temporaryFolder);

    // Then
    verify(logger).error("FAILURE [%s]", "failed to execute");
  }

  @Test
  void setFailureAndCause_whenSummaryDisabled_shouldLogNothing() {
    // Given
    summaryService = new SummaryService(temporaryFolder, logger, false);

    // When
    summaryService.setFailureAndCause( "failed");
    summaryService.printSummary(temporaryFolder);

    // Then
    verify(logger, times(0)).error(anyString(), anyString());
  }

  @Test
  void clear_whenInvoked_shouldDeleteSummaryFile() {
    // Given
    summaryService.setSuccessful(true);

    // When
    summaryService.clear();

    // Then
    verify(logger, times(0)).info(anyString());
  }

  private void initializeSummary() {
    summaryService.setBuildStrategy("Local Docker");
    summaryService.addToGenerators("java-exec");
    summaryService.setDockerFileImageSummary("quay.io/example/test:latest", new File(temporaryFolder, "src/main/docker/Dockerfile").getAbsolutePath());
    summaryService.setBaseImageNameImageSummary("quay.io/example/test:latest", "quay.io/jkube/java:latest");
    summaryService.setImageShaImageSummary("quay.io/example/test:latest", "def3");
    summaryService.setImageStreamUsedImageSummary("quay.io/example/test:latest", "test");
    summaryService.setPushRegistry("quay.io");
    summaryService.setOpenShiftBuildConfigName("test");
    summaryService.addGeneratedResourceFile(new File(temporaryFolder, "target/classes/META-INF/jkube/kubernetes/test-deployment.yml"));
    summaryService.addGeneratedResourceFile(new File(temporaryFolder, "target/classes/META-INF/jkube/kubernetes/test-service.yml"));
    summaryService.setAggregateResourceFile(new File(temporaryFolder, "target/classes/META-INF/jkube/kubernetes.yml"));
    summaryService.addToEnrichers("jkube-controller");
    summaryService.addToEnrichers("jkube-service");
    summaryService.addAppliedKubernetesResource(KubernetesResourceSummary.builder()
        .kind("Deployment")
        .group("apps")
        .version("v1")
        .namespace("test-ns")
        .resourceName("test")
        .build());
    summaryService.addAppliedKubernetesResource(KubernetesResourceSummary.builder()
        .kind("Service")
        .group("v1")
        .version("v1")
        .namespace("test-ns")
        .resourceName("test")
        .build());
    summaryService.setHelmChartName("test");
    summaryService.setHelmChartCompressedLocation(new File(temporaryFolder, "target/test.tar.gz"));
    summaryService.setHelmChartLocation(new File(temporaryFolder, "target/jkube/helm/test/kubernetes"));
    summaryService.setHelmRepository("localhost:8001/api/charts");
    summaryService.addDeletedKubernetesResource(KubernetesResourceSummary.builder()
        .kind("Deployment")
        .group("apps")
        .version("v1")
        .namespace("test-ns")
        .resourceName("test")
        .build());
    summaryService.addDeletedKubernetesResource(KubernetesResourceSummary.builder()
        .kind("Service")
        .group("v1")
        .version("v1")
        .namespace("test-ns")
        .resourceName("test")
        .build());
    summaryService.setAppliedClusterUrl("https://192.168.39.75:8443/");
    summaryService.setUndeployedClusterUrl("https://192.168.39.75:8443/");
    summaryService.setActionType("Goals");
    summaryService.addToActions("k8sResource");
    summaryService.addToActions("k8sHelm");
    summaryService.setSuccessful(true);
  }

  private void verifySummaryPrintedOnce() {
    verifySummaryBannerPrinted();
    verifyCommonSummaryPrinted();
    verifyBuildSummaryPrinted();
    verifyPushSummaryPrinted();
    verifyResourceSummaryPrinted();
    verifyHelmSummaryPrinted();
    verifyApplyUndeploySummaryPrinted();
    verifyActionsRunPrinted();
    verify(logger).info("      SUMMARY");
  }

  private void verifyApplyUndeploySummaryPrinted() {
    verify(logger).info("Undeployed resources to %s: %s", "https://192.168.39.75:8443/", "[Deployment/test, Service/test]");
    verify(logger).info("Applied resources to %s: %s", "https://192.168.39.75:8443/", "[Deployment/test, Service/test]");
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
    verify(logger).info("Enrichers applied: [%s]", "jkube-controller, jkube-service");
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

  private void verifyActionsRunPrinted() {
    verify(logger).info("%s executed : [ %s ]", "Goals", "k8sResource, k8sHelm");
  }
}
