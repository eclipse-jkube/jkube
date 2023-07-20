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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

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
}
