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

import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
import org.gradle.api.provider.Property;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenShiftRemoteDevTaskTest {
  @SuppressWarnings("unused")
  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  @Test
  void getLogPrefix_whenInvoked_shouldReturnOpenShiftPrefix() {
    // Given
    OpenShiftRemoteDevTask openShiftRemoteDevTask = new OpenShiftRemoteDevTask(OpenShiftExtension.class);

    // When
    String logPrefix = openShiftRemoteDevTask.getLogPrefix();

    // Then
    assertThat(logPrefix).isEqualTo("oc: ");
  }

  @Test
  void runTask_withSkip_shouldDoNothing() {
    // Given
    TestOpenShiftExtension extension = new TestOpenShiftExtension() {
      @Override
      public Property<Boolean> getSkip() {
        return super.getSkip().value(true);
      }
    };
    when(taskEnvironment.project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
    final OpenShiftRemoteDevTask task = new OpenShiftRemoteDevTask(OpenShiftExtension.class);
    when(task.getName()).thenReturn("ocRemoteDev");

    // When
    task.runTask();

    // Then
    verify(taskEnvironment.logger, times(1)).lifecycle(contains("oc: `ocRemoteDev` task is skipped."));
  }
}
