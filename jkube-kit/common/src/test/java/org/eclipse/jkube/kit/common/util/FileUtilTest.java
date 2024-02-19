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
package org.eclipse.jkube.kit.common.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.common.util.FileUtil.getRelativeFilePath;
import static org.eclipse.jkube.kit.common.util.FileUtil.getRelativePath;

class FileUtilTest {

  @TempDir
  File folder;

  /**
   * Taken from
   * https://github.com/sonatype/plexus-utils/blob/5ba6cfcca911200b5b9d2b313bb939e6d7cbbac6/src/test/java/org/codehaus/plexus/util/PathToolTest.java#L90
   */
  @Test
  @EnabledOnOs(OS.WINDOWS)
  void testGetRelativeFilePathWindows() {
    assertThat(getRelativeFilePath(null, null)).isEmpty();
    assertThat(getRelativeFilePath(null, "\\usr\\local\\java\\bin")).isEmpty();
    assertThat(getRelativeFilePath("\\usr\\local", null)).isEmpty();
    assertThat(getRelativeFilePath("\\usr\\local", "\\usr\\local\\java\\bin")).isEqualTo("java\\bin");
    assertThat(getRelativeFilePath("\\usr\\local", "\\usr\\local\\java\\bin\\")).isEqualTo("java\\bin\\");
    assertThat(getRelativeFilePath("\\usr\\local\\java\\bin", "\\usr\\local\\")).isEqualTo("..\\..\\");
    assertThat(getRelativeFilePath("\\usr\\local\\", "\\usr\\local\\java\\bin\\java.sh")).isEqualTo("java\\bin\\java.sh");
    assertThat(getRelativeFilePath("\\usr\\local\\java\\bin\\java.sh", "\\usr\\local\\")).isEqualTo("..\\..\\..\\");
    assertThat(getRelativeFilePath("\\usr\\local\\", "\\bin")).isEqualTo("..\\..\\bin");
    assertThat(getRelativeFilePath("\\bin", "\\usr\\local")).isEqualTo("..\\usr\\local");
    assertThat(getRelativeFilePath("\\bin", "\\bin")).isEmpty();
  }

    /**
     * Taken from
     * https://github.com/sonatype/plexus-utils/blob/5ba6cfcca911200b5b9d2b313bb939e6d7cbbac6/src/test/java/org/codehaus/plexus/util/PathToolTest.java#L90
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testGetRelativeFilePathUnix() {
      assertThat(getRelativeFilePath(null, null)).isEmpty();
      assertThat(getRelativeFilePath(null, "/usr/local/java/bin")).isEmpty();
      assertThat(getRelativeFilePath("/usr/local", null)).isEmpty();
      assertThat(getRelativeFilePath("/usr/local", "/usr/local/java/bin")).isEqualTo("java/bin");
      assertThat(getRelativeFilePath("/usr/local", "/usr/local/java/bin/")).isEqualTo("java/bin/");
      assertThat(getRelativeFilePath("/usr/local/java/bin", "/usr/local/")).isEqualTo("../../");
      assertThat(getRelativeFilePath("/usr/local/", "/usr/local/java/bin/java.sh")).isEqualTo("java/bin/java.sh");
      assertThat(getRelativeFilePath("/usr/local/java/bin/java.sh", "/usr/local/")).isEqualTo("../../../");
      assertThat(getRelativeFilePath("/usr/local/", "/bin")).isEqualTo("../../bin");
      assertThat(getRelativeFilePath("/bin", "/usr/local")).isEqualTo("../usr/local");
      assertThat(getRelativeFilePath("/bin", "/bin")).isEmpty();
  }

  @Test
  void testCreateDirectory() throws IOException {
    File newDirectory = new File(folder, "firstdirectory");
    FileUtil.createDirectory(newDirectory);
    assertThat(newDirectory).exists();
  }

  // https://github.com/eclipse/jkube/issues/895
  @Test
  void createDirectory_withTrailingSlash_shouldNotFail() throws IOException {
    final File toCreate = new File(folder.toPath().resolve("first").resolve("second").toFile(),
        File.separator);
    FileUtil.createDirectory(toCreate);
    assertThat(toCreate).exists();
  }

  @Test
  void testListFilesRecursively() throws IOException {
    prepareDirectory();
    List<File> fileList = FileUtil.listFilesAndDirsRecursivelyInDirectory(folder);
    assertThat(fileList).isNotNull().hasSize(5);
  }

  @Test
  void testCleanDirectory() throws IOException {
    prepareDirectory();
    List<File> fileList = FileUtil.listFilesAndDirsRecursivelyInDirectory(folder);
    assertThat(fileList).hasSize(5);
    FileUtil.cleanDirectory(folder);
    assertThat(folder).doesNotExist();
  }

  @Test
  void trimWildCardCharactersFromPath() {
    assertThat(FileUtil.trimWildcardCharactersFromPath("lib/**")).isEqualTo("lib");
  }

  @Test
  void testCopyDirectoryIfNotExists() throws IOException {
    // Given
    prepareDirectory();
    File copyTarget = new File(folder, "copyTarget");
    // When
    FileUtil.copyDirectoryIfNotExists(new File(folder, "foo"), copyTarget);
    // Then
    assertThat(copyTarget).exists();
    assertThat(copyTarget.list()).hasSize(2);
    assertThat(new File(copyTarget, "fileInFoo1")).exists();
    assertThat(new File(copyTarget, "fileInFoo2")).exists();
  }

  @Test
  void testCopyDirectoryIfNotExistsWithExistingDirectory() throws IOException {
    // Given
    prepareDirectory();
    File copyTarget = new File(folder, "copyTarget");
    assertThat(copyTarget.mkdirs()).isTrue();
    assertThat(new File(copyTarget, "emptyFile").createNewFile()).isTrue();
    // When
    FileUtil.copyDirectoryIfNotExists(new File(folder, "foo"), copyTarget);
    // Then
    assertThat(copyTarget).exists();
    assertThat(copyTarget.list()).hasSize(1);
    assertThat(new File(copyTarget, "emptyFile")).exists();
    assertThat(new File(copyTarget, "fileInFoo1")).doesNotExist();
    assertThat(new File(copyTarget, "fileInFoo2")).doesNotExist();
  }

  @Test
  void testGetRelativePath() throws IOException {
    prepareDirectory();
    File relativeFile = getRelativePath(folder, new File(folder, "foo"));
    assertThat(relativeFile.getPath()).isEqualTo("foo");
    relativeFile = getRelativePath(folder,
        new File(folder.getAbsolutePath() + File.separator + "foo" + File.separator + "fileInFoo1"));
    assertThat(relativeFile.getPath()).isEqualTo("foo" + File.separator + "fileInFoo1");
  }

  @Test
  void copy_whenFileCopied_shouldPreserveLastModifiedTimestamp() throws IOException {
    // Given
    File sourceFile = new File(folder, "source");
    Files.write(sourceFile.toPath(), "testdata".getBytes(StandardCharsets.UTF_8));
    long originalTimestamp = new Date().getTime() - 10;
    assertThat(sourceFile.setLastModified(originalTimestamp)).isTrue();
    originalTimestamp = sourceFile.lastModified(); // Update last modified, because different platforms have different precision.
    Path targetFilePath = folder.toPath().resolve("target");

    // When
    FileUtil.copy(sourceFile.toPath(), targetFilePath);

    // Then
    assertThat(targetFilePath.toFile()).hasSameTextualContentAs(sourceFile);
    assertThat(targetFilePath.toFile().lastModified()).isEqualTo(originalTimestamp);
  }

  private void prepareDirectory() throws IOException {
    final File dir1 = Files.createDirectories(folder.toPath().resolve("foo")).toFile();
    assertThat(new File(dir1, "fileInFoo1").createNewFile()).isTrue();
    assertThat(new File(dir1, "fileInFoo2").createNewFile()).isTrue();
    new File(folder, "something");
    final File dir2 = Files.createDirectories(folder.toPath().resolve("bar")).toFile();
    assertThat(new File(dir2, "fileInBar2").createNewFile()).isTrue();
  }

}
