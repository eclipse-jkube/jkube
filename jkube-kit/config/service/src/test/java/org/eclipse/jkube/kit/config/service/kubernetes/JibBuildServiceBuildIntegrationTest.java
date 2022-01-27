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
package org.eclipse.jkube.kit.config.service.kubernetes;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;

import org.assertj.core.api.Condition;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.assertj.ArchiveAssertions;
import org.eclipse.jkube.kit.common.assertj.FileAssertions;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import mockit.Expectations;
import mockit.Mocked;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.AbstractFileAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
public class JibBuildServiceBuildIntegrationTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mocked
  private JKubeServiceHub hub;
  @Mocked
  private KitLogger log;
  private File projectRoot;
  private File targetDirectory;
  private ImageConfiguration imageConfiguration;
  private JibBuildService jibBuildService;

  @Before
  public void setUp() throws Exception {
    projectRoot = temporaryFolder.getRoot();
    imageConfiguration = ImageConfiguration.builder()
        .name("registry/image-name:tag")
        .build(BuildConfiguration.builder().build())
        .build();
    targetDirectory = temporaryFolder.newFolder("target");
    final JKubeConfiguration configuration = JKubeConfiguration.builder()
        .outputDirectory("target/docker")
        .project(JavaProject.builder()
            .buildFinalName("final-artifact")
            .packaging("jar")
            .baseDirectory(projectRoot)
            .buildDirectory(targetDirectory)
            .properties(new Properties())
            .build())
        .registryConfig(RegistryConfig.builder().settings(Collections.emptyList()).build())
        .build();
    // @formatter:off
    new Expectations() {{
      hub.getConfiguration(); result = configuration;
      hub.getLog(); result = log;
    }};
    // @formatter:on
    jibBuildService = new JibBuildService(hub);
  }

  @Test
  public void build_dockerFileMode_shouldThrowException() {
    // Given
    final ImageConfiguration ic = imageConfiguration.toBuilder()
        .build(imageConfiguration.getBuild().toBuilder().dockerFile("Dockerfile").build())
        .build();
    // When
    final JKubeServiceException result = assertThrows(JKubeServiceException.class, () -> jibBuildService.build(Collections.singletonList(ic)));
    // Then
    assertThat(result).hasMessage("Error when building JIB image")
        .getCause().hasMessage("Dockerfile mode is not supported with JIB build strategy");
  }

  @Test
  public void build_withLayersAndArtifact_shouldPerformJibBuild() throws Exception {
    // Given
    FileUtils.touch(new File(targetDirectory, "final-artifact.jar"));
    final File dlFile = new File(targetDirectory, "to-deployments.txt");
    FileUtils.touch(dlFile);
    final File otherFile = new File(targetDirectory, "to-other.txt");
    FileUtils.touch(otherFile);
    final ImageConfiguration ic = imageConfiguration.toBuilder()
        .build(BuildConfiguration.builder()
            .assembly(AssemblyConfiguration.builder()
                .name("custom")
                .targetDir("/deployments")
                .layer(Assembly.builder()
                    .id("deployments-layer")
                    .file(AssemblyFile.builder()
                        .outputDirectory(new File(".")).source(dlFile).destName(dlFile.getName()).build())
                    .build())
                .layer(Assembly.builder()
                    .id("other-layer")
                    .file(AssemblyFile.builder()
                        .outputDirectory(new File("other")).source(otherFile)
                        .destName(otherFile.getName()).build())
                    .build())
                .build())
            .build())
        .build();
    // When
    jibBuildService.build(Collections.singletonList(ic));
    // Then
    assertDockerFile()
        .hasContent("FROM busybox\n" +
            "COPY /deployments-layer/deployments /deployments/\n" +
            "COPY /other-layer/deployments /deployments/\n" +
            "COPY /jkube-generated-layer-final-artifact/deployments /deployments/\n" +
            "VOLUME [\"/deployments\"]");
    FileAssertions.assertThat(resolveDockerBuildDirs().resolve("build").toFile())
        .fileTree()
        .containsExactlyInAnyOrder(
            "Dockerfile",
            "deployments-layer",
            "deployments-layer/deployments",
            "deployments-layer/deployments/to-deployments.txt",
            "other-layer",
            "other-layer/deployments",
            "other-layer/deployments/other",
            "other-layer/deployments/other/to-other.txt",
            "jkube-generated-layer-final-artifact",
            "jkube-generated-layer-final-artifact/deployments",
            "jkube-generated-layer-final-artifact/deployments/final-artifact.jar",
            "deployments"
        );
    ArchiveAssertions.assertThat(resolveDockerBuildDirs().resolve("tmp").resolve("docker-build.tar").toFile())
      .fileTree()
      .hasSize(6)
      .contains("config.json", "manifest.json")
      .haveExactly(4, new Condition<>(s -> s.endsWith(".tar.gz"), "Tar File layers"));
  }



  private Path resolveDockerBuildDirs() {
    return projectRoot.toPath().resolve("target")
        .resolve("docker").resolve("registry").resolve("image-name").resolve("tag");
  }

  private AbstractFileAssert<?> assertDockerFile() {
    return assertThat(resolveDockerBuildDirs().resolve("build").resolve("Dockerfile").toFile()).isFile().exists();
  }
}
