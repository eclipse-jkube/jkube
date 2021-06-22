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
package org.eclipse.jkube.kit.common.util;

import mockit.Mocked;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MavenFileFilterHelperTest {
    @Mocked
    private MavenFileFilter mavenFileFilter;

    @Mocked
    private MavenProject project;

    @Mocked
    private MavenSession session;

    @Test
    public void testMavenFilterFilesWithEmptyResourceFileList() throws IOException {
        // Given
        File[] resourceFiles = new File[0];
        File outDir = new File("target");

        // When
        File[] result = MavenFileFilterHelper.mavenFilterFiles(mavenFileFilter, project, session, resourceFiles, outDir);

        // Then
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testMavenFilterFilesWithNullResourceFileList() throws IOException {
        // Given
        File[] resourceFiles = null;
        File outDir = new File("target");

        // When
        File[] result = MavenFileFilterHelper.mavenFilterFiles(mavenFileFilter, project, session, resourceFiles, outDir);

        // Then
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testMavenFilterFiles() throws IOException {
        // Given
        File[] resourceFiles = new File[] { new File("f1"), new File("f2")};
        File outDir = new File("target");

        // When
        File[] result = MavenFileFilterHelper.mavenFilterFiles(mavenFileFilter, project, session, resourceFiles, outDir);

        // Then
        assertNotNull(result);
        assertArrayEquals(new File[]{new File("target/f1"), new File("target/f2")}, result);
    }
}
