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

class NamespaceIT {
  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  static Stream<Arguments> data() {
    return Stream.of(
        arguments("default", "", ""),
        arguments("set-namespace", "namespace-to-operate", ""),
        arguments("create-namespace", "", "namespace-to-create-name"),
        arguments("create-and-set-namespace", "namespace-to-create-and-operate", "namespace-to-create-and-operate"),
        arguments("create-and-set-different-namespace", "namespace-to-operate", "namespace-to-create-name")
    );
  }

  @ParameterizedTest(name = "{0} : jkube.namespace = {1}, jkube.enricher.jkube-namespace.namespace={2}")
  @MethodSource("data")
  void k8sResourceTask_whenRunWithConfiguredWithNamespace_generatesK8sManifestWithExpectedNamespace(String expectedDirectory, String namespaceViaJKubeNamespaceProperty, String namespaceViaNamespaceEnricherConfig) throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("namespace")
        .withArguments("-Pjkube.enricher.jkube-namespace.namespace=" + namespaceViaNamespaceEnricherConfig,
            "-Pjkube.namespace=" + namespaceViaJKubeNamespaceProperty,
            "k8sResource")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", expectedDirectory, "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }

  @ParameterizedTest(name = "{0} : jkube.namespace = {1}, jkube.enricher.jkube-namespace.namespace={2}")
  @MethodSource("data")
  void ocResourceTask_whenRunWithConfiguredWithNamespace_generatesOpenShiftManifestWithExpectedNamespace(String expectedDirectory, String namespaceViaJKubeNamespaceProperty, String namespaceViaNamespaceEnricherConfig) throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("namespace")
        .withArguments("-Pjkube.enricher.jkube-namespace.namespace=" + namespaceViaNamespaceEnricherConfig,
            "-Pjkube.namespace=" + namespaceViaJKubeNamespaceProperty,
            "ocResource")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", expectedDirectory, "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }
}
