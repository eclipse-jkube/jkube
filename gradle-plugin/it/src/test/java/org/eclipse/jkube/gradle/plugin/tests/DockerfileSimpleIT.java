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

import net.minidev.json.parser.ParseException;
import org.eclipse.jkube.kit.common.ResourceVerify;
import org.gradle.testkit.runner.BuildResult;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DockerfileSimpleIT {
  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Test
  public void k8sResource_whenRun_generatesK8sManifests() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("dockerfile-simple").withArguments("k8sResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
      .contains("Using resource templates from")
      .contains("Adding a default Deployment")
      .contains("Adding revision history limit to 2")
      .contains("Using first mentioned service port")
      .contains("validating");
  }

  @Test
  public void ocResource_whenRun_generatesOpenShiftManifests() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("dockerfile-simple").withArguments("ocResource").build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
      gradleRunner.resolveFile("expected", "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
      .contains("Using resource templates from")
      .contains("Adding a default Deployment")
      .contains("Converting Deployment to DeploymentConfig")
      .contains("Adding revision history limit to 2")
      .contains("Using first mentioned service port")
      .contains("validating");
  }
}
