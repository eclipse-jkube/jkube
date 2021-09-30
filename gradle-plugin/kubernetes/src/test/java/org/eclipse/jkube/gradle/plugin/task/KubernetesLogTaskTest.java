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

  @Before
  public void setUp() throws IOException {
    TestKubernetesExtension extension = new TestKubernetesExtension();
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
    verify(taskEnvironment.logger).lifecycle("k8s: Running in Kubernetes mode");
    verify(taskEnvironment.logger).warn("k8s: No selector in deployment so cannot watch pods!");
  }

  @Test
  public void runTask_withManifestLoadException_shouldThrowException() {
    try (MockedStatic<KubernetesHelper> mockStatic = Mockito.mockStatic(KubernetesHelper.class)) {
      // Given
      KubernetesLogTask kubernetesLogTask = new KubernetesLogTask(KubernetesExtension.class);

      mockStatic.when(() -> KubernetesHelper.loadResources(any())).thenThrow(new IOException("Can't load manifests"));

      // When
      IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, kubernetesLogTask::runTask);

      // Then
      assertThat(illegalStateException).hasMessage("Failure in getting logs");
    }
  }
}
