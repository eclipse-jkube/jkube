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
package org.eclipse.jkube.gradle.plugin.task;

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.gradle.api.provider.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KubernetesConfigViewTaskTest {

  @RegisterExtension
  final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private TestKubernetesExtension extension;

  @BeforeEach
  void setUp() {
    extension = new TestKubernetesExtension();
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
  }

  @Test
  void runTask_withManualSettings_shouldLogThem() {
    // Given
    extension.buildStrategy = JKubeBuildStrategy.s2i;
    extension.isOffline = true;
    final KubernetesConfigViewTask configViewTask = new KubernetesConfigViewTask(KubernetesExtension.class);
    // When
    configViewTask.runTask();
    // Then
    verify(taskEnvironment.logger, times(1))
      .lifecycle(matches(String.format("k8s: %n---\noffline: true\nbuildStrategy: \"s2i\"")));
  }

  @Test
  void runTask_withSkip_shouldDoNothing() {
    // Given
    extension = new TestKubernetesExtension() {
      @Override
      public Property<Boolean> getSkip() {
        return super.getSkip().value(true);
      }
    };
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    final KubernetesConfigViewTask kubernetesConfigViewTask = new KubernetesConfigViewTask(KubernetesExtension.class);
    when(kubernetesConfigViewTask.getName()).thenReturn("k8sConfigView");

    // When
    kubernetesConfigViewTask.runTask();

    // Then
    verify(taskEnvironment.logger, times(1)).lifecycle(contains("k8s: `k8sConfigView` task is skipped."));
  }
}
