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
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ProbesGroovyDSLIT {
  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"startup", "liveness-readiness", "none"})
  void k8sResource_whenRun_generatesK8sManifestsWithProbes(String probeConfigMode) throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("probes-groovy-dsl-config")
        .withArguments("build", "k8sResource", "-PprobeConfigMode=" + probeConfigMode, "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", probeConfigMode, "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Running generator spring-boot")
        .contains("jkube-controller: Adding a default Deployment")
        .contains("jkube-service: Adding a default service")
        .contains("jkube-service-discovery: Using first mentioned service port '8080' ")
        .contains("jkube-revision-history: Adding revision history limit to 2");
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"startup", "liveness-readiness", "none"})
  void ocResource_whenRun_generatesOpenShiftManifests(String probeConfigMode) throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("probes-groovy-dsl-config")
        .withArguments("build", "ocResource", "-PprobeConfigMode=" + probeConfigMode, "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", probeConfigMode, "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Running generator spring-boot")
        .contains("jkube-controller: Adding a default Deployment")
        .contains("jkube-service: Adding a default service")
        .contains("jkube-openshift-deploymentconfig: Converting Deployment to DeploymentConfig")
        .contains("jkube-service-discovery: Using first mentioned service port '8080' ")
        .contains("jkube-revision-history: Adding revision history limit to 2");
  }
}
