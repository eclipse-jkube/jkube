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
package org.eclipse.jkube.gradle.plugin.tests;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class HelmLintIT {

  @RegisterExtension
  public final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @BeforeEach
  void setUp() {
    gradleRunner.withITProject("helm-lint");
  }

  @Test
  void k8sHelmLint() {
    // When
    final BuildResult result = gradleRunner
      .withArguments("clean", "k8sResource", "k8sHelm", "k8sHelmLint").build();
    // Then
    assertThat(result).extracting(BuildResult::getOutput).asString()
      .contains("k8s: Linting helm-lint 0.0.1-SNAPSHOT")
      .contains("k8s: [INFO] Chart.yaml: icon is recommended")
      .contains("k8s: Linting successful");
  }

  @Test
  void ocHelmLint() {
    // When
    final BuildResult result = gradleRunner
      .withArguments("clean", "ocResource", "ocHelm", "ocHelmLint").build();
    // Then
    assertThat(result).extracting(BuildResult::getOutput).asString()
      .contains("oc: Linting helm-lint 0.0.1-SNAPSHOT")
      .contains("oc: [INFO] Chart.yaml: icon is recommended")
      .contains("oc: Linting successful");
  }
}
