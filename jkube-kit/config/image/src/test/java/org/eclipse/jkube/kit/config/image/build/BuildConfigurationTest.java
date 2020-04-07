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
package org.eclipse.jkube.kit.config.image.build;

import java.io.File;

import mockit.Expectations;
import org.eclipse.jkube.kit.common.KitLogger;
import mockit.Mocked;
import org.junit.Test;

import static org.eclipse.jkube.kit.common.archive.ArchiveCompression.bzip2;
import static org.eclipse.jkube.kit.common.archive.ArchiveCompression.gzip;
import static org.eclipse.jkube.kit.common.archive.ArchiveCompression.none;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author roland
 * @since 04/04/16
 */

public class BuildConfigurationTest {

    @Mocked
    KitLogger logger;

    @Test
    public void empty() {
        BuildConfiguration config = new BuildConfiguration();
        config.validate();
        assertFalse(config.isDockerFileMode());
    }

    @Test
    public void simpleDockerfile() {
        BuildConfiguration config =
            new BuildConfiguration.Builder().
                dockerFile("src/docker/Dockerfile").build();
        config.validate();
        assertTrue(config.isDockerFileMode());
        assertEquals(config.calculateDockerFilePath(),new File("src/docker/Dockerfile"));
    }

    @Test
    public void simpleDockerfileDir() {
        BuildConfiguration config =
            new BuildConfiguration.Builder().
                                                     contextDir("src/docker/").build();
        config.validate();
        assertTrue(config.isDockerFileMode());
        assertEquals(config.calculateDockerFilePath(),new File("src/docker/Dockerfile"));
    }

    @Test
    public void DockerfileDirAndDockerfileAlsoSet() {
        BuildConfiguration config =
            new BuildConfiguration.Builder().
                                                     contextDir("/tmp/").
                dockerFile("Dockerfile").build();
        config.validate();
        assertTrue(config.isDockerFileMode());
        assertEquals(config.calculateDockerFilePath(),new File("/tmp/Dockerfile"));
    }

    @Test
    public void dockerFileAndArchive() {
        BuildConfiguration config =
            new BuildConfiguration.Builder().
                dockerArchive("this").
                dockerFile("that").build();

        try {
            config.validate();
        } catch (IllegalArgumentException expected) {
            return;
        }
        fail("Should have failed.");
    }

    @Test
    public void dockerArchive() {
        BuildConfiguration config =
                new BuildConfiguration.Builder().
                        dockerArchive("this").build();
        config.initAndValidate(logger);

        assertFalse(config.isDockerFileMode());
        assertEquals(new File("this"), config.getDockerArchive());
    }

    @Test
    public void compression() {
        BuildConfiguration config =
            new BuildConfiguration.Builder().
                compression("gzip").build();
        assertEquals(gzip, config.getCompression());

        config = new BuildConfiguration.Builder().build();
        assertEquals(none, config.getCompression());

        config =
            new BuildConfiguration.Builder().
                compression("bzip2").build();
        assertEquals(bzip2, config.getCompression());

        try {
            new BuildConfiguration.Builder().
                compression("bzip").build();
            fail();
        } catch (Exception exp) {
            assertTrue(exp.getMessage().contains("bzip"));
        }
    }


    @Test
    public void isValidWindowsFileName() {
        BuildConfiguration cfg = new BuildConfiguration();
        assertFalse(cfg.isValidWindowsFileName("/Dockerfile"));
        assertTrue(cfg.isValidWindowsFileName("Dockerfile"));
        assertFalse(cfg.isValidWindowsFileName("Dockerfile/"));
    }

    @Test
    public void testBuilder(@Mocked AssemblyConfiguration mockAssemblyConfiguration) {
        // Given
        new Expectations() {{
            mockAssemblyConfiguration.getName();
            result = "1337";
        }};
        // When
        final BuildConfiguration result = new BuildConfiguration.Builder()
            .assembly(mockAssemblyConfiguration)
            .user("super-user")
            .build();
        // Then
        assertThat(result.getUser(), equalTo("super-user"));
        assertThat(result.getAssemblyConfiguration().getName(), equalTo("1337"));
    }

}
