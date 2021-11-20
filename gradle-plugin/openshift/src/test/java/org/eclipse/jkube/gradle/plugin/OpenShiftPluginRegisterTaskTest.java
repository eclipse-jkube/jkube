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

import org.eclipse.jkube.gradle.plugin.task.KubernetesConfigViewTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesLogTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftApplyTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftBuildTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftDebugTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftHelmPushTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftHelmTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftPushTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftResourceTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftUndeployTask;

import org.eclipse.jkube.gradle.plugin.task.OpenShiftWatchTask;
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
public class OpenShiftPluginRegisterTaskTest {

  @Parameterized.Parameters(name = "{index} {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "ocApply", OpenShiftApplyTask.class },
        new Object[] { "ocBuild", OpenShiftBuildTask.class },
        new Object[] { "ocConfigView", KubernetesConfigViewTask.class },
        new Object[] { "ocDebug", OpenShiftDebugTask.class },
        new Object[] { "ocLog", KubernetesLogTask.class },
        new Object[] { "ocPush", OpenShiftPushTask.class },
        new Object[] { "ocResource", OpenShiftResourceTask.class },
        new Object[] { "ocUndeploy", OpenShiftUndeployTask.class },
        new Object[] { "ocHelm", OpenShiftHelmTask.class},
        new Object[] { "ocHelmPush", OpenShiftHelmPushTask.class},
        new Object[] { "ocWatch", OpenShiftWatchTask.class});
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
    new OpenShiftPlugin().apply(project);
    // Then
    verify(project.getExtensions(), times(1))
        .create("openshift", OpenShiftExtension.class);
    verify(project.getTasks(), times(1))
        .register(task, taskClass, OpenShiftExtension.class);
  }

}
