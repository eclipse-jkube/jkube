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
package org.eclipse.jkube.kit.common.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Consumer;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.KitLogger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class MigrateServiceTest {
  private KitLogger logger;

  @TempDir
  File folder;

  @BeforeEach
  public void setUp() {
    logger = new KitLogger.SilentLogger();
  }

  @Test
  void testPomPluginMigrationInBuild() throws Exception {
    // Given
    File pomFile = copyToTempDirectory("/service/migrate/test-project/fabric8-pom.xml");
    // When
    new MigrateService(folder, logger).migrate(
        "org.eclipse.jkube", "kubernetes-maven-plugin", "1.0.0-SNAPSHOT");
    // Then
    assertExpectedDocument("/service/migrate/test-project/expected-pom.xml").accept(pomFile);
  }

  @Test
  void testPomPluginMigrationInProfile() throws Exception {
    // Given
    File pomFile = copyToTempDirectory("/service/migrate/test-project-profile/fabric8-pom.xml");
    // When
    new MigrateService(folder, logger).migrate(
        "org.eclipse.jkube", "openshift-maven-plugin", "1.0.0-SNAPSHOT");
    // Then
    assertExpectedDocument("/service/migrate/test-project-profile/expected-pom.xml").accept(pomFile);
  }

  @Test
  void testProjectResourceFragmentDirectoryRename() throws Exception {
    // Given
    copyToTempDirectory("/service/migrate/test-project/fabric8-pom.xml");
    final File fabric8 = Files.createDirectories(folder.toPath().resolve("src").resolve("main").resolve("fabric8")).toFile();
    assertThat(new File(fabric8, "file1").createNewFile()).isTrue();
    // When
    new MigrateService(folder, logger)
        .migrate("org.eclipse.jkube", "openshift-maven-plugin", "1.0.0-SNAPSHOT");
    // Then
    assertThat(fabric8).doesNotExist();
    assertThat(folder.toPath())
            .extracting(p -> p.resolve("src").resolve("main").resolve("jkube"))
            .asInstanceOf(InstanceOfAssertFactories.PATH)
            .exists().isDirectory()
            .extracting(p -> p.resolve("file1"))
            .asInstanceOf(InstanceOfAssertFactories.PATH)
            .exists().isRegularFile();
  }

  @Test
  void testProjectResourceFragmentDirectoryRenameWithMerge() throws Exception {
    // Given
    copyToTempDirectory("/service/migrate/test-project/fabric8-pom.xml");
    final File fabric8 = Files.createDirectories(folder.toPath().resolve("src").resolve("main").resolve("fabric8")).toFile();
    assertThat(new File(fabric8, "file1").createNewFile()).isTrue();
    final File jkube = Files.createDirectories(folder.toPath().resolve("src").resolve("main").resolve("jkube")).toFile();
    assertThat(new File(jkube, "existing-file").createNewFile()).isTrue();
    // When
    new MigrateService(folder, logger)
        .migrate("org.eclipse.jkube", "openshift-maven-plugin", "1.0.0-SNAPSHOT");
    // Then
    assertThat(fabric8).doesNotExist();
    assertThat(folder.toPath())
            .extracting(p -> p.resolve("src").resolve("main").resolve("jkube"))
            .asInstanceOf(InstanceOfAssertFactories.PATH)
            .exists().isDirectory()
            .extracting(p -> p.resolve("file1"))
            .asInstanceOf(InstanceOfAssertFactories.PATH)
            .exists().isRegularFile();
    assertThat(folder.toPath())
            .extracting(p -> p.resolve("src").resolve("main").resolve("jkube").resolve("existing-file"))
            .asInstanceOf(InstanceOfAssertFactories.PATH)
            .exists().isRegularFile();
  }

  private File copyToTempDirectory(String fabric8PomResource) throws Exception {
    File projectPom = new File(MigrateServiceTest.class.getResource(fabric8PomResource).toURI());
    File pomFile = folder.toPath().resolve("pom.xml").toFile();
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
            .trim().replace("\r\n", "\n");
          assertThat(result).isEqualTo(expected);
        } catch (IOException exception){
          fail(exception.getMessage());
        }
      };
    }
  }
}
