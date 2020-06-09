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
package org.eclipse.jkube.generator.javaexec;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class FatJarDetectorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void scanDirectoryDoesntExist() {
    final File nonExistentDirectory = new File(temporaryFolder.getRoot(), "I-dont-exist");
    assertThat(nonExistentDirectory.exists(), is(false));
    // When
    FatJarDetector.Result result = new FatJarDetector(nonExistentDirectory).scan();
    // Then
    assertThat(result, nullValue());
  }

  @Test
  public void scanJarExists() throws Exception {
    // Given
    final URL testDirUrl = getClass().getResource("/fatjar-simple");
    // When
    FatJarDetector.Result result = new FatJarDetector(Paths.get(testDirUrl.toURI()).toFile()).scan();
    // Then
    assertNotNull(result);
    assertThat(result.getArchiveFile().exists(), is(true));
    assertThat(result.getArchiveFile().getName(), is("test.jar"));
    assertThat(result.getArchiveFile().getParentFile().getName(), is("fatjar-simple"));
    assertThat(result.getMainClass(), is("org.springframework.boot.loader.JarLauncher"));
    assertThat(result.getManifestEntry("Archiver-Version"), is("Plexus Archiver"));
  }
}
