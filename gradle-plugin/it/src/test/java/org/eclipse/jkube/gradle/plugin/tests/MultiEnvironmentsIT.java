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
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

class MultiEnvironmentsIT {
  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  static Stream<Arguments> data() {
    return Stream.of(
        arguments("common", "common"),
        arguments("dev", "dev"),
        arguments("prod", "prod"),
        arguments("common,dev", "common-and-dev"),
        arguments("common,prod", "common-and-prod")
    );
  }

  @ParameterizedTest(name = "environment {0}")
  @MethodSource("data")
  void k8sResource_whenRun_generatesK8sManifestsContainingConfigMap(String environment, String expectedDirectory) throws IOException, ParseException {
    // When
    gradleRunner.withITProject("multi-environments")
        .withArguments("build", "-Pjkube.environment=" + environment, "k8sResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", expectedDirectory, "kubernetes.yml"));
  }

  @ParameterizedTest(name = "environment {0}")
  @MethodSource("data")
  void ocResource_whenRun_generatesOpenShiftManifestsContainingConfigMap(String environment, String expectedDirectory) throws IOException, ParseException {
    // When
    gradleRunner.withITProject("multi-environments")
        .withArguments("build", "-Pjkube.environment=" + environment, "ocResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", expectedDirectory, "openshift.yml"));
  }
}
