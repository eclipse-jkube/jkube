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

import org.eclipse.jkube.gradle.plugin.task.KubernetesApplyTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesBuildTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesConfigViewTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesDebugTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesLogTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesPushTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesResourceTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesUndeployTask;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public class KubernetesPluginRegisterTaskTest {

  @Parameterized.Parameters(name = "{index} {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "k8sApply", KubernetesApplyTask.class },
        new Object[] { "k8sBuild", KubernetesBuildTask.class },
        new Object[] { "k8sConfigView", KubernetesConfigViewTask.class },
        new Object[] { "k8sDebug", KubernetesDebugTask.class },
        new Object[] { "k8sLog", KubernetesLogTask.class },
        new Object[] { "k8sPush", KubernetesPushTask.class },
        new Object[] { "k8sResource", KubernetesResourceTask.class },
        new Object[] { "k8sUndeploy", KubernetesUndeployTask.class });
  }

  @Parameterized.Parameter
  public String task;

  @Parameterized.Parameter(1)
  public Class<Task> taskClass;

  private Project project;

  @Before
  public void setUp() {
    project = mock(Project.class, RETURNS_DEEP_STUBS);
  }

  @Test
  public void apply_withValidProject_shouldCreateExtensionAndRegisterTask() {
    // When
    new KubernetesPlugin().apply(project);
    // Then
    verify(project.getExtensions(), times(1))
        .create("kubernetes", KubernetesExtension.class);
    verify(project.getTasks(), times(1))
        .register(task, taskClass, KubernetesExtension.class);
  }
}
