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

import static org.eclipse.jkube.kit.common.util.EnvUtil.isWindows;
import static org.eclipse.jkube.kit.common.util.FileUtil.getRelativeFilePath;
import static org.eclipse.jkube.kit.common.util.FileUtil.getRelativePath;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

public class FileUtilTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  /**
   * Taken from
   * https://github.com/sonatype/plexus-utils/blob/5ba6cfcca911200b5b9d2b313bb939e6d7cbbac6/src/test/java/org/codehaus/plexus/util/PathToolTest.java#L90
   */
  @Test
  public void testGetRelativeFilePathWindows() {
    assumeTrue(isWindows());
    assertEquals("", getRelativeFilePath(null, null));
    assertEquals("", getRelativeFilePath(null, "\\usr\\local\\java\\bin"));
    assertEquals("", getRelativeFilePath("\\usr\\local", null));
    assertEquals("java\\bin", getRelativeFilePath("\\usr\\local", "\\usr\\local\\java\\bin"));
    assertEquals("java\\bin\\", getRelativeFilePath("\\usr\\local", "\\usr\\local\\java\\bin\\"));
    assertEquals("..\\..\\", getRelativeFilePath("\\usr\\local\\java\\bin", "\\usr\\local\\"));
    assertEquals("java\\bin\\java.sh",
      getRelativeFilePath("\\usr\\local\\", "\\usr\\local\\java\\bin\\java.sh"));
    assertEquals("..\\..\\..\\",
      getRelativeFilePath("\\usr\\local\\java\\bin\\java.sh", "\\usr\\local\\"));
    assertEquals("..\\..\\bin", getRelativeFilePath("\\usr\\local\\", "\\bin"));
    assertEquals("..\\usr\\local", getRelativeFilePath("\\bin", "\\usr\\local"));
    assertEquals("", getRelativeFilePath("\\bin", "\\bin"));
  }

    /**
     * Taken from
     * https://github.com/sonatype/plexus-utils/blob/5ba6cfcca911200b5b9d2b313bb939e6d7cbbac6/src/test/java/org/codehaus/plexus/util/PathToolTest.java#L90
     */
    @Test
    public void testGetRelativeFilePathUnix() {
      assumeFalse(isWindows());
      assertEquals("", getRelativeFilePath(null, null));
      assertEquals("", getRelativeFilePath(null, "/usr/local/java/bin"));
      assertEquals("", getRelativeFilePath("/usr/local", null));
      assertEquals("java/bin", getRelativeFilePath("/usr/local", "/usr/local/java/bin"));
      assertEquals("java/bin/", getRelativeFilePath("/usr/local", "/usr/local/java/bin/"));
      assertEquals("../../", getRelativeFilePath("/usr/local/java/bin", "/usr/local/"));
      assertEquals("java/bin/java.sh",
        getRelativeFilePath("/usr/local/", "/usr/local/java/bin/java.sh"));
      assertEquals("../../../", getRelativeFilePath("/usr/local/java/bin/java.sh", "/usr/local/"));
      assertEquals("../../bin", getRelativeFilePath("/usr/local/", "/bin"));
      assertEquals("../usr/local", getRelativeFilePath("/bin", "/usr/local"));
      assertEquals("", getRelativeFilePath("/bin", "/bin"));
  }

  @Test
  public void testCreateDirectory() throws IOException {
    File newDirectory = new File(folder.getRoot(), "firstdirectory");
    FileUtil.createDirectory(newDirectory);
    assertTrue(newDirectory.exists());
  }

  // https://github.com/eclipse/jkube/issues/895
  @Test
  public void createDirectory_withTrailingSlash_shouldNotFail() throws IOException {
    final File toCreate = new File(folder.getRoot().toPath().resolve("first").resolve("second").toFile(),
        File.separator);
    FileUtil.createDirectory(toCreate);
    assertTrue(toCreate.exists());
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
    File relativeFile = getRelativePath(folder.getRoot(), new File(folder.getRoot(), "foo"));
    assertEquals("foo", relativeFile.getPath());
    relativeFile = getRelativePath(folder.getRoot(),
        new File(folder.getRoot().getAbsolutePath() + File.separator + "foo" + File.separator + "fileInFoo1"));
    assertEquals("foo" + File.separator + "fileInFoo1", relativeFile.getPath());
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