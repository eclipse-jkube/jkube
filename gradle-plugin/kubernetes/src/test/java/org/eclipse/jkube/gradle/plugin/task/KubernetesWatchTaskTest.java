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

import io.fabric8.kubernetes.client.KubernetesClient;
import org.assertj.core.api.AssertionsForClassTypes;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.service.kubernetes.DockerBuildService;
import org.eclipse.jkube.watcher.api.WatcherManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class KubernetesWatchTaskTest {

  @Rule
  public TaskEnvironment taskEnvironment = new TaskEnvironment();

  private MockedConstruction<DockerAccessFactory> dockerAccessFactoryMockedConstruction;
  private MockedConstruction<DockerBuildService> dockerBuildServiceMockedConstruction;
  private MockedConstruction<ClusterAccess> clusterAccessMockedConstruction;
  private MockedStatic<WatcherManager> watcherManagerMockedStatic;
  private TestKubernetesExtension extension;

  @Before
  public void setUp() throws IOException {
    // Mock required for environments with no DOCKER available (don't remove)
    dockerAccessFactoryMockedConstruction = mockConstruction(DockerAccessFactory.class,
        (mock, ctx) -> when(mock.createDockerAccess(any())).thenReturn(mock(DockerAccess.class)));
    dockerBuildServiceMockedConstruction = mockConstruction(DockerBuildService.class, (mock, ctx) -> {
      when(mock.isApplicable()).thenReturn(true);
    });
    clusterAccessMockedConstruction = mockConstruction(ClusterAccess.class, (mock, ctx) -> {
      final KubernetesClient kubernetesClient = mock(KubernetesClient.class);
      when(kubernetesClient.getMasterUrl()).thenReturn(new URL("http://kubernetes-cluster"));
      when(mock.createDefaultClient()).thenReturn(kubernetesClient);
    });
    watcherManagerMockedStatic = mockStatic(WatcherManager.class);
    extension = new TestKubernetesExtension();
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    extension.isFailOnNoKubernetesJson = false;
  }

  @After
  public void tearDown() {
    watcherManagerMockedStatic.close();
    clusterAccessMockedConstruction.close();
    dockerBuildServiceMockedConstruction.close();
    dockerAccessFactoryMockedConstruction.close();
  }

  @Test
  public void runTask_withNoManifest_shouldThrowException() {
    // Given
    extension.isFailOnNoKubernetesJson = true;
    final KubernetesWatchTask watchTask = new KubernetesWatchTask(KubernetesExtension.class);
    // When
    final IllegalStateException result = assertThrows(IllegalStateException.class, watchTask::runTask);
    // Then
    AssertionsForClassTypes.assertThat(watchTask.jKubeServiceHub.getBuildService()).isNotNull()
        .isInstanceOf(DockerBuildService.class);
    assertThat(result)
        .hasMessageContaining("An error has occurred while while trying to watch the resources");
  }

  @Test
  public void runTask_withManifest_shouldWatchEntities() throws Exception {
    // Given
    taskEnvironment.withKubernetesManifest();
    final KubernetesWatchTask watchTask = new KubernetesWatchTask(KubernetesExtension.class);
    // When
    watchTask.runTask();
    // Then
    AssertionsForClassTypes.assertThat(watchTask.jKubeServiceHub.getBuildService()).isNotNull()
        .isInstanceOf(DockerBuildService.class);
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), any(), any()), times(1));
  }
}
