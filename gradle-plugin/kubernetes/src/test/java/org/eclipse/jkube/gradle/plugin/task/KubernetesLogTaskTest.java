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
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPluginConvention;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KubernetesLogTaskTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private MockedConstruction<DefaultTask> defaultTaskMockedConstruction;
  private Project project;
  private Logger logger;

  @Before
  public void setUp() throws IOException {
    project = mock(Project.class, Mockito.RETURNS_DEEP_STUBS);
    logger = mock(Logger.class);

    defaultTaskMockedConstruction = mockConstruction(DefaultTask.class, (mock, ctx) -> {
      when(mock.getProject()).thenReturn(project);
      when(mock.getLogger()).thenReturn(logger);
    });
    when(project.getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getBuildscript().getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getProjectDir()).thenReturn(temporaryFolder.getRoot());
    when(project.getBuildDir()).thenReturn(temporaryFolder.newFolder("build"));
    KubernetesExtension extension = new TestKubernetesExtension();
    when(project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    when(project.getConvention().getPlugin(JavaPluginConvention.class)).thenReturn(mock(JavaPluginConvention.class));
  }

  @Test
  public void runTask_withNoK8sManifests_shouldLogCantWatchPods() {
    // Given
    KubernetesLogTask kubernetesLogTask = new KubernetesLogTask(KubernetesExtension.class);

    // When
    kubernetesLogTask.runTask();

    // Then
    verify(logger).lifecycle("k8s: Running in Kubernetes mode");
    verify(logger).warn("k8s: No selector in deployment so cannot watch pods!");
  }

  @Test
  public void runTask_withManifestLoadException_shouldThrowException() {
    try (MockedStatic<KubernetesHelper> mockStatic = Mockito.mockStatic(KubernetesHelper.class)) {
      // Given
      KubernetesLogTask kubernetesLogTask = new KubernetesLogTask(KubernetesExtension.class);

      mockStatic.when(() -> KubernetesHelper.loadResources(any())).thenThrow(new IOException("Can't load manifests"));

      // When
      IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, kubernetesLogTask::runTask);

      // Then
      assertThat(illegalStateException.getMessage()).isEqualTo("Failure in getting logs");
    }
  }

  @After
  public void tearDown() {
    defaultTaskMockedConstruction.close();
  }
}
