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

import java.io.IOException;

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KubernetesLogTaskTest {

  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private TestKubernetesExtension extension;

  @BeforeEach
  void setUp() {
    extension = new TestKubernetesExtension();
    extension.isUseColor = false;
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
  }

  @Test
  void runTask_withNoK8sManifests_shouldLogCantWatchPods() {
    // Given
    KubernetesLogTask kubernetesLogTask = new KubernetesLogTask(KubernetesExtension.class);

    // When
    kubernetesLogTask.runTask();

    // Then
    verify(taskEnvironment.logger).warn("k8s: No selector detected and no Pod name specified, cannot watch Pods!");
  }

  @Test
  void runTask_withNoManifestAndFailure_shouldThrowException() {
    // Given
    extension.isFailOnNoKubernetesJson = true;
    final KubernetesLogTask kubernetesLogTask = new KubernetesLogTask(KubernetesExtension.class);
    // When & Then
    assertThatIllegalStateException()
        .isThrownBy(kubernetesLogTask::runTask)
        .withMessageMatching("No such generated manifest file: .+kubernetes\\.yml");
  }

  @Test
  void runTask_withIOException_shouldThrowException() {
    try (MockedStatic<KubernetesHelper> mockStatic = Mockito.mockStatic(KubernetesHelper.class)) {
      // Given
      mockStatic.when(() -> KubernetesHelper.loadResources(any())).thenThrow(new IOException("IO error with logs"));
      KubernetesLogTask kubernetesLogTask = new KubernetesLogTask(KubernetesExtension.class);
      // When & Then
      assertThatExceptionOfType(GradleException.class)
          .isThrownBy(kubernetesLogTask::runTask)
          .withMessage("Failure in getting logs");
    }
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
    final KubernetesLogTask kubernetesLogTask = new KubernetesLogTask(KubernetesExtension.class);
    when(kubernetesLogTask.getName()).thenReturn("k8sLog");

    // When
    kubernetesLogTask.runTask();

    // Then
    verify(taskEnvironment.logger, never()).warn(anyString());
    verify(taskEnvironment.logger, times(1)).lifecycle(contains("k8s: `k8sLog` task is skipped."));
  }
}
