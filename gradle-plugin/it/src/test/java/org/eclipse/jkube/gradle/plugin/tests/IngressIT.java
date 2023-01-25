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

class IngressIT {
  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  static Stream<Arguments> data() {
    return Stream.of(
        arguments("zero-config-no-host", "", "", "", "false"),
        arguments("zero-config-host-enricher-config", "test.example.com", "", "", "false"),
        arguments("zero-config-extensionsv1beta1-enricher-config", "", "extensions/v1beta1", "", "false"),
        arguments("zero-config-extensionsv1beta1-host-enricher-config", "test.example.com", "extensions/v1beta1", "", "false"),
        arguments("groovy-dsl-config", "", "", "org.eclipse.jkube.quickstart", "true")
    );
  }

  @ParameterizedTest(name = "k8sResource with {0}")
  @MethodSource("data")
  void k8sResourceTask_whenRun_generatesK8sManifestWithIngress(String profileName, String host, String targetApiVersion, String domain, String groovyDslEnabled) throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("ingress")
        .withArguments("k8sResource",
            "-Pjkube.enricher.jkube-ingress.host=" + host,
            "-Pjkube.enricher.jkube-ingress.targetApiVersion=" + targetApiVersion,
            "-Pjkube.domain=" + domain,
            "-Pgroovy-dsl-config=" + groovyDslEnabled,
            "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", profileName, "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding a default Deployment")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }
}
