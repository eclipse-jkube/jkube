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
package org.eclipse.jkube.watcher.standard;

import java.io.File;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jkube.kit.build.service.docker.WatchService;
import org.eclipse.jkube.kit.build.service.docker.watch.CopyFilesTask;
import org.eclipse.jkube.kit.build.service.docker.watch.ExecTask;
import org.eclipse.jkube.kit.build.service.docker.watch.WatchContext;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.watcher.api.WatcherContext;

import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Verifications;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
public class DockerImageWatcherTest {

  @Mocked
  private WatcherContext watcherContext;
  @Mocked
  private WatchService watchService;

  private DockerImageWatcher dockerImageWatcher;
  private WatchContext watchContext;

  @Before
  public void setUp() {
    dockerImageWatcher = new DockerImageWatcher(watcherContext);
    // @formatter:off
    new Expectations() {{
      watcherContext.getWatchContext(); result = new WatchContext(); minTimes = 0;
    }};
    // @formatter:on
    new MockUp<WatchService>() {
      @Mock
      void watch(WatchContext context, JKubeConfiguration buildContext, List<ImageConfiguration> images) {
        watchContext = context;
      }
    };
  }

  @Test
  public void watchShouldInitWatchContext() {
    // When
    dockerImageWatcher.watch(null, null, null, null);
    // Then
    assertThat(watchContext)
        .isNotNull()
        .extracting("imageCustomizer", "containerRestarter", "containerCommandExecutor", "containerCopyTask")
        .doesNotContainNull();
  }

  @Test
  public void watchExecuteCommandInPodTask(@Mocked PodExecutor podExecutor) throws Exception {
    // Given
    dockerImageWatcher.watch(null, null, null, null);
    final ExecTask execTask = watchContext.getContainerCommandExecutor();
    // When
    execTask.exec("the command");
    // Then
    // @formatter:off
    new Verifications() {{
      new PodExecutor(watcherContext.getJKubeServiceHub().getClusterAccess(), Duration.ofMinutes(1)); times = 1;
      podExecutor.executeCommandInPod(null, "the command"); times = 1;
    }};
    // @formatter:on
  }

  @SuppressWarnings("unchecked")
  @Test
  public void watchCopyFileToPod(@Mocked PodExecutor podExecutor) throws Exception {
    // Given
    dockerImageWatcher.watch(null, null, null, null);
    final CopyFilesTask copyFilesTask = watchContext.getContainerCopyTask();
    // When
    copyFilesTask.copy(null);
    // Then
    // @formatter:off
    new Verifications() {{
      new PodExecutor(watcherContext.getJKubeServiceHub().getClusterAccess(),
          (InputStream)any, Duration.ofMinutes(1), (Consumer<Response>)any); times = 1;
      podExecutor.executeCommandInPod(null, "sh"); times = 1;
    }};
    // @formatter:on
  }

  @Test
  public void uploadFilesConsumer() throws Exception {
    // Given
    final PipedOutputStream pipedOutputStream = new PipedOutputStream();
    final PipedInputStream pipedInputStream = new PipedInputStream(pipedOutputStream);
    // When
    DockerImageWatcher.uploadFilesConsumer(
        new File(DockerImageWatcherTest.class.getResource("/file.txt").toURI().getPath()),
        pipedOutputStream,
        watcherContext.getLogger()
    ).accept(null);
    assertThat(IOUtils.toString(pipedInputStream, StandardCharsets.UTF_8))
      .isEqualTo("base64 -d << EOF | tar --no-overwrite-dir -C / -xf - && exit 0 || exit 1\nQSBmaWxl\nEOF\n");
  }
}
