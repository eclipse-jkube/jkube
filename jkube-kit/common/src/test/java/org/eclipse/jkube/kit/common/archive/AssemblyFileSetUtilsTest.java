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
package org.eclipse.jkube.kit.common.archive;

import java.io.File;
import java.util.List;

import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.AssemblyFileSet;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.eclipse.jkube.kit.common.archive.AssemblyFileSetUtils.calculateFilePermissions;
import static org.eclipse.jkube.kit.common.archive.AssemblyFileSetUtils.isSelfPath;
import static org.assertj.core.api.Assertions.*;
public class AssemblyFileSetUtilsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void isSelfPathNullShouldBeTrue() {
    // When
    boolean result = isSelfPath(null);
    // Then
    assertThat(result).isEqualTo(true);
  }

  @Test
  public void isSelfPathEmptyShouldBeTrue() {
    // When
    boolean result = isSelfPath("   ");
    // Then
    assertThat(result).isEqualTo(true);
  }

  @Test
  public void isSelfPathDotShouldBeTrue() {
    // When
    boolean result = isSelfPath(".");
    // Then
    assertThat(result).isEqualTo(true);
  }

  @Test
  public void calculateFilePermissionsFileWithNoFileMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().build();
    final File sourceFile = temp.newFile("source-file.txt");
    final File aFile = temp.newFile("just-a-file.txt");
    // When
    final List<AssemblyFileEntry> result = calculateFilePermissions(sourceFile, aFile, afs);
    // Then
    assertThat(result).hasSize(1);
    assertThat(result).contains(new AssemblyFileEntry(sourceFile, aFile, "0644"));
  }

  @Test
  public void calculateFilePermissionsFileWithFileMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().fileMode("0777").build();
    final File sourceFile = temp.newFile("source-file.txt");
    final File aFile = temp.newFile("just-a-file.txt");
    // When
    final List<AssemblyFileEntry> result = calculateFilePermissions(sourceFile, aFile, afs);
    // Then
    assertThat(result).hasSize(1);
    assertThat(result).contains(new AssemblyFileEntry(sourceFile, aFile, "0777"));
  }

  @Test
  public void calculateFilePermissionsDirectoryWithNoDirectoryMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().build();
    final File sourceDirectory = temp.newFile("source-directory");
    final File aDirectory = temp.newFolder("just-a-directory");
    // When
    final List<AssemblyFileEntry> result = calculateFilePermissions(sourceDirectory, aDirectory, afs);
    // Then
    assertThat(result).hasSize(1);
    assertThat(result).contains(new AssemblyFileEntry(sourceDirectory, aDirectory, "040755"));
  }

  @Test
  public void calculateFilePermissionsDirectoryWithDirectoryMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().directoryMode("040777").build();
    final File sourceDirectory = temp.newFolder("source-directory");
    final File sourceSubdirectory = new File(sourceDirectory, "subdirectory");
    FileUtils.forceMkdir(sourceSubdirectory);
    final File sourceFile = new File(sourceDirectory, "file.txt");
    assertThat(sourceFile.createNewFile()).isEqualTo(true);
    final File aDirectory = temp.newFolder("just-a-directory");
    final File aSubdirectory = new File(aDirectory, "subdirectory");
    FileUtils.forceMkdir(aSubdirectory);
    final File aFile = new File(aDirectory, "file.txt");
    assertThat(aFile.createNewFile()).isEqualTo(true);
    // When
    final List<AssemblyFileEntry> result = calculateFilePermissions(sourceDirectory, aDirectory, afs);
    // Then
    assertThat(result).hasSize(3);
    assertThat(result).containsExactlyInAnyOrder(
        new AssemblyFileEntry(sourceDirectory, aDirectory, "040777"),
        new AssemblyFileEntry(sourceSubdirectory, aSubdirectory, "040777"),
        new AssemblyFileEntry(sourceFile, aFile, "0644")
    );
  }

  @Test
  public void calculateFilePermissionsDirectoryAndNestedDirectoryWithDirectoryAndFileMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().directoryMode("040775").fileMode("0755").build();
    final File sourceDirectory = temp.newFolder("source-directory");
    final File sourceSubdirectory = new File(sourceDirectory, "subdirectory");
    FileUtils.forceMkdir(sourceSubdirectory);
    final File aDirectory = temp.newFolder("just-a-directory");
    final File aSubdirectory = new File(aDirectory, "subdirectory");
    FileUtils.forceMkdir(aSubdirectory);
    // When
    final List<AssemblyFileEntry> result = calculateFilePermissions(sourceDirectory, aDirectory, afs);
    // Then
    assertThat(result).hasSize(2);
    assertThat(result).containsExactlyInAnyOrder(
        new AssemblyFileEntry(sourceDirectory, aDirectory, "040775"),
        new AssemblyFileEntry(sourceSubdirectory, aSubdirectory, "040775")
    );
  }

  @Test
  public void calculateFilePermissionsDirectoryAndNestedDirectoryAndFileWithDirectoryAndFileMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().directoryMode("040755").fileMode("0755").build();
    final File sourceDirectory = temp.newFolder("source-directory");
    final File sourceSubdirectory = new File(sourceDirectory, "subdirectory");
    FileUtils.forceMkdir(sourceSubdirectory);
    final File sourceFile = new File(sourceDirectory, "file.txt");
    assertThat(sourceFile.createNewFile()).isEqualTo(true);
    final File aDirectory = temp.newFolder("just-a-directory");
    final File aSubdirectory = new File(aDirectory, "subdirectory");
    FileUtils.forceMkdir(aSubdirectory);
    final File aFile = new File(aDirectory, "file.txt");
    assertThat(aFile.createNewFile()).isEqualTo(true);
    // When
    final List<AssemblyFileEntry> result = calculateFilePermissions(sourceDirectory, aDirectory, afs);
    // Then
    assertThat(result).hasSize(3);
    assertThat(result).containsExactlyInAnyOrder(
        new AssemblyFileEntry(sourceDirectory, aDirectory, "040755"),
        new AssemblyFileEntry(sourceSubdirectory, aSubdirectory, "040755"),
        new AssemblyFileEntry(sourceFile, aFile, "0755")
    );
  }
}
