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
package org.eclipse.jkube.maven.plugin.mojo.develop;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemoteDevMojoTest {

  private RemoteDevMojo remoteDevMojo;
  private MockedConstruction<RemoteDevelopmentService> remoteDevelopmentService;
  private CompletableFuture<Void> started;
  private Runnable onStart;

  @BeforeEach
  void setUp() {
    final MavenProject mavenProject = mock(MavenProject.class);
    final MavenSession mavenSession = mock(MavenSession.class);
    final MojoExecution mavenMojoExecution = mock(MojoExecution.class);
    when(mavenSession.getGoals()).thenReturn(Collections.singletonList("k8s:remote-dev"));
    when(mavenMojoExecution.getGoal()).thenReturn("k8s:remote-dev");
    when(mavenProject.getProperties()).thenReturn(new Properties());
    remoteDevMojo = new RemoteDevMojo() {{
      project = mavenProject;
      settings = mock(Settings.class, RETURNS_DEEP_STUBS);
      mojoExecution = mavenMojoExecution;
      session = mavenSession;
    }};
    started = new CompletableFuture<>();
    remoteDevelopmentService = mockConstruction(RemoteDevelopmentService.class, (mock, ctx) ->
      when(mock.start()).then(invocation -> {
        onStart.run();
        return started;
      })
    );
  }

  @AfterEach
  void tearDown() {
    remoteDevelopmentService.close();
  }

  @Test
  @DisplayName("execute, should start remote development service")
  void execute_shouldStart() throws Exception {
    // Given
    onStart = () -> started.complete(null);
    // When
    remoteDevMojo.execute();
    // Then
    assertThat(started).isCompleted();
  }

  @Test
  @DisplayName("execute, with exception, should stop remote development service")
  void execute_withException_shouldStop() throws Exception {
    // Given
    onStart = () -> started.completeExceptionally(new Exception("The Exception"));
    // When
    remoteDevMojo.execute();
    // Then
    assertThat(remoteDevelopmentService.constructed())
      .singleElement()
      .satisfies(remoteDevelopmentService -> verify(remoteDevelopmentService, times(1)).stop());
  }
}
