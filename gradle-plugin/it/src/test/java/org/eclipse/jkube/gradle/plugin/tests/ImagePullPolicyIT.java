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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ImagePullPolicyIT {
  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  static Stream<Arguments> data() {
    return Stream.of(
        arguments("property", new String[] { "-Pjkube.imagePullPolicy=Always-property" }),
        arguments("resourceconfig", new String[] { "-PimagePullPolicyMode=resourceconfig" }),
        arguments("enricherconfig", new String[] { "-PimagePullPolicyMode=enricherconfig" }),
        arguments("fragment", new String[] { "-Pjkube.resourceDir=./fragments" }),
        arguments("property-and-resourceconfig", new String[] { "-Pjkube.imagePullPolicy=Always-property", "-PimagePullPolicyMode=property-and-resourceconfig" }),
        arguments("property-and-enricherconfig", new String[] { "-Pjkube.imagePullPolicy=Always-property", "-PimagePullPolicyMode=property-and-enricherconfig" }),
        arguments("property-and-fragment", new String[] { "-Pjkube.imagePullPolicy=Always-property",   "-Pjkube.resourceDir=./fragments", "-PimagePullPolicyMode=property", })
    );
  }

  @ParameterizedTest(name = "resource task with {1} ")
  @MethodSource("data")
  void k8sResource_whenRun_generatesK8sManifestsWithProjectLabels(String expectedDir, String[] arguments) throws IOException, ParseException {
    // When
    List<String> gradleArgs = new ArrayList<>(Arrays.asList(arguments));
    gradleArgs.add("k8sResource");
    gradleArgs.add("--stacktrace");
    final BuildResult result = gradleRunner.withITProject("imagepullpolicy")
        .withArguments(gradleArgs.toArray(new String[0]))
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", expectedDir, "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }

  @ParameterizedTest(name = "resource task with {1} ")
  @MethodSource("data")
  void ocResource_whenRun_generatesOpenShiftManifestsWithProjectLabels(String expectedDir, String[] arguments) throws IOException, ParseException {
    // When
    List<String> gradleArgs = new ArrayList<>(Arrays.asList(arguments));
    gradleArgs.add("ocResource");
    gradleArgs.add("--stacktrace");
    final BuildResult result = gradleRunner.withITProject("imagepullpolicy")
        .withArguments(gradleArgs.toArray(new String[0]))
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", expectedDir, "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }
}
