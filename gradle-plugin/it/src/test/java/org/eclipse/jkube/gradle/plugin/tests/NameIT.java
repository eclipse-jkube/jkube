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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class NameIT {
  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  static Stream<Arguments> data() {
    return Stream.of(
        arguments("default", ""),
        arguments("custom-name", "configured-name")
    );
  }

  @ParameterizedTest(name = "k8sResource {0} configured name = {1}")
  @MethodSource("data")
  void k8sResource_whenRun_generatesK8sManifestsWithDefaultName(String expectedDir, String nameFromEnricherConfig) throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("name")
        .withArguments("-Pjkube.enricher.jkube-name.name=" + nameFromEnricherConfig, "k8sResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", expectedDir, "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding a default Deployment")
        .contains("Adding revision history limit to 2")
        .contains("validating")
        .contains("SUMMARY")
        .contains("Generated resources:")
        .contains("build/classes/java/main/META-INF/jkube/kubernetes.yml")
        .contains("SUCCESS");
  }

  @ParameterizedTest(name = "k8sResource {0} configured name = {1}")
  @MethodSource("data")
  void ocResource_whenRun_generatesOpenShiftManifestsWithDefaultName(String expectedDir, String nameFromEnricherConfig) throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("name")
        .withArguments("-Pjkube.enricher.jkube-name.name=" + nameFromEnricherConfig, "ocResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", expectedDir, "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding a default Deployment")
        .contains("Adding revision history limit to 2")
        .contains("validating")
        .contains("SUMMARY")
        .contains("Generated resources:")
        .contains("build/classes/java/main/META-INF/jkube/openshift.yml")
        .contains("SUCCESS");
  }

}
