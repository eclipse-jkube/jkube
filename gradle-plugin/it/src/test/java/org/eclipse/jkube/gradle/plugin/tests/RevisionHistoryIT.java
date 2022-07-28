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

class RevisionHistoryIT {
  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  static Stream<Arguments> data() {
    return Stream.of(
        arguments("default", new String[] {}),
        arguments("configured", new String[] { "-Pjkube.enricher.jkube-revision-history.limit=10" })
    );
  }

  @ParameterizedTest(name = "k8sResource {0} configured limit = {1}")
  @MethodSource("data")
  void k8sResource_whenRun_generatesK8sManifestsWithRevisionHistory(String expectedDir, String[] arguments) throws IOException, ParseException {
    // When
    List<String> gradleArgs = new ArrayList<>(Arrays.asList(arguments));
    gradleArgs.add("k8sResource");
    gradleArgs.add("--stacktrace");
    final BuildResult result = gradleRunner.withITProject("revisionhistory")
        .withArguments(gradleArgs.toArray(new String[0]))
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", expectedDir, "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding a default Deployment")
        .contains("Adding revision history limit to ")
        .contains("validating")
        .contains("SUMMARY")
        .contains("Generated resources:")
        .contains("build/classes/java/main/META-INF/jkube/kubernetes/revisionhistory-deployment.yml")
        .contains("build/classes/java/main/META-INF/jkube/kubernetes.yml")
        .contains("SUCCESS");
  }

  @ParameterizedTest(name = "k8sResource {0} configured limit = {1}")
  @MethodSource("data")
  void ocResource_whenRun_generatesOpenShiftManifestsWithRevisionHistory(String expectedDir, String[] arguments) throws IOException, ParseException {
    // When
    List<String> gradleArgs = new ArrayList<>(Arrays.asList(arguments));
    gradleArgs.add("ocResource");
    gradleArgs.add("--stacktrace");
    final BuildResult result = gradleRunner.withITProject("revisionhistory")
        .withArguments(gradleArgs.toArray(new String[0]))
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", expectedDir, "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding a default Deployment")
        .contains("Adding revision history limit to ")
        .contains("validating")
        .contains("SUMMARY")
        .contains("Generated resources:")
        .contains("build/classes/java/main/META-INF/jkube/openshift/revisionhistory-deploymentconfig.yml")
        .contains("build/classes/java/main/META-INF/jkube/openshift.yml")
        .contains("SUCCESS");
  }
}
