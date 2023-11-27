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
package org.eclipse.jkube.watcher.standard;

import java.io.IOException;
import org.eclipse.jkube.kit.build.service.docker.DockerServiceHub;
import org.eclipse.jkube.kit.build.service.docker.WatchService;
import org.eclipse.jkube.kit.build.service.docker.watch.CopyFilesTask;
import org.eclipse.jkube.kit.build.service.docker.watch.ExecTask;
import org.eclipse.jkube.kit.build.service.docker.watch.WatchContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.watcher.api.WatcherContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DockerImageWatcherTest {
  private WatchService watchService;
  private DockerImageWatcher dockerImageWatcher;

  @BeforeEach
  public void setUp() {
    WatcherContext watcherContext = mock(WatcherContext.class, RETURNS_DEEP_STUBS);
    ClusterAccess mockedClusterAccess = mock(ClusterAccess.class);
    watchService = mock(WatchService.class);
    DockerServiceHub mockedDockerServiceHub = mock(DockerServiceHub.class,RETURNS_DEEP_STUBS);
    dockerImageWatcher = new DockerImageWatcher(watcherContext);
    when(mockedDockerServiceHub.getWatchService()).thenReturn(watchService);
    when(watcherContext.getJKubeServiceHub().getDockerServiceHub()).thenReturn(mockedDockerServiceHub);
    when(watcherContext.getWatchContext()).thenReturn(new WatchContext());
    when(watcherContext.getBuildContext().getProject()).thenReturn(JavaProject.builder()
        .groupId("org.example")
        .artifactId("test")
        .version("1.0.0-SNAPSHOT").build());
    when(watcherContext.getJKubeServiceHub().getClusterAccess()).thenReturn(mockedClusterAccess);
  }

  @Test
  void customizeImageName_whenImageConfigurationProvided_thenModifiesImageNameForWatch() {
    // Given
    ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .name("foo/example:latest")
        .build();

    // When
    dockerImageWatcher.customizeImageName(imageConfiguration);

    // Then
    assertThat(imageConfiguration.getName()).startsWith("foo/example:snapshot-");
  }

  @Test
  void watchShouldInitWatchContext() throws IOException {
    // Given
    ArgumentCaptor<WatchContext> watchContextArgumentCaptor = ArgumentCaptor.forClass(WatchContext.class);
    // When
    dockerImageWatcher.watch(null, null, null, null);
    // Then
    verify(watchService).watch(watchContextArgumentCaptor.capture(), any());
    assertThat(watchContextArgumentCaptor.getValue())
        .isNotNull()
        .extracting("imageCustomizer","containerRestarter","containerCommandExecutor","containerCopyTask")
        .doesNotContainNull();
  }

  @Test
  void watchExecuteCommandInPodTask() throws Exception {
    try (MockedConstruction<PodExecutor> podExecutorMockedConstruction = mockConstruction(PodExecutor.class)) {
      // Given
      ArgumentCaptor<WatchContext>watchContextArgumentCaptor=ArgumentCaptor.forClass(WatchContext.class);
      dockerImageWatcher.watch(null,null,null,null);
      verify(watchService).watch(watchContextArgumentCaptor.capture(), any());
      final ExecTask execTask=watchContextArgumentCaptor.getValue().getContainerCommandExecutor();
      //When
      execTask.exec("thecommand");
      // Then
      assertThat(podExecutorMockedConstruction.constructed()).hasSize(1);
      verify(podExecutorMockedConstruction.constructed().get(0)).executeCommandInPod(isNull(),eq("thecommand"));
    }
  }

  @Test
  void watchCopyFileToPod() throws Exception {
    try (MockedConstruction<PodExecutor> podExecutorMockedConstruction = mockConstruction(PodExecutor.class)) {
      // Given
      ArgumentCaptor<WatchContext> watchContextArgumentCaptor = ArgumentCaptor.forClass(WatchContext.class);
      dockerImageWatcher.watch(null,null,null,null);
      verify(watchService).watch(watchContextArgumentCaptor.capture(), any());
      final CopyFilesTask copyFilesTask = watchContextArgumentCaptor.getValue().getContainerCopyTask();
      // When
      copyFilesTask.copy(null);
      // Then
      assertThat(podExecutorMockedConstruction.constructed()).hasSize(1);
      verify(podExecutorMockedConstruction.constructed().get(0)).uploadChangedFilesToPod(isNull(), any());
    }
  }
}
