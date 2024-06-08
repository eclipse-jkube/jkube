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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.apache.commons.io.FilenameUtils.separatorsToSystem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SuppressWarnings({"unused"})
class JibImageBuildServiceBuildTest {

  private File projectRoot;
  private Path targetDirectory;
  private Path dockerOutput;
  private ByteArrayOutputStream out;
  private JKubeServiceHub hub;
  private JibImageBuildService jibBuildService;

  @BeforeEach
  void setUp(@TempDir Path projectRoot) throws IOException {
    targetDirectory = Files.createDirectory(projectRoot.resolve("target"));
    dockerOutput = targetDirectory.resolve("docker");
    out = new ByteArrayOutputStream();
    hub = JKubeServiceHub.builder()
      .log(new KitLogger.PrintStreamLogger(new PrintStream(out)))
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
        .pullRegistryConfig(RegistryConfig.builder().settings(Collections.emptyList()).build())
        .pushRegistryConfig(RegistryConfig.builder().settings(Collections.emptyList()).build())
        .build())
      .build();
    jibBuildService = new JibImageBuildService(hub);
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
        .withMessage("Dockerfile mode is not supported with JIB build strategy");
  }

  @Test
  void build_withImageMissingBuildConfiguration_shouldNotBuildImage() throws JKubeServiceException {
    // Given
    final ImageConfiguration ic  = ImageConfiguration.builder()
      .name("test/foo:latest")
      .build();
    // When
    jibBuildService.build(ic);
    // Then
    assertThat(out.toString())
      .contains("[test/foo:latest] : Skipped building (Image configuration has no build settings)");
  }

  @Test
  void build_withImageBuildConfigurationSkipTrue_shouldNotBuildImage() throws JKubeServiceException {
    // Given
    final ImageConfiguration ic = ImageConfiguration.builder()
      .name("test/foo:latest")
      .build(BuildConfiguration.builder()
        .from("test/base:latest")
        .skip(true)
        .build())
      .build();
    // When
    jibBuildService.build(ic);
    // Then
    assertThat(out.toString())
      .contains("[test/foo:latest] : Skipped building" + System.lineSeparator());
  }

  @Test
  void build_shouldCallPluginServiceAddFiles() throws JKubeServiceException {
    // Given
    final ImageConfiguration ic = ImageConfiguration.builder()
      .name("test/foo:latest")
      .build();
    // When
    jibBuildService.build(ic);
    // Then
    assertThat(out.toString())
      .contains("Adding extra files for plugin org.eclipse.jkube");
  }

  @Test
  void build_shouldLogBuiltTarImage() throws JKubeServiceException {
    final ImageConfiguration ic = ImageConfiguration.builder()
      .name("test/foo:latest")
      .build(BuildConfiguration.builder()
        .from("gcr.io/distroless/base@sha256:8267a5d9fa15a538227a8850e81cf6c548a78de73458e99a67e8799bbffb1ba0")
        .build())
      .build();
    // When
    jibBuildService.build(ic);
    // Then
    assertThat(out.toString())
      .contains(separatorsToSystem("/latest/tmp/jib-image.linux-amd64.tar successfully built"));
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
            separatorsToSystem("deployments-layer/deployments"),
            separatorsToSystem("deployments-layer/deployments/to-deployments.txt"),
            "other-layer",
            separatorsToSystem("other-layer/deployments"),
            separatorsToSystem("other-layer/deployments/other"),
            separatorsToSystem("other-layer/deployments/other/to-other.txt"),
            "jkube-generated-layer-final-artifact",
            separatorsToSystem("jkube-generated-layer-final-artifact/deployments"),
            separatorsToSystem("jkube-generated-layer-final-artifact/deployments/final-artifact.jar"),
            "deployments"
        );
    ArchiveAssertions.assertThat(dockerDir.resolve("tmp").resolve("jib-image.linux-amd64.tar").toFile())
      .fileTree()
      .hasSize(17)
      .contains("config.json", "manifest.json")
      .haveExactly(15, new Condition<>(s -> s.endsWith(".tar.gz"), "Tar File layers"));
  }

  @Test
  void buildMultiplatform_shouldBuildSeparateTars() throws Exception {
    // Given
    final ImageConfiguration ic = ImageConfiguration.builder()
      .name("namespace/image-name:multiplatform")
      .build(BuildConfiguration.builder()
        .from("scratch")
        .platform("linux/amd64")
        .platform("linux/arm64")
        .build())
      .build();
    final Path dockerDir = dockerOutput.resolve("namespace").resolve("image-name").resolve("multiplatform");
    // When
    jibBuildService.build(ic);
    // Then
    ArchiveAssertions.assertThat(dockerDir.resolve("tmp").resolve("jib-image.linux-amd64.tar").toFile())
      .fileTree()
      .hasSize(2)
      .contains("config.json", "manifest.json");
    ArchiveAssertions.assertThat(dockerDir.resolve("tmp").resolve("jib-image.linux-arm64.tar").toFile())
      .fileTree()
      .hasSize(2)
      .contains("config.json", "manifest.json");
  }

  @Nested
  @DisplayName("with global registry")
  class GlobalRegistry {

    private ImageConfiguration imageConfiguration;
    private ByteArrayOutputStream out;

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
      out = new ByteArrayOutputStream();
      hub = hub.toBuilder()
        .configuration(hub.getConfiguration().toBuilder()
          .pullRegistryConfig(RegistryConfig.builder()
            .registry("gcr.io")
            .authConfig(Collections.emptyMap())
            .settings(Collections.emptyList())
            .build())
          .build())
        .build();
      jibBuildService = new JibImageBuildService(hub, new JibLogger(new KitLogger.SilentLogger(), new PrintStream(out)));
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
      ArchiveAssertions.assertThat(dockerDir.resolve("tmp").resolve("jib-image.linux-amd64.tar").toFile())
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
      assertThat(out.toString())
        .contains("Getting manifest for base image gcr.io/distroless/base");
    }

  }

}
