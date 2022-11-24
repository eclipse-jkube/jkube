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
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Locale;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class KubernetesConfiguredControllerIT {
  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @ParameterizedTest(name = "k8sResource with {0} configured controller should have {0} in generated resource list")
  @ValueSource(strings = { "DaemonSet", "Deployment", "Job", "ReplicaSet", "ReplicationController", "StatefulSet" })
  void k8sResourceTask_whenRunWithConfiguredControllerType_generatesK8sManifestWithExpectedController(String controllerType) throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("controller")
        .withArguments("-Pjkube.enricher.jkube-controller.type=" + controllerType, "k8sResource")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", controllerType.toLowerCase(Locale.ROOT), "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding a default " + controllerType)
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }
}
