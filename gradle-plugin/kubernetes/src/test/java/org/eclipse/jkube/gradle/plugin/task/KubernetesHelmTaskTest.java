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
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.resource.helm.HelmService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedConstruction;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KubernetesHelmTaskTest {
  @Rule
  public TaskEnvironment taskEnvironment = new TaskEnvironment();

  private TestKubernetesExtension extension;
  private MockedConstruction<HelmService> helmServiceMockedConstruction;

  @Before
  public void setUp() throws IOException {
    extension = new TestKubernetesExtension();
    extension.isUseColor = false;
    helmServiceMockedConstruction = mockConstruction(HelmService.class);
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
  }

  @Test
  public void runTask_withNoTemplateDir_shouldThrowException() {
    // Given
    KubernetesHelmTask kubernetesHelmTask = new KubernetesHelmTask(KubernetesExtension.class);

    // When
    IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, kubernetesHelmTask::runTask);

    // Then
    assertThat(illegalStateException)
      .hasMessageContaining("META-INF/jkube/kubernetes (No such file or directory)");
  }

  @Test
  public void runTask_withTemplateDir_shouldCallHelmService() throws IOException {
    // Given
    taskEnvironment.withKubernetesTemplate();
    KubernetesHelmTask kubernetesHelmTask = new KubernetesHelmTask(KubernetesExtension.class);

    // When
    kubernetesHelmTask.runTask();

    // Then
    verify((helmServiceMockedConstruction.constructed().iterator().next()), times(1)).generateHelmCharts(any());
  }

  @After
  public void tearDown() {
    helmServiceMockedConstruction.close();
  }
}
