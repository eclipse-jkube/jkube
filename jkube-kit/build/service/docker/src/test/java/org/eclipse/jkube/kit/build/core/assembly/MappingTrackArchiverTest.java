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
package org.eclipse.jkube.kit.build.core.assembly;

import org.eclipse.jkube.kit.common.KitLogger;
import mockit.Injectable;
import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author roland
 * @since 02/07/15
 */
public class MappingTrackArchiverTest {

    @Injectable
    private MavenSession session;

    private MappingTrackArchiver archiver;

    @Before
    public void setup() throws IllegalAccessException {
        archiver = new MappingTrackArchiver();
        archiver.init(new KitLogger.StdoutLogger(), "maven");
    }

    @Test(expected = IllegalArgumentException.class)
    public void noDirectory() throws Exception {
        archiver.setDestFile(new File("."));
        archiver.addDirectory(new File(System.getProperty("user.home")), "tmp");
        AssemblyFiles files = archiver.getAssemblyFiles(session);
    }

    @Test
    public void simple() throws Exception {
        archiver.setDestFile(new File("target/test-data/maven.tracker"));
        new File(archiver.getDestFile(), "maven").mkdirs();

        File tempFile = File.createTempFile("tracker", "txt");
        File destination = new File("target/test-data/maven/test.txt");
        org.codehaus.plexus.util.FileUtils.copyFile(tempFile, destination);

        archiver.addFile(tempFile, "test.txt");
        AssemblyFiles files = archiver.getAssemblyFiles(session);
        assertNotNull(files);
        List<AssemblyFiles.Entry> entries = files.getUpdatedEntriesAndRefresh();
        assertEquals(0, entries.size());
        Thread.sleep(1000);
        FileUtils.touch(tempFile);
        entries = files.getUpdatedEntriesAndRefresh();
        assertEquals(1, entries.size());
        AssemblyFiles.Entry entry = entries.get(0);
        assertEquals(tempFile, entry.getSrcFile());
        assertEquals(destination, entry.getDestFile());
    }
}

