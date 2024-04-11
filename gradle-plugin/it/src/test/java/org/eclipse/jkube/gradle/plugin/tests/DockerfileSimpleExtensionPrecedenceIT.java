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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DockerfileSimpleExtensionPrecedenceIT {


  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @Test
  void k8sResource_whenRun_generatesK8sManifests() {
    // Remove kubernetes-gradle-plugin from classpath to avoid using its profiles-default.yml
    gradleRunner.withPluginClassPath(gradleRunner.pluginClassPath().stream()
      .filter(f -> !f.getAbsolutePath().contains("gradle-plugin" + File.separator + "openshift"))
      .collect(Collectors.toList())
    );
    // When
    final BuildResult result = gradleRunner
      .withITProject("dockerfile-simple-generator-precedence-kubernetes")
      .withArguments("k8sResource", "--stacktrace")
      .build();
    // Then
    assertThat(result).extracting(BuildResult::getOutput).asString()
      .contains("Running generator dockerfile-simple");
  }

  @Test
  void ocResource_whenRun_generatesOpenShiftManifests() {
    // When
    final BuildResult result = gradleRunner
      .withITProject("dockerfile-simple-generator-precedence-openshift")
      .withArguments("ocResource")
      .build();
    // Then
    assertThat(result).extracting(BuildResult::getOutput).asString()
      .contains("Running generator dockerfile-simple");
  }
}
