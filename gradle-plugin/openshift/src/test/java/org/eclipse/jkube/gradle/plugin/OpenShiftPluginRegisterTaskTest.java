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

import java.util.stream.Stream;

import org.eclipse.jkube.gradle.plugin.task.KubernetesConfigViewTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesLogTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftApplyTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftBuildTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftDebugTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftHelmPushTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftHelmTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftPushTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftRemoteDevTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftResourceTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftUndeployTask;

import org.eclipse.jkube.gradle.plugin.task.OpenShiftWatchTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class OpenShiftPluginRegisterTaskTest {

  static Stream<Arguments> data() {
    return Stream.of(
        arguments("ocApply", OpenShiftApplyTask.class),
        arguments("ocBuild", OpenShiftBuildTask.class),
        arguments("ocConfigView", KubernetesConfigViewTask.class),
        arguments("ocDebug", OpenShiftDebugTask.class),
        arguments("ocLog", KubernetesLogTask.class),
        arguments("ocPush", OpenShiftPushTask.class),
        arguments("ocResource", OpenShiftResourceTask.class),
        arguments("ocUndeploy", OpenShiftUndeployTask.class),
        arguments("ocHelm", OpenShiftHelmTask.class),
        arguments("ocHelmPush", OpenShiftHelmPushTask.class),
        arguments("ocRemoteDev", OpenShiftRemoteDevTask.class),
        arguments("ocWatch", OpenShiftWatchTask.class));
  }

  private Project project;

  @BeforeEach
  void setUp() {
    project = mock(Project.class, RETURNS_DEEP_STUBS);
  }

  @ParameterizedTest(name = "{index}: with valid project, should create extension and register task ''{0}''")
  @MethodSource("data")
  void apply_withValidProject_shouldCreateExtensionAndRegisterTask(String task, Class<Task> taskClass) {
    // When
    new OpenShiftPlugin().apply(project);
    // Then
    verify(project.getExtensions(), times(1))
        .create("openshift", OpenShiftExtension.class);
    verify(project.getTasks(), times(1))
        .register(task, taskClass, OpenShiftExtension.class);
  }

}
