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

import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.AssemblyFileSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.eclipse.jkube.kit.common.archive.AssemblyFileSetUtils.calculateFilePermissions;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class AssemblyFileSetUtilsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void calculateFilePermissionsFileWithNoFileMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().build();
    final File sourceFile = temp.newFile("source-file.txt");
    final File aFile = temp.newFile("just-a-file.txt");
    // When
    final List<AssemblyFileEntry> result = calculateFilePermissions(sourceFile, aFile, afs);
    // Then
    assertThat(result, empty());
  }

  @Test
  public void calculateFilePermissionsFileWithFileMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().fileMode("0644").build();
    final File sourceFile = temp.newFile("source-file.txt");
    final File aFile = temp.newFile("just-a-file.txt");
    // When
    final List<AssemblyFileEntry> result = calculateFilePermissions(sourceFile, aFile, afs);
    // Then
    assertThat(result, hasSize(1));
    assertThat(result, contains(new AssemblyFileEntry(sourceFile, aFile, "0644")));
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
    assertThat(result, hasSize(1));
    assertThat(result, contains(new AssemblyFileEntry(sourceDirectory, aDirectory, "040111")));
  }

  @Test
  public void calculateFilePermissionsDirectoryWithDirectoryMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().directoryMode("040755").build();
    final File sourceDirectory = temp.newFolder("source-directory");
    final File sourceSubdirectory = new File(sourceDirectory, "subdirectory");
    FileUtils.forceMkdir(sourceSubdirectory);
    final File sourceFile = new File(sourceDirectory, "file.txt");
    assertThat(sourceFile.createNewFile(), equalTo(true));
    final File aDirectory = temp.newFolder("just-a-directory");
    final File aSubdirectory = new File(aDirectory, "subdirectory");
    FileUtils.forceMkdir(aSubdirectory);
    final File aFile = new File(aDirectory, "file.txt");
    assertThat(aFile.createNewFile(), equalTo(true));
    // When
    final List<AssemblyFileEntry> result = calculateFilePermissions(sourceDirectory, aDirectory, afs);
    // Then
    assertThat(result, hasSize(2));
    assertThat(result, containsInAnyOrder(
        new AssemblyFileEntry(sourceDirectory, aDirectory, "040755"),
        new AssemblyFileEntry(sourceSubdirectory, aSubdirectory, "040755")
    ));
  }

  @Test
  public void calculateFilePermissionsDirectoryWithDirectoryAndFileMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().directoryMode("040755").fileMode("0644").build();
    final File sourceDirectory = temp.newFolder("source-directory");
    final File sourceSubdirectory = new File(sourceDirectory, "subdirectory");
    FileUtils.forceMkdir(sourceSubdirectory);
    final File sourceFile = new File(sourceDirectory, "file.txt");
    assertThat(sourceFile.createNewFile(), equalTo(true));
    final File aDirectory = temp.newFolder("just-a-directory");
    final File aSubdirectory = new File(aDirectory, "subdirectory");
    FileUtils.forceMkdir(aSubdirectory);
    final File aFile = new File(aDirectory, "file.txt");
    assertThat(aFile.createNewFile(), equalTo(true));
    // When
    final List<AssemblyFileEntry> result = calculateFilePermissions(sourceDirectory, aDirectory, afs);
    // Then
    assertThat(result, hasSize(3));
    assertThat(result, containsInAnyOrder(
        new AssemblyFileEntry(sourceDirectory, aDirectory, "040755"),
        new AssemblyFileEntry(sourceSubdirectory, aSubdirectory, "040755"),
        new AssemblyFileEntry(sourceFile, aFile, "0644")
    ));
  }
}