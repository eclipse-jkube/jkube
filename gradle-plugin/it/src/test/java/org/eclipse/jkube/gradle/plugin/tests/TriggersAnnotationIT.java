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
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class TriggersAnnotationIT {
  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @Test
  void k8sResourceTask_whenRun_generatesK8sManifestWithTriggersAnnotation() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("triggersannotation")
        .withArguments("k8sResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding a default ReplicaSet")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }

  @Test
  void ocResourceTask_whenRun_generatesOpenShiftManifestWithTriggersAnnotation() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("triggersannotation")
        .withArguments("ocResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding a default ReplicaSet")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }
}
