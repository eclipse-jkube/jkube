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

class SmallRyeIT {
  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @Test
  void k8sResource_whenRun_generatesK8sManifestsWithProbes() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("smallrye-health")
        .withArguments("build", "k8sResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("jkube-controller: Adding a default Deployment")
        .contains("jkube-service: Adding a default service")
        .contains("jkube-healthcheck-smallrye: Adding readiness probe on port 8080")
        .contains("jkube-healthcheck-smallrye: Adding liveness probe on port 8080")
        .contains("jkube-healthcheck-smallrye: Adding startup probe on port 8080")
        .contains("jkube-service-discovery: Using first mentioned service port '8080' ")
        .contains("jkube-revision-history: Adding revision history limit to 2");
  }

  @Test
  void ocResource_whenRun_generatesOpenShiftManifestsWithProbes() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("smallrye-health")
        .withArguments("build", "ocResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("jkube-controller: Adding a default Deployment")
        .contains("jkube-service: Adding a default service")
        .contains("jkube-openshift-deploymentconfig: Converting Deployment to DeploymentConfig")
        .contains("jkube-healthcheck-smallrye: Adding readiness probe on port 8080")
        .contains("jkube-healthcheck-smallrye: Adding liveness probe on port 8080")
        .contains("jkube-healthcheck-smallrye: Adding startup probe on port 8080")
        .contains("jkube-service-discovery: Using first mentioned service port '8080' ")
        .contains("jkube-revision-history: Adding revision history limit to 2");
  }
}
