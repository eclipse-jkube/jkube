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
import java.util.Properties;

import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.common.JKubeConfiguration;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Verifications;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"TestMethodWithIncorrectSignature", "ResultOfMethodCallIgnored"})
public class AssemblyManagerTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private AssemblyManager assemblyManager;
  private File targetDirectory;

  @Before
  public void setUp() throws Exception {
    assemblyManager = AssemblyManager.getInstance();
    targetDirectory = temporaryFolder.newFolder("target");
  }

  @Test
  public void testGetInstanceShouldBeSingleton() {
    // When
    final AssemblyManager other = AssemblyManager.getInstance();
    // Then
    assertThat(assemblyManager).isSameAs(other);
  }

  @Test
  public void assemblyFiles(
      @Injectable final JKubeConfiguration configuration, @Injectable final JavaProject project) throws Exception {
    // Given
    final File buildDirs = temporaryFolder.newFolder("buildDirs");
    // @formatter:off
      new Expectations() {{
          configuration.getProject(); result = project;
          project.getBaseDirectory(); result = buildDirs;
          project.getBuildDirectory(); result = targetDirectory;
      }};
      // @formatter:on
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
  public void testCopyValidVerifyGivenDockerfile(@Injectable final KitLogger logger) throws IOException {
    BuildConfiguration buildConfig = createBuildConfig();

    AssemblyManager.verifyAssemblyReferencedInDockerfile(
        new File(getClass().getResource("/docker/Dockerfile_assembly_verify_copy_valid.test").getPath()),
        buildConfig, new Properties(),
        logger);

    // @formatter:off
    new Verifications() {{
      logger.warn(anyString, (Object []) any); times = 0;
    }};
    //@formatter:on
  }

  @Test
  public void testCopyMultipleValidVerifyGivenDockerfile(@Injectable final KitLogger logger) throws IOException {
    BuildConfiguration buildConfig = createBuildConfig().toBuilder()
        .assembly(AssemblyConfiguration.builder().name("other-layer").build())
        .build();

    AssemblyManager.verifyAssemblyReferencedInDockerfile(
        new File(getClass().getResource("/docker/Dockerfile_assembly_verify_copy_multiple_valid.test").getPath()),
        buildConfig, new Properties(),
        logger);

    // @formatter:off
    new Verifications() {{
      logger.warn(anyString, (Object []) any); times = 0;
    }};
    //@formatter:on
  }

  @Test
  public void testCopyMultipleInvalidVerifyGivenDockerfile(@Injectable final KitLogger logger) throws IOException {
    BuildConfiguration buildConfig = createBuildConfig().toBuilder()
        .assembly(AssemblyConfiguration.builder().name("other-layer").build())
        .build();

    AssemblyManager.verifyAssemblyReferencedInDockerfile(
        new File(getClass().getResource("/docker/Dockerfile_assembly_verify_copy_valid.test").getPath()),
        buildConfig, new Properties(),
        logger);

    // @formatter:off
    new Verifications() {{
      logger.warn(anyString, (Object []) any); times = 1;
    }};
    //@formatter:on
  }

  @Test
  public void testCopyInvalidVerifyGivenDockerfile(@Injectable final KitLogger logger) throws IOException {
    BuildConfiguration buildConfig = createBuildConfig();

    AssemblyManager.verifyAssemblyReferencedInDockerfile(
        new File(getClass().getResource("/docker/Dockerfile_assembly_verify_copy_invalid.test").getPath()),
        buildConfig, new Properties(),
        logger);

    // @formatter:off
    new Verifications() {{
      logger.warn(anyString, (Object []) any); times = 1;
    }};
    //@formatter:on
  }

  @Test
  public void testCopyChownValidVerifyGivenDockerfile(@Injectable final KitLogger logger) throws IOException {
    BuildConfiguration buildConfig = createBuildConfig();

    AssemblyManager.verifyAssemblyReferencedInDockerfile(
        new File(getClass().getResource("/docker/Dockerfile_assembly_verify_copy_chown_valid.test").getPath()),
        buildConfig,
        new Properties(),
        logger);

    // @formatter:off
    new Verifications() {{
      logger.warn(anyString, (Object []) any); times = 0;
    }};
    //@formatter:on
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
  public void testEnsureThatArtifactFileIsSetWithProjectArtifactSet() throws IOException {
    // Given
    JavaProject project = JavaProject.builder()
        .artifact(temporaryFolder.newFile("temp-project-0.0.1.jar"))
        .build();
    // When
    final File artifactFile = assemblyManager.ensureThatArtifactFileIsSet(project);
    // Then
    assertThat(artifactFile).isFile().exists().hasName("temp-project-0.0.1.jar");
  }

  @Test
  public void testEnsureThatArtifactFileIsSetWithNullProjectArtifact() throws IOException {
    // Given
    assertTrue(new File(targetDirectory, "foo-project-0.0.1.jar").createNewFile());
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
  public void testEnsureThatArtifactFileIsSetWithEverythingNull() throws IOException {
    // Given
    JavaProject project = JavaProject.builder().build();
    // When
    final File artifactFile = assemblyManager.ensureThatArtifactFileIsSet(project);
    // Then
    assertThat(artifactFile).isNull();
  }

}

