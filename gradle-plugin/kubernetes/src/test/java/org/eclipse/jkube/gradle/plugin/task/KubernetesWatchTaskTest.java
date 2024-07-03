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

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.assertj.core.api.AssertionsForClassTypes;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.service.kubernetes.DockerBuildService;
import org.eclipse.jkube.watcher.api.WatcherManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@EnableKubernetesMockClient(crud = true)
class KubernetesWatchTaskTest {

  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private MockedConstruction<DockerAccessFactory> dockerAccessFactoryMockedConstruction;
  private MockedConstruction<DockerBuildService> dockerBuildServiceMockedConstruction;
  private MockedStatic<WatcherManager> watcherManagerMockedStatic;
  private MockedStatic<KubernetesHelper> kubernetesHelperMockedStatic;
  private TestKubernetesExtension extension;
  private KubernetesClient kubernetesClient;

  @BeforeEach
  void setUp() {
    // Mock required for environments with no DOCKER available (don't remove)
    dockerAccessFactoryMockedConstruction = mockConstruction(DockerAccessFactory.class,
        (mock, ctx) -> when(mock.createDockerAccess(any())).thenReturn(mock(DockerAccess.class)));
    dockerBuildServiceMockedConstruction = mockConstruction(DockerBuildService.class, (mock, ctx) -> {
      when(mock.isApplicable()).thenReturn(true);
    });
    watcherManagerMockedStatic = mockStatic(WatcherManager.class);
    kubernetesHelperMockedStatic = mockStatic(KubernetesHelper.class);
    extension = new TestKubernetesExtension();
    extension.access = ClusterConfiguration.from(kubernetesClient.getConfiguration()).build();
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    kubernetesHelperMockedStatic.when(KubernetesHelper::getDefaultNamespace).thenReturn(null);
    extension.isFailOnNoKubernetesJson = false;
  }

  @AfterEach
  void tearDown() {
    watcherManagerMockedStatic.close();
    kubernetesHelperMockedStatic.close();
    dockerBuildServiceMockedConstruction.close();
    dockerAccessFactoryMockedConstruction.close();
  }

  @Test
  void runTask_withNoManifest_shouldThrowException() {
    // Given
    extension.isFailOnNoKubernetesJson = true;
    final KubernetesWatchTask watchTask = new KubernetesWatchTask(KubernetesExtension.class);
    // When & Then
    assertThatIllegalStateException()
        .isThrownBy(watchTask::runTask)
        .withMessageContaining("An error has occurred while while trying to watch the resources");
    assertThat(watchTask.jKubeServiceHub.getBuildService()).isNotNull()
        .isInstanceOf(DockerBuildService.class);
  }

  @Test
  void runTask_withManifest_shouldWatchEntities() throws Exception {
    // Given
    taskEnvironment.withKubernetesManifest();
    final KubernetesWatchTask watchTask = new KubernetesWatchTask(KubernetesExtension.class);
    // When
    watchTask.runTask();
    // Then
    AssertionsForClassTypes.assertThat(watchTask.jKubeServiceHub.getBuildService()).isNotNull()
        .isInstanceOf(DockerBuildService.class);
    watcherManagerMockedStatic.verify(() -> WatcherManager.watch(any(), eq(kubernetesClient.getNamespace()), any(), any()), times(1));
  }
}
