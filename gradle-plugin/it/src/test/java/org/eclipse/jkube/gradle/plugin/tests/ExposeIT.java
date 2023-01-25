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

/**
 * Checks that Services and Ingresses/Routes are handled according to the following conditions:
 * <ul>
 *   <li>(no-ports) Image has no ports: Service and Ingress/Route are not created.</li>
 *   <li>(ftp-port) Image has a NON-WEB port: Service is created, Ingress/Route are not created.</li>
 *   <li>(http-port) Image has a WEB port: Service is created, Ingress is not created, Route is created.</li>
 *   <li>(http-port-external) Image has a WEB port, and create external URLs:
 *    Service is created, Ingress is created, Route is created.</li>
 *   <li>(ftp-port-external) Image has a NON-WEB port, and create external URLs:
 *    Service is created, Ingress is created, Route is created</li>
 *   <li>(ftp-port-expose-annotation) Image has a NON-WEB port, and expose annotation in service (from Fragment):
 *    Service is created, Ingress is not created, Route is created</li>
 *   <li>(ftp-port-expose-annotation-enricher) Image has a NON-WEB port, and expose annotation in service (from Service Enricher):
 *    Service is created, Ingress is not created, Route is created</li>
 *   <li>(multi-ports) Image has a WEB port, multiple services:
 *    Services are created, Ingresses are not created, Routes are created</li>
 * </ul>
 */
class ExposeIT {

  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  static Stream<Arguments> data() {
    return Stream.of(
        arguments("Image has no ports", "no-ports", new String[] { "-Pjkube.environment=none" }),
        arguments("Image has FTP port", "ftp-port", new String[] { "-Pexpose-it.port=21", "-Pjkube.environment=none" }),
        arguments("Image has Web port", "http-port", new String[] { "-Pexpose-it.port=80", "-Pjkube.environment=none" }),
        arguments("Image has Web port and createExternalUrls", "http-port-external",
            new String[] { "-Pexpose-it.port=80", "-Pjkube.createExternalUrls=true", "-Pjkube.environment=none" }),
        arguments("Image has FTP port and createExternalUrls", "ftp-port-external",
            new String[] { "-Pexpose-it.port=21", "-Pjkube.createExternalUrls=true", "-Pjkube.environment=none" }),
        arguments("Image has FTP port, and expose annotation in service (from Fragment)",
            "ftp-port-expose-annotation", new String[] { "-Pexpose-it.port=21", "-Pjkube.environment=label-expose" }),
        arguments("Image has FTP port, and expose annotation in service (from Service Enricher)",
            "ftp-port-expose-annotation-enricher",
            new String[] { "-Pexpose-it.port=21", "-Pjkube.enricher.jkube-service.expose=true", "-Pjkube.environment=none" }),
        arguments("Image has HTTPS port, and multiple services", "multiple-services",
            new String[] { "-Pexpose-it.port=443", "-Pjkube.environment=multiple-services" })
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("data")
  void k8sResource_whenRun_generatesK8sManifestsWithProjectLabels(String description, String expectedDir, String[] arguments) throws IOException, ParseException {
    // When
    List<String> gradleArgs = new ArrayList<>(Arrays.asList(arguments));
    gradleArgs.add("k8sResource");
    gradleArgs.add("--stacktrace");
    final BuildResult result = gradleRunner.withITProject("expose")
      .withArguments(gradleArgs.toArray(new String[0]))
      .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
      gradleRunner.resolveFile("expected", expectedDir, "kubernetes.yml"), true); // n.b. strict mode
    assertThat(result).extracting(BuildResult::getOutput).asString()
      .contains("Using resource templates from")
      .contains("Adding revision history limit to 2")
      .contains("validating");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("data")
  void ocResource_whenRun_generatesOpenShiftManifestsWithProjectLabels(String description, String expectedDir, String[] arguments) throws IOException, ParseException {
    // When
    List<String> gradleArgs = new ArrayList<>(Arrays.asList(arguments));
    gradleArgs.add("ocResource");
    gradleArgs.add("--stacktrace");
    final BuildResult result = gradleRunner.withITProject("expose")
      .withArguments(gradleArgs.toArray(new String[0]))
      .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
      gradleRunner.resolveFile("expected", expectedDir, "openshift.yml"), true); // n.b. strict mode
    assertThat(result).extracting(BuildResult::getOutput).asString()
      .contains("Using resource templates from")
      .contains("Adding revision history limit to 2")
      .contains("validating");
  }
}
