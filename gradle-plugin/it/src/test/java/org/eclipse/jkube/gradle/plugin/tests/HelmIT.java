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
package org.eclipse.jkube.gradle.plugin.tests;

import org.eclipse.jkube.kit.common.ResourceVerify;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class HelmIT {

  @RegisterExtension
  public final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @ParameterizedTest(name = "k8sResource k8sHelm with {0}")
  @ValueSource(strings = {
    "helm-dsl",
    "helm-fragment",
    "helm-fragment-and-dsl",
    "helm-properties",
    "helm-zero-config"
  })
  void k8sResourceHelmFromFragment_whenRun_generatesK8sManifestsAndHelmChart(String projectName) throws Exception {
    // When
    final BuildResult result = gradleRunner.withITProject(projectName)
      .withArguments("clean", "k8sResource", "k8sHelm").build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesHelmMetadataFile(projectName),
        gradleRunner.resolveFile("expected", "Chart.yaml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding a default Deployment")
        .contains("Adding revision history limit to 2")
        .contains("validating")
        .contains(String.format("Creating Helm Chart \"%s\" for Kubernetes", projectName));
  }

  @ParameterizedTest(name = "ocResource ocHelm with {0}")
  @ValueSource(strings = {
    "helm-dsl",
    "helm-fragment",
    "helm-fragment-and-dsl",
    "helm-properties",
    "helm-zero-config"
  })
  void ocResourceHelmFromFragment_whenRun_generatesOpenShiftManifestsAndHelmChart(String projectName) throws Exception {
    // When
    final BuildResult result = gradleRunner.withITProject(projectName)
      .withArguments("clean", "ocResource", "ocHelm").build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftHelmMetadataFile(projectName),
        gradleRunner.resolveFile("expected", "Chart.yaml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding a default Deployment")
        .contains("Adding revision history limit to 2")
        .contains("validating")
        .contains(String.format("Creating Helm Chart \"%s\" for OpenShift", projectName));
  }
}
