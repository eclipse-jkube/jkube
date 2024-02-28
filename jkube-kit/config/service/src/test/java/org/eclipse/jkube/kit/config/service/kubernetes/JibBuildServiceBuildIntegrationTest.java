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
package org.eclipse.jkube.kit.config.service.kubernetes;

import com.fasterxml.jackson.core.type.TypeReference;
import org.assertj.core.api.Condition;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.assertj.ArchiveAssertions;
import org.eclipse.jkube.kit.common.assertj.FileAssertions;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.service.jib.JibLogger;
import org.fusesource.jansi.Ansi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"unused"})
class JibBuildServiceBuildIntegrationTest {

  private File projectRoot;
  private Path targetDirectory;
  private Path dockerOutput;
  private JKubeServiceHub hub;
  private JibBuildService jibBuildService;

  @BeforeEach
  void setUp(@TempDir Path projectRoot) throws IOException {
    targetDirectory = Files.createDirectory(projectRoot.resolve("target"));
    dockerOutput = targetDirectory.resolve("docker");
    hub = JKubeServiceHub.builder()
      .log(new KitLogger.SilentLogger())
      .platformMode(RuntimeMode.KUBERNETES)
      .buildServiceConfig(BuildServiceConfig.builder().build())
      .configuration(JKubeConfiguration.builder()
        .outputDirectory("target/docker")
        .project(JavaProject.builder()
          .buildFinalName("final-artifact")
          .packaging("jar")
          .baseDirectory(projectRoot.toFile())
          .buildDirectory(targetDirectory.toFile())
          .outputDirectory(targetDirectory.toFile())
          .properties(new Properties())
          .build())
        .registryConfig(RegistryConfig.builder().settings(Collections.emptyList()).build())
        .build())
      .build();
    jibBuildService = new JibBuildService(hub);
  }

  @Test
  void build_dockerFileMode_shouldThrowException() {
    // Given
    final ImageConfiguration ic = ImageConfiguration.builder()
        .name("namespace/image-name:tag")
        .build(BuildConfiguration.builder().dockerFile("Dockerfile").build())
        .build();
    // When + Then
    assertThatExceptionOfType(JKubeServiceException.class)
        .isThrownBy(() -> jibBuildService.build(ic))
        .withMessage("Error when building JIB image")
        .havingCause()
        .withMessage("Dockerfile mode is not supported with JIB build strategy");
  }

  @Test
  void build_withLayersAndArtifact_shouldPerformJibBuild() throws Exception {
    // Given
    Files.createFile(targetDirectory.resolve( "final-artifact.jar"));
    final File dlFile = Files.createFile(targetDirectory.resolve( "to-deployments.txt")).toFile();
    final File otherFile = Files.createFile(targetDirectory.resolve( "to-other.txt")).toFile();
    final ImageConfiguration ic = ImageConfiguration.builder()
      .name("namespace/image-name:tag")
      .build(BuildConfiguration.builder()
        .from("gcr.io/distroless/base@sha256:8267a5d9fa15a538227a8850e81cf6c548a78de73458e99a67e8799bbffb1ba0")
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
        .volume("/deployments")
        .build())
      .build();
    // When
    jibBuildService.build(ic);
    // Then
    final Path dockerDir = dockerOutput.resolve("namespace").resolve("image-name").resolve("tag");
    assertThat(dockerDir.resolve("build"))
      .exists().isDirectory()
      .extracting(p -> p.resolve("Dockerfile").toFile()).asInstanceOf(InstanceOfAssertFactories.FILE)
      .hasContent("FROM gcr.io/distroless/base@sha256:8267a5d9fa15a538227a8850e81cf6c548a78de73458e99a67e8799bbffb1ba0\n" +
          "COPY /deployments-layer/deployments /deployments/\n" +
          "COPY /other-layer/deployments /deployments/\n" +
          "COPY /jkube-generated-layer-final-artifact/deployments /deployments/\n" +
          "VOLUME [\"/deployments\"]");
    FileAssertions.assertThat(dockerDir.resolve("build").toFile())
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
    ArchiveAssertions.assertThat(dockerDir.resolve("tmp").resolve("docker-build.tar").toFile())
      .fileTree()
      .hasSize(17)
      .contains("config.json", "manifest.json")
      .haveExactly(15, new Condition<>(s -> s.endsWith(".tar.gz"), "Tar File layers"));
  }

  @Nested
  @DisplayName("with global registry")
  class GlobalRegistry {

    private ImageConfiguration imageConfiguration;
    private PrintStream out;

    @BeforeEach
    void setUp() {
      imageConfiguration = ImageConfiguration.builder()
        .name("namespace/image-name:tag")
        .build(BuildConfiguration.builder()
          .from("distroless/base")
          .user("1000")
          .entryPoint(Arguments.builder().execArgument("sleep").execArgument("3600").build())
          .build())
        .registry("gcr.io")
        .build();
      hub = hub.toBuilder().configuration(hub.getConfiguration().toBuilder()
          .registryConfig(RegistryConfig.builder()
            .registry("gcr.io")
            .authConfig(Collections.emptyMap())
            .settings(Collections.emptyList())
            .build())
          .build())
        .build();
      out = spy(System.out);
      jibBuildService = new JibBuildService(hub, new JibLogger(hub.getLog(), out));
    }

    @Test
    void shouldConsiderRegistryForTargetImage() throws Exception {
      // When
      jibBuildService.build(imageConfiguration);
      // Then
      final Path dockerDir = dockerOutput
        .resolve("gcr.io").resolve("namespace").resolve("image-name").resolve("tag");
      // Note that the registry is part of the directory tree
      assertThat(dockerDir).exists().isDirectory();
      ArchiveAssertions.assertThat(dockerDir.resolve("tmp").resolve("docker-build.tar").toFile())
        .entry("manifest.json")
        .asString()
        .satisfies(manifest -> assertThat(Serialization.unmarshal(manifest, new TypeReference<List<Map<String, Object>>>(){}))
          .singleElement().asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
          .extracting("RepoTags").asInstanceOf(InstanceOfAssertFactories.list(String.class))
          .singleElement().isEqualTo("gcr.io/namespace/image-name:tag")
        );
    }

    @Test
    void shouldConsiderRegistryForFromImage() throws Exception {
      // When
      jibBuildService.build(imageConfiguration);
      // Then
      verify(out)
        .println(argThat((Ansi ansi) -> ansi.toString().contains("Getting manifest for base image gcr.io/distroless/base")));
    }

  }

}
