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

import net.minidev.json.parser.ParseException;
import org.eclipse.jkube.kit.common.ResourceVerify;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("Service can have name overridden by configuration when there's a service fragment")
class ServiceNameAndFragmentIT {

  private static final String TEST_PROJECT = "service-name-fragment";

  @RegisterExtension
  protected final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @Test
  void k8sResourceTask() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject(TEST_PROJECT)
      .withArguments("k8sResource", "--stacktrace")
      .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
      gradleRunner.resolveFile("expected", "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
      .contains("Using resource templates from")
      .contains("Adding a default Deployment")
      .contains("Adding revision history limit to 2")
      .contains("validating");
  }

  @Test
  void ocResourceTask() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject(TEST_PROJECT)
      .withArguments("ocResource", "--stacktrace")
      .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
      gradleRunner.resolveFile("expected", "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
      .contains("Using resource templates from")
      .contains("Adding a default Deployment")
      .contains("Adding revision history limit to 2")
      .contains("validating");
  }
}
