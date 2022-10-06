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
package org.eclipse.jkube.gradle.plugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.eclipse.jkube.gradle.plugin.task.KubernetesApplyTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesBuildTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesHelmTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesResourceTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftApplyTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftBuildTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftHelmTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftResourceTask;

import org.gradle.api.Task;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenShiftPluginTest {

  @Test
  void configurePrecedence_withValidProject_shouldReturnTaskPrecedence() {
    //When
    final Map<String, Collection<Class<? extends Task>>> result = new OpenShiftPlugin().getTaskPrecedence();
    // Then
    assertThat(result)
        .hasSize(5)
        .containsEntry("ocApply", Arrays.asList(KubernetesResourceTask.class, OpenShiftResourceTask.class))
        .containsEntry("ocDebug", Arrays.asList(KubernetesBuildTask.class, OpenShiftBuildTask.class,
            KubernetesResourceTask.class, OpenShiftResourceTask.class, KubernetesApplyTask.class, OpenShiftApplyTask.class))
        .containsEntry("ocPush", Arrays.asList(KubernetesBuildTask.class, OpenShiftBuildTask.class))
      .containsEntry("ocHelm", Arrays.asList(KubernetesResourceTask.class, OpenShiftResourceTask.class))
      .containsEntry("ocHelmPush", Arrays.asList(KubernetesHelmTask.class, OpenShiftHelmTask.class));
  }
}
