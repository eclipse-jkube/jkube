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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
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
        List<File> fileList = FileUtil.listFilesAndDirsRecursivelyInDirectory(folder.getRoot());
        assertNotNull(fileList);
        assertEquals(6, fileList.size());
    }

    @Test
    public void testCleanDirectory() throws IOException {
        prepareDirectory();
        List<File> fileList = FileUtil.listFilesAndDirsRecursivelyInDirectory(folder.getRoot());
        assertEquals(6, fileList.size());
        FileUtil.cleanDirectory(folder.getRoot());
        assertFalse(folder.getRoot().exists());
    }

    @Test
    public void trimWildCardCharactersFromPath() {
        assertEquals("lib", FileUtil.trimWildcardCharactersFromPath("lib/**"));
    }

    @Test
    public void testCopyDirectoryIfNotExists() throws IOException {
        // Given
        prepareDirectory();
        File copyTarget = new File(folder.getRoot(), "copyTarget");
        // When
        FileUtil.copyDirectoryIfNotExists(new File(folder.getRoot(), "foo"), copyTarget);
        // Then
        assertThat(copyTarget.exists(), is(true));
        assertThat(copyTarget.list(), arrayWithSize(2));
        assertThat(new File(copyTarget, "fileInFoo1").exists(), is(true));
        assertThat(new File(copyTarget, "fileInFoo2").exists(), is(true));
    }

    @Test
    public void testCopyDirectoryIfNotExistsWithExistingDirectory() throws IOException {
        // Given
        prepareDirectory();
        File copyTarget = new File(folder.getRoot(), "copyTarget");
        assertThat(copyTarget.mkdirs(), is(true));
        assertThat(new File(copyTarget, "emptyFile").createNewFile(), is(true));
        // When
        FileUtil.copyDirectoryIfNotExists(new File(folder.getRoot(), "foo"), copyTarget);
        // Then
        assertThat(copyTarget.exists(), is(true));
        assertThat(copyTarget.list(), arrayWithSize(1));
        assertThat(new File(copyTarget, "emptyFile").exists(), is(true));
        assertThat(new File(copyTarget, "fileInFoo1").exists(), is(false));
        assertThat(new File(copyTarget, "fileInFoo2").exists(), is(false));
    }

    @Test
    public void testGetRelativePath() throws IOException {
      prepareDirectory();
      File relativeFile = FileUtil.getRelativePath(folder.getRoot(), new File(folder.getRoot(), "foo"));
      assertEquals("foo", relativeFile.getPath());
      relativeFile = FileUtil.getRelativePath(folder.getRoot(),
          new File(folder.getRoot().getAbsolutePath() + File.separator + "foo" + File.separator + "fileInFoo1"));
      assertEquals("foo/fileInFoo1", relativeFile.getPath());
    }

    private void prepareDirectory() throws IOException {
        final File dir1 = folder.newFolder("foo");
        assertThat(new File(dir1, "fileInFoo1").createNewFile(), is(true));
        assertThat(new File(dir1, "fileInFoo2").createNewFile(), is(true));
        folder.newFile("something");
        final File dir2 = folder.newFolder("bar");
        assertThat(new File(dir2, "fileInBar2").createNewFile(), is(true));
    }

}