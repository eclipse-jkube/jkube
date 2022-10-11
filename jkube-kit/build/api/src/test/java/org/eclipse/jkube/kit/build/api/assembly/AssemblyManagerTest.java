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
package org.eclipse.jkube.kit.build.api.assembly;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssemblyManagerTest {

  @TempDir
  Path temporaryFolder;
  private KitLogger logger;
  private JKubeConfiguration configuration;
  private JavaProject project;
  private AssemblyManager assemblyManager;
  private File targetDirectory;

  @BeforeEach
  void setUp() throws Exception {
    assemblyManager = AssemblyManager.getInstance();
    targetDirectory = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    logger = spy(new KitLogger.SilentLogger());
    configuration = mock(JKubeConfiguration.class);
    project = mock(JavaProject.class);
  }

  @Test
  void getInstance_shouldBeSingleton() {
    // When
    final AssemblyManager other = AssemblyManager.getInstance();
    // Then
    assertThat(assemblyManager).isSameAs(other);
  }

  @Test
  void assemblyFiles() throws Exception {
    // Given
    final File buildDirs = Files.createDirectory(temporaryFolder.resolve("buildDirs")).toFile();
      when(configuration.getProject()).thenReturn(project);
      when(project.getBaseDirectory()).thenReturn(buildDirs);
      when(project.getBuildDirectory()).thenReturn(targetDirectory);
    ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .name("testImage").build(createBuildConfig())
        .build();
    // When
    AssemblyFiles assemblyFiles = assemblyManager.getAssemblyFiles(imageConfiguration, configuration);
    // Then
    assertThat(assemblyFiles)
        .isNotNull()
        .hasFieldOrPropertyWithValue("assemblyDirectory",
            buildDirs.toPath().resolve("testImage").resolve("build").toFile())
        .extracting(AssemblyFiles::getUpdatedEntriesAndRefresh)
        .asList().isEmpty();
  }

  @Test
  void copyMultipleValidVerifyGivenDockerfile() throws IOException {
    BuildConfiguration buildConfig = createBuildConfig().toBuilder()
        .assembly(AssemblyConfiguration.builder().name("other-layer").build())
        .build();

    AssemblyManager.verifyAssemblyReferencedInDockerfile(
        new File(getClass().getResource("/docker/Dockerfile_assembly_verify_copy_multiple_valid.test").getPath()),
        buildConfig, new Properties(),
        logger);
    verify(logger,times(0)).warn(anyString(), any());
  }

  @Test
  void copyMultipleInvalidVerifyGivenDockerfile() throws IOException {
    BuildConfiguration buildConfig = createBuildConfig().toBuilder()
        .assembly(AssemblyConfiguration.builder().name("other-layer").build())
        .build();

    AssemblyManager.verifyAssemblyReferencedInDockerfile(
        new File(getClass().getResource("/docker/Dockerfile_assembly_verify_copy_valid.test").getPath()),
        buildConfig, new Properties(),
        logger);
    verify(logger,times(1)).warn(anyString(), any());
  }

  private BuildConfiguration createBuildConfig() {
    return BuildConfiguration.builder()
        .assembly(AssemblyConfiguration.builder()
            .name("maven")
            .targetDir("/maven")
            .build())
        .build();
  }

  @Test
  void ensureThatArtifactFileIsSet_withProjectArtifactSet() throws IOException {
    // Given
    JavaProject project = JavaProject.builder()
        .artifact(Files.createFile(temporaryFolder.resolve("temp-project-0.0.1.jar")).toFile())
        .build();
    // When
    final File artifactFile = assemblyManager.ensureThatArtifactFileIsSet(project);
    // Then
    assertThat(artifactFile).isFile().exists().hasName("temp-project-0.0.1.jar");
  }

  @Test
  void ensureThatArtifactFileIsSet_withNullProjectArtifact() throws IOException {
    // Given
    Files.createFile(targetDirectory.toPath().resolve("foo-project-0.0.1.jar"));
    JavaProject project = JavaProject.builder()
        .buildDirectory(targetDirectory)
        .packaging("jar")
        .buildFinalName("foo-project-0.0.1")
        .build();
    // When
    final File artifactFile = assemblyManager.ensureThatArtifactFileIsSet(project);
    // Then
    assertThat(artifactFile).isFile().exists().hasName("foo-project-0.0.1.jar");
  }

  @Test
  void ensureThatArtifactFileIsSet_withEverythingNull() throws IOException {
    // Given
    JavaProject project = JavaProject.builder().build();
    // When
    final File artifactFile = assemblyManager.ensureThatArtifactFileIsSet(project);
    // Then
    assertThat(artifactFile).isNull();
  }

}

