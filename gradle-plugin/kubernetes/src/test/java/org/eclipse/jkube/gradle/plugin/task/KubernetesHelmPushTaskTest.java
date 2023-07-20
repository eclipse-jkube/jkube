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
import org.eclipse.jkube.kit.resource.helm.BadUploadException;
import org.eclipse.jkube.kit.resource.helm.HelmService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.nio.file.NoSuchFileException;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KubernetesHelmPushTaskTest {
  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private MockedConstruction<HelmService> helmServiceMockedConstruction;

  @BeforeEach
  void setUp() throws IOException {
    TestKubernetesExtension extension = new TestKubernetesExtension();
    extension.isUseColor = false;
    helmServiceMockedConstruction = mockConstruction(HelmService.class);
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
  }

  @AfterEach
  void tearDown() {
    helmServiceMockedConstruction.close();
  }

  @Test
  void runTask_withNoTemplateDir_shouldThrowException() {
    // Given
    KubernetesHelmPushTask kubernetesHelmPushTask = new KubernetesHelmPushTask(KubernetesExtension.class);

    // When & Then
    assertThatIllegalStateException()
        .isThrownBy(kubernetesHelmPushTask::runTask)
        .withMessageContaining("META-INF/jkube/kubernetes")
        .withCauseInstanceOf(NoSuchFileException.class);
  }

  @Test
  void runTask_withTemplateDir_shouldCallHelmService() throws IOException, BadUploadException {
    // Given
    taskEnvironment.withKubernetesTemplate();
    KubernetesHelmPushTask kubernetesHelmPushTask = new KubernetesHelmPushTask(KubernetesExtension.class);

    // When
    kubernetesHelmPushTask.runTask();

    // Then
    verify((helmServiceMockedConstruction.constructed().iterator().next()), times(1)).uploadHelmChart(any());
  }
}
