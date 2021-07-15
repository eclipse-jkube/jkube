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

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;

import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

public class KubernetesApplyTaskTest {

  private MockedConstruction<DefaultTask> defaultTaskMockedConstruction;
  private Project project;

  @Before
  public void setUp() {
    project = mock(Project.class, RETURNS_DEEP_STUBS);
    defaultTaskMockedConstruction = mockConstruction(DefaultTask.class,
        (mock, ctx) -> when(mock.getProject()).thenReturn(project));
    final KubernetesExtension extension = mock(KubernetesExtension.class, RETURNS_DEEP_STUBS);
    when(project.getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getBuildscript().getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getProperties()).thenReturn(Collections.emptyMap());
    when(project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    when(extension.getOffline().getOrElse(false)).thenReturn(true);
  }

  @After
  public void tearDown() {
    defaultTaskMockedConstruction.close();
  }

  @Test
  public void run_withImplementationPending_shouldThrowException() {
    // Given
    final KubernetesApplyTask applyTask = new KubernetesApplyTask(KubernetesExtension.class);
    // When
    final UnsupportedOperationException result = assertThrows(UnsupportedOperationException.class, applyTask::runTask);
    // Then
    assertThat(result).hasMessageContaining("To be implemented");
  }
}
