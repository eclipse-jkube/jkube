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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SecurityHardeningIT {

  @RegisterExtension
  public final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @Test
  void k8sResourceTask_whenRunWithSecurityHardeningProfile_generatesK8sManifestWithSecurityConstraints() throws Exception {
    // When
    final BuildResult result = gradleRunner.withITProject("security-hardening")
      .withArguments("k8sResource", "-Pjkube.profile=security-hardening", "--stacktrace")
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
  void ocResourceTask_whenRunWithSecurityHardeningProfile_generatesK8sManifestWithSecurityConstraints() throws Exception {
    // When
    final BuildResult result = gradleRunner.withITProject("security-hardening")
      .withArguments("ocResource", "-Pjkube.profile=security-hardening", "--stacktrace")
      .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
      gradleRunner.resolveFile("expected", "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
      .contains("Using resource templates from")
      .contains("Adding a default Deployment")
      .contains("Converting Deployment to DeploymentConfig")
      .contains("Adding revision history limit to 2")
      .contains("validating");
  }

  @Test
  void k8sResourceTask_whenFinalVersion_warnsAboutLatestTag() {
    final BuildResult result = gradleRunner.withITProject("security-hardening")
      .withArguments("k8sResource", "-Pversion=1.0.0", "-Pjkube.profile=security-hardening", "--stacktrace")
      .build();
    assertThat(result).extracting(BuildResult::getOutput).asString()
      .contains("Container repository-security-hardening has an image with tag 'latest', it's recommended to use a fixed tag or a digest instead");

  }

  @Test
  void k8sResourceTask_whenSnapshotVersion_doesntWarnAboutLatestTag() {
    final BuildResult result = gradleRunner.withITProject("security-hardening")
      .withArguments("k8sResource", "-Pversion=0.0.0-SNAPSHOT", "-Pjkube.profile=security-hardening", "--stacktrace")
      .build();
    assertThat(result).extracting(BuildResult::getOutput).asString()
      .doesNotContain("it's recommended to use a fixed tag or a digest instead");

  }
}
