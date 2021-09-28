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

import org.eclipse.jkube.gradle.plugin.task.KubernetesBuildTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesResourceTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftApplyTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftBuildTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftResourceTask;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OpenShiftPluginTest {

  private Project project;

  @Before
  public void setUp() {
    project = mock(Project.class, RETURNS_DEEP_STUBS);
  }

  @Test
  public void apply_withValidProject_shouldCreateExtensionAndRegisterTasks() {
    // When
    new OpenShiftPlugin().apply(project);
    // Then
    verify(project.getExtensions(), times(1))
        .create("openshift", OpenShiftExtension.class);
    verify(project.getTasks(), times(1))
        .register("ocBuild", OpenShiftBuildTask.class, OpenShiftExtension.class);
    verify(project.getTasks(), times(1))
        .register("ocResource", OpenShiftResourceTask.class, OpenShiftExtension.class);
    verify(project.getTasks(), times(1))
        .register("ocApply", OpenShiftApplyTask.class, OpenShiftExtension.class);
  }

  @Test
  public void configurePrecedence_withValidProject_shouldReturnTaskPrecedence() {
    //When
    final Map<String, Collection<Class<? extends Task>>> result = new OpenShiftPlugin().getTaskPrecedence();
    // Then
    assertThat(result)
        .hasSize(2)
        .containsEntry("ocApply", Arrays.asList(KubernetesResourceTask.class, OpenShiftResourceTask.class))
        .containsEntry("ocPush", Arrays.asList(KubernetesBuildTask.class, OpenShiftBuildTask.class));
  }
}
