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

import mockit.Mocked;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.WatchImageConfiguration;
import org.eclipse.jkube.kit.config.image.WatchMode;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WatchServiceTest {
    @Mocked
    ArchiveService archiveService;

    @Mocked
    BuildService buildService;

    @Mocked
    QueryService queryService;

    @Mocked
    RunService runService;

    @Mocked
    KitLogger logger;

    ImageConfiguration imageConfiguration;

    @Before
    public void setUp() {
        imageConfiguration = ImageConfiguration.builder()
                .name("test-app")
                .watch(WatchImageConfiguration.builder()
                        .postExec("ls -lt /deployments")
                        .build())
                .build();
    }

    @Test
    public void testRestartContainerAndCallPostGoalRestartDisabled() throws Exception {
        // Given
        AtomicReference<String> stringAtomicReference = new AtomicReference<>("oldVal");
        String mavenGoalToExecute = "org.apache.maven.plugins:maven-help-plugin:help";
        WatchService.WatchContext watchContext = WatchService.WatchContext.builder()
                .watchMode(WatchMode.both)
                // Override PostGoal task to set this value to goal executed
                .postGoalTask(() -> stringAtomicReference.compareAndSet( "oldVal", mavenGoalToExecute))
                .build();
        WatchService.ImageWatcher imageWatcher =  new WatchService.ImageWatcher(imageConfiguration, watchContext, "test-img", "efe1234");
        WatchService watchService = new WatchService(archiveService, buildService, queryService, runService, logger);

        // When
        watchService.restartContainerAndCallPostGoal(imageWatcher, false);

        // Then
        assertEquals(mavenGoalToExecute, stringAtomicReference.get());
    }

    @Test
    public void testRestartContainerAndCallPostGoalRestartEnabled() throws Exception {
        // Given
        AtomicBoolean restarted = new AtomicBoolean(false);
        WatchService.WatchContext watchContext = WatchService.WatchContext.builder()
                .watchMode(WatchMode.both)
                .containerRestarter(i -> restarted.set(true)) // Override Restart task to set this value to true
                .build();
        WatchService.ImageWatcher imageWatcher =  new WatchService.ImageWatcher(imageConfiguration, watchContext, "test-img", "efe1234");
        WatchService watchService = new WatchService(archiveService, buildService,  queryService, runService, logger);

        // When
        watchService.restartContainerAndCallPostGoal(imageWatcher, true);

        // Then
        assertTrue(restarted.get());
    }

    @Test
    public void testCopyFilesToContainer() throws IOException {
        // Given
        AtomicBoolean fileCopied = new AtomicBoolean(false);
        WatchService.WatchContext watchContext = WatchService.WatchContext.builder()
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
        assertTrue(fileCopied.get());
    }

    @Test
    public void testCallPostExec() throws Exception {
        // Given
        AtomicBoolean postExecCommandExecuted = new AtomicBoolean(false);
        WatchService.WatchContext watchContext = WatchService.WatchContext.builder()
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
        String output = watchService.callPostExec(imageWatcher);

        // Then
        assertTrue(postExecCommandExecuted.get());
        assertEquals("Some Output", output);
    }
}
