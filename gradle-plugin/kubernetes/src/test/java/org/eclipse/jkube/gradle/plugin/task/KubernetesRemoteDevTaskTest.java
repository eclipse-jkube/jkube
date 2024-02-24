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
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedConstruction;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KubernetesRemoteDevTaskTest {

  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private KubernetesRemoteDevTask task;
  private MockedConstruction<RemoteDevelopmentService> remoteDevelopmentService;
  private CompletableFuture<Void> started;
  private Runnable onStart;

  @BeforeEach
  void setUp() {
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(new TestKubernetesExtension());
    task = new KubernetesRemoteDevTask(KubernetesExtension.class);
    task.jKubeServiceHub = mock(JKubeServiceHub.class, RETURNS_DEEP_STUBS);
    started = new CompletableFuture<>();
    remoteDevelopmentService = mockConstruction(RemoteDevelopmentService.class, (mock, ctx) ->
      when(mock.start()).then(invocation -> {
        onStart.run();
        return started;
      })
    );
  }

  @AfterEach
  void tearDown() throws Exception {
    remoteDevelopmentService.close();
  }

  @Test
  void runTask_shouldStart() {
    // Given
    onStart = () -> started.complete(null);
    // When
    task.runTask();
    // Then
    assertThat(started).isCompleted();
  }

  @Test
  void runTask_withException_shouldStop() {
    // Given
    onStart = () -> started.completeExceptionally(new Exception("The Exception"));
    // When
    task.runTask();
    // Then
    assertThat(remoteDevelopmentService.constructed())
      .singleElement()
      .satisfies(remoteDevelopmentService -> verify(remoteDevelopmentService, times(1)).stop());
  }
}
