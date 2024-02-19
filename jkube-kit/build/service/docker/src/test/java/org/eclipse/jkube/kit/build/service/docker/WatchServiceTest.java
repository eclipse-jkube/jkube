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
package org.eclipse.jkube.kit.build.service.docker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.build.api.assembly.AssemblyManager;
import org.eclipse.jkube.kit.build.service.docker.watch.WatchContext;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.WatchImageConfiguration;
import org.eclipse.jkube.kit.config.image.WatchMode;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class WatchServiceTest {

  private File watchedFile;
  private ScheduledExecutorService executorService;
  private ImageConfiguration imageConfiguration;
  private BuildService buildService;
  private WatchService watchService;
  private WatchContext watchContext;

  @BeforeEach
  void setUp(@TempDir Path tempDir) throws IOException {
    watchedFile = Files.createFile(tempDir.resolve("file.txt")).toFile();
    executorService = Executors.newScheduledThreadPool(2);
    executorService.scheduleAtFixedRate(this::changeFile, 0, 10, TimeUnit.MILLISECONDS);
    final KitLogger logger = new KitLogger.SilentLogger();
    buildService = mock(BuildService.class);
    watchService = new WatchService(
      new ArchiveService(AssemblyManager.getInstance(), logger),
      buildService,
      logger);
    imageConfiguration = ImageConfiguration.builder()
      .name("test-app")
      .build(BuildConfiguration.builder()
        .assembly(AssemblyConfiguration.builder()
          .targetDir("deployments")
          .layer(Assembly.builder()
            .id("single")
            .file(AssemblyFile.builder()
              .outputDirectory(new File("."))
              .source(watchedFile)
              .fileMode("0755")
              .destName("target")
              .build())
            .build())
          .build())
        .build())
      .watch(WatchImageConfiguration.builder()
        .interval(100)
        .postExec("ls -lt /deployments")
        .build())
      .build();
    Path path = Files.createDirectory(tempDir.resolve("target"));
    watchContext = WatchContext.builder()
      .buildContext(JKubeConfiguration.builder()
        .project(JavaProject.builder()
          .baseDirectory(tempDir.toFile())
          .build())
        .outputDirectory("target")
        .build())
      .build();
  }

  @AfterEach
  void tearDown() {
    executorService.shutdown();
  }

  @Nested
  class Build {
    @BeforeEach
    void build() {
      watchContext = watchContext.toBuilder().watchMode(WatchMode.build).build();
    }

    @Test
    void customizesImage() {
      // Given
      final CompletableFuture<Boolean> imageCustomized = new CompletableFuture<>();
      watchContext = watchContext.toBuilder()
        // Override ImageCustomizer task to set this value to goal executed
        .imageCustomizer(imageConfiguration -> imageCustomized.complete(true))
        .build();
      // When
      executorService.submit(() -> {
        watchService.watch(watchContext, Collections.singletonList(imageConfiguration));
        return null;
      });
      // Then
      assertThat(imageCustomized)
        .succeedsWithin(Duration.ofSeconds(5))
        .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
        .isTrue();
    }

    @Test
    void buildsImage() throws IOException {
      // When
      executorService.submit(() -> {
        watchService.watch(watchContext, Collections.singletonList(imageConfiguration));
        return null;
      });
      // Then
      verify(buildService, timeout(5000))
        .buildImage(imageConfiguration, null, watchContext.getBuildContext());
    }

    @Test
    void runsPostGoal() {
      // Given
      final CompletableFuture<Boolean> postGoalTask = new CompletableFuture<>();
      watchContext = watchContext.toBuilder()
        // Override ImageCustomizer task to set this value to goal executed
        .postGoalTask(() -> postGoalTask.complete(true))
        .build();
      // When
      executorService.submit(() -> {
        watchService.watch(watchContext, Collections.singletonList(imageConfiguration));
        return null;
      });
      // Then
      assertThat(postGoalTask)
        .succeedsWithin(Duration.ofSeconds(5))
        .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
        .isTrue();
    }
  }
  @Nested
  class Both {
    @BeforeEach
    void both() {
      watchContext = watchContext.toBuilder().watchMode(WatchMode.both).build();
    }

    @Test
    void buildsImage() throws IOException {
      // When
      executorService.submit(() -> {
        watchService.watch(watchContext, Collections.singletonList(imageConfiguration));
        return null;
      });
      // Then
      verify(buildService, timeout(5000))
        .buildImage(imageConfiguration, null, watchContext.getBuildContext());
    }

    @Test
    void restartsContainer() {
      // Given
      final CompletableFuture<Boolean> containerRestarted = new CompletableFuture<>();
      watchContext = watchContext.toBuilder()
        // Override ImageCustomizer task to set this value to goal executed
        .containerRestarter(imageWatcher -> containerRestarted.complete(true))
        .build();
      // When
      executorService.submit(() -> {
        watchService.watch(watchContext, Collections.singletonList(imageConfiguration));
        return null;
      });
      // Then
      assertThat(containerRestarted)
        .succeedsWithin(Duration.ofSeconds(5))
        .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
        .isTrue();
    }
  }

  @Nested
  class Copy {
    @BeforeEach
    void copy() {
      watchContext = watchContext.toBuilder().watchMode(WatchMode.copy).build();
    }

    @Test
    void copiesFiles() {
      // Given
      final CompletableFuture<Boolean> fileCopied = new CompletableFuture<>();
      watchContext = watchContext.toBuilder()
        // Override Copy task to set this value to goal executed
        .containerCopyTask(f -> fileCopied.complete(true))
        .build();
      // When
      executorService.submit(() -> {
        watchService.watch(watchContext, Collections.singletonList(imageConfiguration));
        return null;
      });
      // Then
      assertThat(fileCopied)
        .succeedsWithin(Duration.ofSeconds(5))
        .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
        .isTrue();
    }

    @Test
    void callsPostExec() {
      // Given
      final CompletableFuture<Boolean> postExec = new CompletableFuture<>();
      watchContext = watchContext.toBuilder()
        // Override PostExec task to set this value to goal executed
        .containerCommandExecutor(imageWatcher -> {
          postExec.complete(true);
          return "done!";
        })
        .build();
      // When
      executorService.submit(() -> {
        watchService.watch(watchContext, Collections.singletonList(imageConfiguration));
        return null;
      });
      // Then
      assertThat(postExec)
        .succeedsWithin(Duration.ofSeconds(5))
        .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
        .isTrue();
    }
  }


  private void changeFile() {
    try {
      Files.write(watchedFile.toPath(), "test".getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
