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

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ResourceValidationIT {
  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @Test
  void k8sResource_whenRun_shouldValidateGeneratedResources() {
    // When
    final BuildResult result = gradleRunner.withITProject("spring-boot")
        .withArguments("build", "k8sResource", "--stacktrace")
        .build();
    // Then
    assertThat(result).extracting(BuildResult::getOutput)
        .asString()
        .containsPattern("validating .*deployment.yml resource")
        .containsPattern("validating .*service.yml resource")
        .doesNotContain("Unknown keyword");
  }

  @Test
  void ocResource_whenRun_shouldValidateGeneratedResources() {
    // When
    final BuildResult result = gradleRunner.withITProject("spring-boot")
        .withArguments("build", "ocResource", "--stacktrace")
        .build();
    // Then
    assertThat(result).extracting(BuildResult::getOutput)
        .asString()
        .containsPattern("validating .*deploymentconfig.yml resource")
        .containsPattern("validating .*service.yml resource")
        .doesNotContain("Unknown keyword");
  }
}
