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
package org.eclipse.jkube.kit.common.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.eclipse.jkube.kit.common.KitLogger;

import mockit.Mocked;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MigrateServiceTest {
  @Mocked
  KitLogger logger;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testPomPluginMigrationInBuild() throws Exception {
    // Given
    File pomFile = copyToTempDirectory("/service/migrate/test-project/fabric8-pom.xml");
    // When
    new MigrateService(folder.getRoot(), logger).migrate(
        "org.eclipse.jkube", "kubernetes-maven-plugin", "1.0.0-SNAPSHOT");
    // Then
    assertExpectedDocument("/service/migrate/test-project/expected-pom.xml").accept(pomFile);
  }

  @Test
  public void testPomPluginMigrationInProfile() throws Exception {
    // Given
    File pomFile = copyToTempDirectory("/service/migrate/test-project-profile/fabric8-pom.xml");
    // When
    new MigrateService(folder.getRoot(), logger).migrate(
        "org.eclipse.jkube", "openshift-maven-plugin", "1.0.0-SNAPSHOT");
    // Then
    assertExpectedDocument("/service/migrate/test-project-profile/expected-pom.xml").accept(pomFile);
  }

  @Test
  public void testProjectResourceFragmentDirectoryRename() throws Exception {
    // Given
    copyToTempDirectory("/service/migrate/test-project/fabric8-pom.xml");
    final File fabric8 = folder.newFolder("src", "main", "fabric8");
    assertTrue(new File(fabric8, "file1").createNewFile());
    // When
    new MigrateService(folder.getRoot(), logger)
        .migrate("org.eclipse.jkube", "openshift-maven-plugin", "1.0.0-SNAPSHOT");
    // Then
    assertFalse(fabric8.exists());
    assertTrue(new File(folder.getRoot(), "src/main/jkube").exists());
    assertTrue(new File(folder.getRoot(), "src/main/jkube/file1").exists());
  }

  @Test
  public void testProjectResourceFragmentDirectoryRenameWithMerge() throws Exception {
    // Given
    copyToTempDirectory("/service/migrate/test-project/fabric8-pom.xml");
    final File fabric8 = folder.newFolder("src", "main", "fabric8");
    assertTrue(new File(fabric8, "file1").createNewFile());
    final File jkube = folder.newFolder("src", "main", "jkube");
    assertTrue(new File(jkube, "existing-file").createNewFile());
    // When
    new MigrateService(folder.getRoot(), logger)
        .migrate("org.eclipse.jkube", "openshift-maven-plugin", "1.0.0-SNAPSHOT");
    // Then
    assertFalse(fabric8.exists());
    assertTrue(new File(folder.getRoot(), "src/main/jkube").exists());
    assertTrue(new File(folder.getRoot(), "src/main/jkube/file1").exists());
    assertTrue(new File(folder.getRoot(), "src/main/jkube/existing-file").exists());
  }

  private File copyToTempDirectory(String fabric8PomResource) throws Exception {
    File projectPom = new File(MigrateServiceTest.class.getResource(fabric8PomResource).toURI());
    File pomFile = folder.newFile("pom.xml");
    FileUtils.copyFile(projectPom, pomFile);
    return pomFile;
  }

  private static Consumer<File> assertExpectedDocument(String expectedResource) throws IOException {
    try (final InputStream is = MigrateServiceTest.class.getResourceAsStream(expectedResource)) {
      final String expected = IOUtils.toString(is, StandardCharsets.UTF_8)
        .trim().replace("\r\n", "\n");
      return convertedProject -> {
        try {
          final String result = FileUtils.readFileToString(convertedProject, StandardCharsets.UTF_8)
            .trim().replace("\r\n", "\n");;
          assertEquals(expected, result);
        } catch (IOException exception){
          fail(exception.getMessage());
        }
      };
    }
  }
}
