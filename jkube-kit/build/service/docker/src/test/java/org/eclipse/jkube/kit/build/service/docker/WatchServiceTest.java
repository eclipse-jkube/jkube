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
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.WatchImageConfiguration;
import org.eclipse.jkube.kit.config.image.WatchMode;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WatchServiceTest {
    @Mocked
    ArchiveService archiveService;

    @Mocked
    BuildService buildService;

    @Mocked
    DockerAccess dockerAccess;

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
                        .postGoal("org.apache.maven.plugins:maven-help-plugin:help")
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
        WatchService watchService = new WatchService(archiveService, buildService, dockerAccess, queryService, runService, logger);

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
        WatchService watchService = new WatchService(archiveService, buildService, dockerAccess, queryService, runService, logger);

        // When
        watchService.restartContainerAndCallPostGoal(imageWatcher, true);

        // Then
        assertTrue(restarted.get());
    }
}
