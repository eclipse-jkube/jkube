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
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.AssemblyFileSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.eclipse.jkube.kit.common.archive.AssemblyFileSetUtils.calculateFilePermissions;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;

public class AssemblyFileSetUtilsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void calculateFilePermissionsFileWithNoFileMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().build();
    final File aFile = temp.newFile("just-a-file.txt");
    // When
    final Map<File, String> result = calculateFilePermissions(aFile, afs);
    // Then
    assertThat(result.entrySet(), empty());
  }

  @Test
  public void calculateFilePermissionsFileWithFileMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().fileMode("0644").build();
    final File aFile = temp.newFile("just-a-file.txt");
    // When
    final Map<File, String> result = calculateFilePermissions(aFile, afs);
    // Then
    assertThat(result.entrySet(), hasSize(1));
    assertThat(result, hasEntry(aFile, "0644"));
  }

  @Test
  public void calculateFilePermissionsDirectoryWithNoDirectoryMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().build();
    final File aDirectory = temp.newFolder("just-a-directory");
    // When
    final Map<File, String> result = calculateFilePermissions(aDirectory, afs);
    // Then
    assertThat(result.entrySet(), hasSize(1));
    assertThat(result, hasEntry(aDirectory, "040111"));
  }

  @Test
  public void calculateFilePermissionsDirectoryWithDirectoryMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().directoryMode("040755").build();
    final File aDirectory = temp.newFolder("just-a-directory");
    final File aSubdirectory = new File(aDirectory, "subdirectory");
    FileUtils.forceMkdir(aSubdirectory);
    final File aFile = new File(aDirectory, "file.txt");
    assertThat(aFile.createNewFile(), equalTo(true));
    // When
    final Map<File, String> result = calculateFilePermissions(aDirectory, afs);
    // Then
    assertThat(result.entrySet(), hasSize(2));
    assertThat(result, hasEntry(aDirectory, "040755"));
    assertThat(result, hasEntry(aSubdirectory, "040755"));
  }

  @Test
  public void calculateFilePermissionsDirectoryWithDirectoryAndFileMode() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().directoryMode("040755").fileMode("0644").build();
    final File aDirectory = temp.newFolder("just-a-directory");
    final File aSubdirectory = new File(aDirectory, "subdirectory");
    FileUtils.forceMkdir(aSubdirectory);
    final File aFile = new File(aDirectory, "file.txt");
    assertThat(aFile.createNewFile(), equalTo(true));
    // When
    final Map<File, String> result = calculateFilePermissions(aDirectory, afs);
    // Then
    assertThat(result.entrySet(), hasSize(3));
    assertThat(result, hasEntry(aDirectory, "040755"));
    assertThat(result, hasEntry(aSubdirectory, "040755"));
    assertThat(result, hasEntry(aFile, "0644"));
  }
}