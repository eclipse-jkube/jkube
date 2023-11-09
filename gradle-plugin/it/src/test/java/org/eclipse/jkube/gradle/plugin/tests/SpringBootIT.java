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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.jkube.kit.common.ResourceVerify;

import net.minidev.json.parser.ParseException;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SpringBootIT {
  @RegisterExtension
  protected final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @Test
  void k8sBuild_whenRunWithJibBuildStrategy_generatesLayeredImage() throws IOException {
    // When
    final BuildResult result = gradleRunner.withITProject("spring-boot")
      .withArguments("clean", "build", "k8sBuild", "-Pjkube.build.strategy=jib", "--stacktrace")
      .build();
    // Then
    final File dockerFile = gradleRunner.resolveFile("build", "docker", "gradle", "spring-boot", "latest", "build", "Dockerfile");
    assertThat(new String(Files.readAllBytes(dockerFile.toPath())))
      .contains("FROM quay.io/jkube/jkube-java:")
      .contains("ENV JAVA_MAIN_CLASS=org.springframework.boot.loader.JarLauncher JAVA_APP_DIR=/deployments")
      .contains("EXPOSE 8080 8778 9779")
      .contains("COPY /dependencies/deployments /deployments/")
      .contains("COPY /spring-boot-loader/deployments /deployments/")
      .contains("COPY /application/deployments /deployments/")
      .contains("WORKDIR /deployments");
    assertThat(result).extracting(BuildResult::getOutput).asString()
      .contains("Running generator spring-boot")
      .contains("Spring Boot layered jar detected")
      .contains("JIB image build started");
  }

  @Test
  void k8sResource_whenRun_generatesK8sManifests() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("spring-boot")
        .withArguments("build", "k8sResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Running generator spring-boot")
        .contains("jkube-controller: Adding a default Deployment")
        .contains("jkube-service: Adding a default service")
        .contains("jkube-healthcheck-spring-boot: Adding readiness probe on port 8080")
        .contains("jkube-healthcheck-spring-boot: Adding liveness probe on port 8080")
        .contains("jkube-service-discovery: Using first mentioned service port '8080' ")
        .contains("jkube-revision-history: Adding revision history limit to 2");
  }

  @Test
  void ocResource_whenRun_generatesOpenShiftManifests() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("spring-boot")
        .withArguments("build", "ocResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Running generator spring-boot")
        .contains("jkube-controller: Adding a default Deployment")
        .contains("jkube-service: Adding a default service")
        .contains("jkube-openshift-deploymentconfig: Converting Deployment to DeploymentConfig")
        .contains("jkube-healthcheck-spring-boot: Adding readiness probe on port 8080")
        .contains("jkube-healthcheck-spring-boot: Adding liveness probe on port 8080")
        .contains("jkube-service-discovery: Using first mentioned service port '8080' ")
        .contains("jkube-revision-history: Adding revision history limit to 2");
  }

}
