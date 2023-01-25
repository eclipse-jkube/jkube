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

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class FatJarDetectorTest {

  @Test
  void scanDirectoryDoesntExist(@TempDir File temporaryFolder) {
    final File nonExistentDirectory = new File(temporaryFolder, "I-dont-exist");
    // When
    FatJarDetector.Result result = new FatJarDetector(nonExistentDirectory).scan();
    // Then
    assertThat(nonExistentDirectory).doesNotExist();
    assertThat(result).isNull();
  }

  @Test
  void scanJarExists() throws Exception {
    // Given
    final URL testDirUrl = getClass().getResource("/fatjar-simple");
    // When
    FatJarDetector.Result result = new FatJarDetector(Paths.get(testDirUrl.toURI()).toFile()).scan();
    // Then
    assertThat(result)
        .isNotNull()
        .hasFieldOrPropertyWithValue("mainClass", "org.springframework.boot.loader.JarLauncher")
        .extracting(FatJarDetector.Result::getArchiveFile)
        .asInstanceOf(InstanceOfAssertFactories.FILE)
        .exists()
        .hasName("test.jar")
        .hasParent(testDirUrl.getFile());
    assertThat(result.getManifestEntry("Archiver-Version")).isEqualTo("Plexus Archiver");
  }
}
