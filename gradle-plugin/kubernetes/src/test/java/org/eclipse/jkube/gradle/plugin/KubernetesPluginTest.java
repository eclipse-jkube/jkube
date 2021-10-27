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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jkube.gradle.plugin.task.JKubeTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesApplyTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesBuildTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesHelmTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesResourceTask;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KubernetesPluginTest {

  private Project project;

  @Before
  public void setUp() {
    project = mock(Project.class, RETURNS_DEEP_STUBS);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void apply_withValidProject_shouldConfigureTasks() {
    // Given
    final JKubeTask mockTask = mock(JKubeTask.class, RETURNS_DEEP_STUBS);
    final AtomicReference<Action<? super Task>> action = new AtomicReference<>();
    doAnswer(i -> {
      ((Action<? super Project>)i.getArgument(0)).execute(project);
      return null;
    }).when(project).afterEvaluate(any(Action.class));
    final TaskProvider<Task> taskProvider = mock(TaskProvider.class);
    when(project.getTasks().register(anyString(), any(Class.class), any(Class.class)))
        .thenReturn(taskProvider);
    doAnswer(i -> {
      action.set(i.getArgument(0));
      return null;
    }).when(taskProvider).configure(any(Action.class));
    new KubernetesPlugin().apply(project);
    // When
    action.get().execute(mockTask);
    // Then
    verify(mockTask, times(1)).setGroup("kubernetes");
  }

  @Test
  public void getTaskPrecedence_withValidProject_shouldReturnTaskPrecedence() {
    //When
    final Map<String, Collection<Class<? extends Task>>> result = new KubernetesPlugin().getTaskPrecedence();
    // Then
    assertThat(result)
        .hasSize(5)
        .containsEntry("k8sApply", Collections.singletonList(KubernetesResourceTask.class))
        .containsEntry("k8sDebug",
            Arrays.asList(KubernetesBuildTask.class, KubernetesResourceTask.class, KubernetesApplyTask.class))
        .containsEntry("k8sPush", Collections.singletonList(KubernetesBuildTask.class))
        .containsEntry("k8sHelm", Collections.singletonList(KubernetesResourceTask.class))
        .containsEntry("k8sHelmPush", Collections.singletonList(KubernetesHelmTask.class));
  }
}
