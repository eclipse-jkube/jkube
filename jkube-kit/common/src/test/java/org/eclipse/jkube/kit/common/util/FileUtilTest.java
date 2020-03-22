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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FileUtilTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testCreateDirectory() throws IOException {
        File newDirectory = new File(folder.getRoot(), "firstdirectory");
        FileUtil.createDirectory(newDirectory);
        assertTrue(newDirectory.exists());
    }

    @Test
    public void testListFilesRecursively() throws IOException {
        prepareDirectory();
        List<File> fileList = FileUtil.listFilesRecursivelyInDirectory(folder.getRoot());
        assertNotNull(fileList);
        assertEquals(3, fileList.size());
    }

    @Test
    public void testCleanDirectory() throws IOException {
        prepareDirectory();
        List<File> fileList = FileUtil.listFilesRecursivelyInDirectory(folder.getRoot());
        assertEquals(3, fileList.size());
        FileUtil.cleanDirectory(folder.getRoot());
        assertFalse(folder.getRoot().exists());
    }

    @Test
    public void trimWildCardCharactersFromPath() {
        assertEquals("lib", FileUtil.trimWildcardCharactersFromPath("lib/**"));
    }

    @Test
    public void testCopyDir() throws IOException {
        prepareDirectory();
        File copyTarget = new File(folder.getRoot(), "copyTarget");
        FileUtil.copyDirectory(new File(folder.getRoot(), "foo"), copyTarget);

        assertTrue(copyTarget.exists());
        assertEquals(1, FileUtil.listFilesRecursivelyInDirectory(copyTarget).size());
        assertTrue(new File(copyTarget, "fileInfoo1").exists());
    }

    @Test
    public void testGetRelativePath() throws IOException {
      prepareDirectory();
      File relativeFile = FileUtil.getRelativePath(folder.getRoot(), new File(folder.getRoot(), "foo"));
      assertEquals("foo", relativeFile.getPath());
      relativeFile = FileUtil.getRelativePath(folder.getRoot(), new File(folder.getRoot().getAbsolutePath() + File.separator + "foo" + File.separator + "fileInfoo1"));
      assertEquals("foo/fileInfoo1", relativeFile.getPath());
    }

    private void prepareDirectory() throws IOException {
        File dir1 = folder.newFolder("foo");
        File file1 = new File(dir1, "fileInfoo1");
        folder.newFile("something");
        File dir2 = folder.newFolder("bar");
        File file2 = new File(dir2, "fileInfoo2");
        file1.createNewFile();
        file2.createNewFile();
    }

}