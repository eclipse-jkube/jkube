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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KubernetesLogTaskTest {

  @Rule
  public TaskEnvironment taskEnvironment = new TaskEnvironment();

  private TestKubernetesExtension extension;

  @Before
  public void setUp() throws IOException {
    extension = new TestKubernetesExtension();
    extension.isUseColor = false;
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
  }

  @Test
  public void runTask_withNoK8sManifests_shouldLogCantWatchPods() {
    // Given
    KubernetesLogTask kubernetesLogTask = new KubernetesLogTask(KubernetesExtension.class);

    // When
    kubernetesLogTask.runTask();

    // Then
    verify(taskEnvironment.logger).warn("k8s: No selector detected and no Pod name specified, cannot watch Pods!");
  }

  @Test
  public void runTask_withNoManifestAndFailure_shouldThrowException() {
    // Given
    extension.isFailOnNoKubernetesJson = true;
    final KubernetesLogTask kubernetesLogTask = new KubernetesLogTask(KubernetesExtension.class);
    // When
    final IllegalStateException result = assertThrows(IllegalStateException.class, kubernetesLogTask::runTask);
    // Then
    assertThat(result)
        .hasMessageMatching("No such generated manifest file: .+kubernetes\\.yml");
  }

  @Test
  public void runTask_withIOException_shouldThrowException() {
    try (MockedStatic<KubernetesHelper> mockStatic = Mockito.mockStatic(KubernetesHelper.class)) {
      // Given
      mockStatic.when(() -> KubernetesHelper.loadResources(any())).thenThrow(new IOException("IO error with logs"));
      KubernetesLogTask kubernetesLogTask = new KubernetesLogTask(KubernetesExtension.class);
      // When
      final Exception result = assertThrows(GradleException.class, kubernetesLogTask::runTask);
      // Then
      assertThat(result).hasMessage("Failure in getting logs");
    }
  }
}
