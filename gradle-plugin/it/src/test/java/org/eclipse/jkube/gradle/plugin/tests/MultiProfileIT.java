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

import java.io.IOException;

import org.eclipse.jkube.kit.common.ResourceVerify;

import net.minidev.json.parser.ParseException;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MultiProfileIT {
  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @Test
  void ocResource_whenRunMutliFragments_generatesManifests() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("multi-env-same-kind")
        .withArguments("build", "ocResource")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", "openshift.yml"));
  }

  @Test
  void ocResource_whenRunMutliFragmentsProfileOverridden_generatesManifests() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("multi-env-same-kind-profile-overridden")
        .withArguments("build", "ocResource")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", "openshift.yml"));
  }
}
