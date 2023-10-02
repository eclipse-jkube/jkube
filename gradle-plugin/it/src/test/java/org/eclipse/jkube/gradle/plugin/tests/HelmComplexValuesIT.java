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

import org.eclipse.jkube.kit.common.ResourceVerify;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class HelmComplexValuesIT {

  @RegisterExtension
  public final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @BeforeEach
  void setUp() {
    gradleRunner.withITProject("helm-complex-values");
  }
  @Test
  void outerLoop() throws Exception {
    // When
    gradleRunner.withArguments("clean", "k8sResource", "k8sHelm").build();
    // Then
    ResourceVerify.verifyResourceDescriptors(
      gradleRunner.resolveFile("build", "jkube", "helm", "helm-complex-values", "kubernetes", "values.yaml"),
      gradleRunner.resolveFile("expected", "values.yaml"));
    assertThat(ResourceVerify.readFile(gradleRunner.resolveFile(
      "build", "jkube", "helm", "helm-complex-values", "kubernetes", "templates", "helm-complex-values-deployment.yaml")))
      .contains("annotations: {{- toYaml .Values.common.annotations | nindent 4 }}\n" +
        "                        {{- toYaml .Values.deployment.annotations | nindent 4 }}")
      .contains("hostAliases: {{- toYaml .Values.deployment.hostAliases | nindent 8 }}")
      .contains("replicas: {{ .Values.replicaCount }}");
    assertThat(ResourceVerify.readFile(gradleRunner.resolveFile(
      "build", "jkube", "helm", "helm-complex-values", "kubernetes", "templates", "helm-complex-values-service.yaml")))
      .contains("annotations: {{- toYaml .Values.common.annotations | nindent 4 }}\n" +
        "                        {{- toYaml .Values.service.annotations | nindent 4 }}");
  }

  @Test
  void innerLoop() throws Exception {
    try {
      // Given
      // gradle.properties with properties to resolve placeholders in fragments
      Files.copy(gradleRunner.resolveFile("gradle.dev.properties").toPath(),
        gradleRunner.resolveFile("gradle.properties").toPath());
      // When
      gradleRunner.withArguments("clean", "k8sResource").build();
      // Then
      ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", "inner-loop.yaml"));
    } finally {
      Files.deleteIfExists(gradleRunner.resolveFile("gradle.properties").toPath());
    }
  }

}
