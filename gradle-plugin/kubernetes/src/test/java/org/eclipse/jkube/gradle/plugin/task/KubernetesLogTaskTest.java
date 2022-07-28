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
package org.eclipse.jkube.gradle.plugin.task;

import java.io.IOException;

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.gradle.api.internal.provider.DefaultProperty;
import org.gradle.api.provider.Property;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KubernetesLogTaskTest {

  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private TestKubernetesExtension extension;

  @BeforeEach
  void setUp() throws IOException {
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
    extension = new TestKubernetesExtension() {
      @Override
      public Property<Boolean> getSummaryEnabled() {
        return new DefaultProperty<>(Boolean.class).value(false);
      }
    };
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
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
      extension = new TestKubernetesExtension() {
        @Override
        public Property<Boolean> getSummaryEnabled() {
          return new DefaultProperty<>(Boolean.class).value(false);
        }
      };
      when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
      KubernetesLogTask kubernetesLogTask = new KubernetesLogTask(KubernetesExtension.class);
      // When & Then
      assertThatExceptionOfType(GradleException.class)
          .isThrownBy(kubernetesLogTask::runTask)
          .withMessage("Failure in getting logs");
    }
  }
}
