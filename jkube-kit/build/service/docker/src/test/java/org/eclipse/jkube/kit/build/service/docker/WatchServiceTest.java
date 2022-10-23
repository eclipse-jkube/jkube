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
package org.eclipse.jkube.kit.build.service.docker;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jkube.kit.build.service.docker.watch.WatchContext;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.WatchImageConfiguration;
import org.eclipse.jkube.kit.config.image.WatchMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WatchServiceTest {

    ArchiveService archiveService;

    BuildService buildService;

    QueryService queryService;

    RunService runService;

    KitLogger logger;

    private ImageConfiguration imageConfiguration;

    @BeforeEach
    void setUp() {
        archiveService = mock(ArchiveService.class);
        buildService = mock(BuildService.class);
        queryService = mock(QueryService.class);
        runService = mock(RunService.class);
        logger = mock(KitLogger.SilentLogger.class);
        imageConfiguration = ImageConfiguration.builder()
                .name("test-app")
                .watch(WatchImageConfiguration.builder()
                        .postExec("ls -lt /deployments")
                        .build())
                .build();
    }

    @Test
    void testRestartContainerAndCallPostGoalRestartDisabled() throws Exception {
        // Given
        AtomicReference<String> stringAtomicReference = new AtomicReference<>("oldVal");
        String mavenGoalToExecute = "org.apache.maven.plugins:maven-help-plugin:help";
        WatchContext watchContext = WatchContext.builder()
                .watchMode(WatchMode.both)
                // Override PostGoal task to set this value to goal executed
                .postGoalTask(() -> stringAtomicReference.compareAndSet( "oldVal", mavenGoalToExecute))
                .build();
        WatchService.ImageWatcher imageWatcher =  new WatchService.ImageWatcher(imageConfiguration, watchContext, "test-img", "efe1234");
        WatchService watchService = new WatchService(archiveService, buildService, queryService, runService, logger);
        // When
        watchService.restartContainerAndCallPostGoal(imageWatcher, false);
        // Then
        assertThat(stringAtomicReference).hasValue(mavenGoalToExecute);
    }

    @Test
    void testRestartContainerAndCallPostGoalRestartEnabled() throws Exception {
        // Given
        AtomicBoolean restarted = new AtomicBoolean(false);
        WatchContext watchContext = WatchContext.builder()
                .watchMode(WatchMode.both)
                .containerRestarter(i -> restarted.set(true)) // Override Restart task to set this value to true
                .build();
        WatchService.ImageWatcher imageWatcher =  new WatchService.ImageWatcher(imageConfiguration, watchContext, "test-img", "efe1234");
        WatchService watchService = new WatchService(archiveService, buildService,  queryService, runService, logger);
        // When
        watchService.restartContainerAndCallPostGoal(imageWatcher, true);
        // Then
        assertThat(restarted).isTrue();
    }

    @Test
    void testCopyFilesToContainer() throws Exception {
        // Given
        AtomicBoolean fileCopied = new AtomicBoolean(false);
        WatchContext watchContext = WatchContext.builder()
                .watchMode(WatchMode.copy)
                // Override Copy task to set this value to goal executed
                .containerCopyTask(f -> fileCopied.compareAndSet(false,true))
                .build();
        File fileToCopy = Files.createTempFile("test-changed-files", "tar").toFile();
        WatchService.ImageWatcher imageWatcher =  new WatchService.ImageWatcher(imageConfiguration, watchContext, "test-img", "efe1234");
        WatchService watchService = new WatchService(archiveService, buildService, queryService, runService, logger);

        // When
        watchService.copyFilesToContainer(fileToCopy, imageWatcher);

        // Then
        assertThat(fileCopied).isTrue();
    }

    @Test
    void testCallPostExec() throws Exception {
        // Given
        AtomicBoolean postExecCommandExecuted = new AtomicBoolean(false);
        WatchContext watchContext = WatchContext.builder()
                .watchMode(WatchMode.copy)
                // Override PostExec task to set this value to goal executed
                .containerCommandExecutor(imageWatcher -> {
                    postExecCommandExecuted.set(true);
                    return "Some Output";
                })
                .build();
        WatchService.ImageWatcher imageWatcher =  new WatchService.ImageWatcher(imageConfiguration, watchContext, "test-img", "efe1234");
        WatchService watchService = new WatchService(archiveService, buildService, queryService, runService, logger);

        // When
        watchService.callPostExec(imageWatcher);

        // Then
        assertThat(postExecCommandExecuted).isTrue();
    }
}
