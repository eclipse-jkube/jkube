/*
 * Copyright (c) 2025 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc.
 */
package org.eclipse.jkube.gradle.plugin.tests;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class VerboseOutputIT {

  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @Test
  void k8sResource_whenVerboseEnabled_generatesVerboseOutput() {
    final BuildResult result = gradleRunner.withITProject("verbose-output").withArguments("k8sResource").build();

    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Generators:")
        .contains("Enrichers:");
  }

  @Test
  void ocResource_whenVerboseEnabled_generatesVerboseOutput() {
    final BuildResult result = gradleRunner.withITProject("verbose-output").withArguments("ocResource").build();

    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Generators:")
        .contains("Enrichers:");
  }
}