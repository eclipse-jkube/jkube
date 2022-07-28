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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DependencyResourcesIT {

  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @Test
  void k8sResource_whenRun_generatesK8sManifestsIncludingDependencies() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("dependency-resources")
        .withArguments("clean", "jar", "k8sResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(
        gradleRunner.resolveFile("dependent", "build", "classes", "java", "main",
            "META-INF", "jkube", "kubernetes.yml"),
        gradleRunner.resolveFile("expected", "kubernetes-defaults.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding revision history limit to 2")
        .contains("validating")
        .contains("SUMMARY")
        .contains("Generated resources:")
        .contains("build/classes/java/main/META-INF/jkube/kubernetes/example-deployment.yml")
        .contains("build/classes/java/main/META-INF/jkube/kubernetes/web-replicaset.yml")
        .contains("build/classes/java/main/META-INF/jkube/kubernetes.yml")
        .contains("SUCCESS");
  }

  @Test
  void k8sResource_whenRunWithReplicas_generatesK8sManifestsIncludingDependencies() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("dependency-resources")
        .withArguments("-Pjkube.replicas=1337", "clean", "jar", "k8sResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(
        gradleRunner.resolveFile("dependent", "build", "classes", "java", "main",
            "META-INF", "jkube", "kubernetes.yml"),
        gradleRunner.resolveFile("expected", "kubernetes-with-replica-override.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding revision history limit to 2")
        .contains("validating")
        .contains("SUMMARY")
        .contains("Generated resources:")
        .contains("build/classes/java/main/META-INF/jkube/kubernetes/example-deployment.yml")
        .contains("build/classes/java/main/META-INF/jkube/kubernetes/web-replicaset.yml")
        .contains("build/classes/java/main/META-INF/jkube/kubernetes.yml")
        .contains("SUCCESS");
  }
}
