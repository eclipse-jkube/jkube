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

import org.eclipse.jkube.gradle.plugin.task.KubernetesApplyTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesBuildTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesConfigViewTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesDebugTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesHelmPushTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesHelmTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesLogTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesPushTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesResourceTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesUndeployTask;

import org.eclipse.jkube.gradle.plugin.task.KubernetesWatchTask;
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

class KubernetesPluginRegisterTaskTest {

  static Stream<Arguments> data() {
    return Stream.of(
        arguments("k8sApply", KubernetesApplyTask.class),
        arguments("k8sBuild", KubernetesBuildTask.class),
        arguments("k8sConfigView", KubernetesConfigViewTask.class),
        arguments("k8sDebug", KubernetesDebugTask.class),
        arguments("k8sLog", KubernetesLogTask.class),
        arguments("k8sPush", KubernetesPushTask.class),
        arguments("k8sResource", KubernetesResourceTask.class),
        arguments("k8sUndeploy", KubernetesUndeployTask.class),
        arguments("k8sHelm", KubernetesHelmTask.class),
        arguments("k8sHelmPush", KubernetesHelmPushTask.class),
        arguments("k8sWatch", KubernetesWatchTask.class)
    );
  }

  private Project project;

  @BeforeEach
  void setUp() {
    project = mock(Project.class, RETURNS_DEEP_STUBS);
  }

  @ParameterizedTest(name = "{index}: with valid project, should create extension and register task ''{0}'' ")
  @MethodSource("data")
  void apply_withValidProject_shouldCreateExtensionAndRegisterTask(String task, Class<Task> taskClass) {
    // When
    new KubernetesPlugin().apply(project);
    // Then
    verify(project.getExtensions(), times(1))
        .create("kubernetes", KubernetesExtension.class);
    verify(project.getTasks(), times(1))
        .register(task, taskClass, KubernetesExtension.class);
  }
}
