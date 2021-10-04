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
import java.util.Collections;

import org.eclipse.jkube.gradle.plugin.GradleLogger;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.config.service.DebugService;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KubernetesDebugTaskTest {

  @Rule
  public TaskEnvironment taskEnvironment = new TaskEnvironment();

  private MockedConstruction<DebugService> debugServiceMockedConstruction;
  private TestKubernetesExtension extension;

  @Before
  public void setUp() throws IOException {
    debugServiceMockedConstruction = mockConstruction(DebugService.class);
    extension = new TestKubernetesExtension();
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
  }

  @After
  public void tearDown() {
    debugServiceMockedConstruction.close();
  }

  @Test
  public void runTask_withNoManifest_shouldThrowException() {
    // Given
    extension.isFailOnNoKubernetesJson = true;
    final KubernetesDebugTask debugTask = new KubernetesDebugTask(KubernetesExtension.class);
    // When
    final IllegalStateException result = assertThrows(IllegalStateException.class, debugTask::runTask);
    // Then
    assertThat(result)
        .hasMessageMatching("No such generated manifest file: .+kubernetes\\.yml");
  }

  @Test
  public void runTask_withManifest_shouldStartDebug() throws Exception {
    // Given
    taskEnvironment.withKubernetesManifest();
    final KubernetesDebugTask debugTask = new KubernetesDebugTask(KubernetesExtension.class);
    // When
    debugTask.runTask();
    // Then
    assertThat(debugServiceMockedConstruction.constructed()).hasSize(1);
    verify(debugServiceMockedConstruction.constructed().iterator().next(), times(1))
        .debug(any(), eq("kubernetes.yml"), eq(Collections.emptyList()), eq("5005"), eq(false), any(GradleLogger.class));
  }

}
