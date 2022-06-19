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

import mockit.Expectations;
import mockit.Injectable;
import mockit.Verifications;
import org.assertj.core.api.Assertions;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class AssemblyManagerTest {

  @SuppressWarnings({"TestMethodWithIncorrectSignature", "ResultOfMethodCallIgnored"})
  public static class Misc extends UnitTest {
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
    public void testCopyMultipleValidVerifyGivenDockerfile(@Injectable final KitLogger logger) throws IOException, URISyntaxException {
      BuildConfiguration buildConfig = createBuildConfig().toBuilder()
              .assembly(AssemblyConfiguration.builder().name("other-layer").build())
              .build();

      AssemblyManager.verifyAssemblyReferencedInDockerfile(
              resourceTestFile("/docker/Dockerfile_assembly_verify_copy_multiple_valid.test"),
              buildConfig, new Properties(),
              logger);

      // @formatter:off
      new Verifications() {{
        logger.warn(anyString, (Object []) any); times = 0;
      }};
      //@formatter:on
    }

    @Test
    public void testCopyMultipleInvalidVerifyGivenDockerfile(@Injectable final KitLogger logger) throws IOException, URISyntaxException {
      BuildConfiguration buildConfig = createBuildConfig().toBuilder()
              .assembly(AssemblyConfiguration.builder().name("other-layer").build())
              .build();

      AssemblyManager.verifyAssemblyReferencedInDockerfile(
              resourceTestFile("/docker/Dockerfile_assembly_verify_copy_valid.test"),
              buildConfig, new Properties(),
              logger);

      // @formatter:off
      new Verifications() {{
        logger.warn(anyString, (Object []) any); times = 1;
      }};
      //@formatter:on
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

  @RunWith(Parameterized.class)
  public static class CopySingleDockerFile extends UnitTest {
    @Parameter
    public String testName;

    @Parameter(1)
    public String dockerFilePath;

    @Parameter(2)
    public int expectedNumberOfInvocations;

    @Parameters(name = "{0}")
    public static Object[][] data() {
      return new Object[][] {
              { "testCopyValidVerifyGivenDockerfile", "/docker/Dockerfile_assembly_verify_copy_valid.test", 0 },
              { "testCopyInvalidVerifyGivenDockerfile", "/docker/Dockerfile_assembly_verify_copy_invalid.test", 1 },
              { "testCopyChownValidVerifyGivenDockerfile", "/docker/Dockerfile_assembly_verify_copy_chown_valid.test", 0 }
      };
    }


    @Test
    public void testCopyOfDockerfile(@Injectable final KitLogger logger) throws IOException, URISyntaxException {
      BuildConfiguration buildConfig = createBuildConfig();

      AssemblyManager.verifyAssemblyReferencedInDockerfile(
              resourceTestFile(dockerFilePath), buildConfig, new Properties(), logger
      );

      // @formatter:off
      new Verifications() {{
        logger.warn(anyString, (Object []) any); times = expectedNumberOfInvocations;
      }};
      //@formatter:on
    }
  }

  private static File resourceTestFile(String path) throws URISyntaxException {
    final URL fileUrl = AssemblyManagerTest.class.getResource(path);
    Assertions.assertThat(fileUrl).isNotNull();

    return new File(fileUrl.toURI());
  }

  private static BuildConfiguration createBuildConfig() {
    return BuildConfiguration.builder()
            .assembly(AssemblyConfiguration.builder()
                    .name("maven")
                    .targetDir("/maven")
                    .build())
            .build();
  }

  static abstract class UnitTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected AssemblyManager assemblyManager;
    protected File targetDirectory;

    @Before
    public void setUp() throws Exception {
      assemblyManager = AssemblyManager.getInstance();
      targetDirectory = temporaryFolder.newFolder("target");
    }
  }
}

