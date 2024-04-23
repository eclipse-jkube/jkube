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

import java.util.Collections;

import org.eclipse.jkube.gradle.plugin.GradleLogger;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.config.service.DebugContext;
import org.eclipse.jkube.kit.config.service.DebugService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KubernetesDebugTaskTest {

  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private MockedConstruction<DebugService> debugServiceMockedConstruction;
  private TestKubernetesExtension extension;

  @BeforeEach
  void setUp() {
    debugServiceMockedConstruction = mockConstruction(DebugService.class);
    extension = new TestKubernetesExtension();
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
  }

  @AfterEach
  void tearDown() {
    debugServiceMockedConstruction.close();
  }

  @Test
  void runTask_withNoManifest_shouldThrowException() {
    // Given
    extension.isFailOnNoKubernetesJson = true;
    final KubernetesDebugTask debugTask = new KubernetesDebugTask(KubernetesExtension.class);
    // When & Then
    assertThatIllegalStateException()
        .isThrownBy(debugTask::runTask)
        .withMessageMatching("No such generated manifest file: .+kubernetes\\.yml");
  }

  @Test
  void runTask_withManifest_shouldStartDebug() throws Exception {
    // Given
    taskEnvironment.withKubernetesManifest();
    final KubernetesDebugTask debugTask = new KubernetesDebugTask(KubernetesExtension.class);
    ArgumentCaptor<DebugContext> debugContextArgumentCaptor = ArgumentCaptor.forClass(DebugContext.class);
    // When
    debugTask.runTask();
    // Then
    assertThat(debugServiceMockedConstruction.constructed()).hasSize(1);
    verify(debugServiceMockedConstruction.constructed().iterator().next(), times(1))
        .debug(debugContextArgumentCaptor.capture(), eq(Collections.emptyList()));
    assertThat(debugContextArgumentCaptor.getValue())
        .hasFieldOrPropertyWithValue("fileName", "kubernetes.yml")
        .hasFieldOrPropertyWithValue("localDebugPort", "5005")
        .hasFieldOrPropertyWithValue("debugSuspend", false)
        .satisfies(d -> assertThat(d.getPodWaitLog()).isInstanceOf(GradleLogger.class));
  }

}
