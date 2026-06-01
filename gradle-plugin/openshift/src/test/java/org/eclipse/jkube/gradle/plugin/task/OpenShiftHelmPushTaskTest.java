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
import org.eclipse.jkube.kit.resource.helm.BadUploadException;
import org.eclipse.jkube.kit.resource.helm.HelmService;
import org.gradle.api.provider.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedConstruction;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenShiftHelmPushTaskTest {
  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private MockedConstruction<HelmService> helmServiceMockedConstruction;

  @BeforeEach
  void setUp() {
    TestOpenShiftExtension extension = new TestOpenShiftExtension();
    helmServiceMockedConstruction = mockConstruction(HelmService.class);
    when(taskEnvironment.project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
  }

  @AfterEach
  void tearDown() {
    helmServiceMockedConstruction.close();
  }

  @Test
  void runTask_withNoTemplateDir_shouldThrowException() {
    // Given
    OpenShiftHelmPushTask openShiftHelmPushTask = new OpenShiftHelmPushTask(OpenShiftExtension.class);

    // When & Then
    assertThatIllegalStateException()
        .isThrownBy(openShiftHelmPushTask::runTask)
        .withMessageContaining("META-INF" + File.separator + "jkube" + File.separator + "openshift")
        .withCauseInstanceOf(NoSuchFileException.class);
  }

  @Test
  void runTask_withTemplateDir_shouldCallHelmService() throws IOException, BadUploadException {
    // Given
    taskEnvironment.withOpenShiftTemplate();
    OpenShiftHelmPushTask openShiftHelmPushTask = new OpenShiftHelmPushTask(OpenShiftExtension.class);

    // When
    openShiftHelmPushTask.runTask();

    // Then
    verify((helmServiceMockedConstruction.constructed().iterator().next()), times(1)).uploadHelmChart(any());
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
    final OpenShiftHelmPushTask task = new OpenShiftHelmPushTask(OpenShiftExtension.class);
    when(task.getName()).thenReturn("ocHelmPush");

    // When
    task.runTask();

    // Then
    verify(taskEnvironment.logger, times(1)).lifecycle(contains("oc: `ocHelmPush` task is skipped."));
  }
}
