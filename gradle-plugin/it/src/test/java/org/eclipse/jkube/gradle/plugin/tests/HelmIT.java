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

class HelmIT {
  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();
  private static final String PROJECT_NAME = "helm";

  static Stream<Arguments> data() {
    return Stream.of(
        arguments("zero-config"),
        arguments("fragment"),
        arguments("groovy-dsl"),
        arguments("properties"),
        arguments("fragment-and-groovy-dsl")
    );
  }

  @ParameterizedTest(name = "k8sResource k8sHelm with {0}")
  @MethodSource("data")
  void k8sResourceHelmFromFragment_whenRun_generatesK8sManifestsAndHelmChart(String helmConfigType) throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject(PROJECT_NAME).withArguments("clean", "k8sResource", "k8sHelm", "-PhelmConfigType=" + helmConfigType).build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesHelmMetadataFile(PROJECT_NAME),
        gradleRunner.resolveFile("expected", helmConfigType, "Chart.yaml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding a default Deployment")
        .contains("Adding revision history limit to 2")
        .contains("validating")
        .contains(String.format("Creating Helm Chart \"%s\" for Kubernetes", PROJECT_NAME));
  }

  @ParameterizedTest(name = "ocResource ocHelm with {0}")
  @MethodSource("data")
  void ocResourceHelmFromFragment_whenRun_generatesOpenShiftManifestsAndHelmChart(String helmConfigType) throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject(PROJECT_NAME).withArguments("clean", "ocResource", "ocHelm", "-PhelmConfigType=" + helmConfigType).build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftHelmMetadataFile(PROJECT_NAME),
        gradleRunner.resolveFile("expected", helmConfigType, "Chart.yaml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding a default Deployment")
        .contains("Adding revision history limit to 2")
        .contains("validating")
        .contains(String.format("Creating Helm Chart \"%s\" for OpenShift", PROJECT_NAME));
  }
}
