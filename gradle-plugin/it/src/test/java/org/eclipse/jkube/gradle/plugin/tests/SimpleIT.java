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

import org.gradle.testkit.runner.BuildResult;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleIT {

  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Test
  public void tasks_containsKubernetesAndOpenShiftTasks() {
    // When
    final BuildResult result = gradleRunner.withITProject("simple").withArguments("tasks", "--stacktrace").build();
    // Then
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Help tasks")
        .contains("k8sConfigView - ")
        .contains("Kubernetes tasks")
        .contains("k8sApply - ")
        .contains("k8sBuild - ")
        .contains("k8sLog - ")
        .contains("k8sResource - ")
        .contains("k8sPush")
        .contains("k8sUndeploy - ")
        .contains("Openshift tasks")
        .contains("ocApply - ")
        .contains("ocBuild - ")
        .contains("ocLog - ")
        .contains("ocResource - ")
        .contains("ocPush")
        .contains("ocUndeploy - ");
  }

}
