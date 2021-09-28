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
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KubernetesConfigViewTaskTest {

  @Rule
  public TaskEnvironment taskEnvironment = new TaskEnvironment();

  private KubernetesExtension extension;

  @Before
  public void setUp() throws IOException {
    extension = new TestKubernetesExtension();
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
  }

  @Test
  public void runTask_withImplementationPending_shouldThrowException() {
    // Given
    extension.buildStrategy = JKubeBuildStrategy.s2i;
    extension.getOffline().set(true);
    final KubernetesConfigViewTask configViewTask = new KubernetesConfigViewTask(KubernetesExtension.class);
    // When
    configViewTask.runTask();
    // Then
    verify(taskEnvironment.logger, times(1))
        .lifecycle(matches("k8s: \n---\noffline: true\nbuildStrategy: \"s2i\""));
  }
}