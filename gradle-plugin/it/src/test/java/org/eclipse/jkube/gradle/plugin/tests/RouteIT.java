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

class RouteIT {
  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  static Stream<Arguments> data() {
    return Stream.of(
        arguments("routeDisabled", "false", 2),
        arguments("routeEnabled", "true", 3)
    );
  }

  @ParameterizedTest(name = "ocResource {0} with jkube.openshift.generateRoute={1}")
  @MethodSource("data")
  void ocResource_whenRunWithRouteFlag_generatesOpenShiftManifests(String expectedDir, String routeEnabled, int expectedGeneratedFiles) throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("route")
        .withArguments("clean", "-Pjkube.openshift.generateRoute=" + routeEnabled, "ocResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", expectedDir, "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding a default Deployment")
        .contains("Adding revision history limit to ")
        .contains("validating");
    assertThat(gradleRunner.resolveDefaultOpenShiftResourceDir().listFiles())
        .hasSize(expectedGeneratedFiles);
  }
}
