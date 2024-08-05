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
package org.eclipse.jkube.kit.service.jib;

import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.FileEntry;
import com.google.cloud.tools.jib.api.buildplan.ImageFormat;
import com.google.cloud.tools.jib.api.buildplan.Platform;
import com.google.cloud.tools.jib.api.buildplan.Port;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.build.api.assembly.BuildDirs;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JKubeException;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.jkube.kit.service.jib.JibServiceUtil.containerFromImageConfiguration;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class JibServiceUtilTest {

  @Nested
  @DisplayName("containerFromImageConfiguration")
  class ContainerFromImageConfiguration {

    private ImageConfiguration imageConfiguration;

    @BeforeEach
    void basicImageConfiguration() {
      imageConfiguration = ImageConfiguration.builder()
        .name("test/test-project")
        .build(BuildConfiguration.builder()
          .assembly(AssemblyConfiguration.builder()
            .layer(Assembly.builder()
              .files(Collections.singletonList(AssemblyFile.builder()
                .source(new File("${project.basedir}/foo"))
                .outputDirectory(new File("targetDir"))
                .build()))
              .build())
            .build())
          .entryPoint(Arguments.builder().exec(Arrays.asList("java", "-jar", "foo.jar")).build())
          .labels(Collections.singletonMap("foo", "bar"))
          .user("root")
          .workdir("/home/foo")
          .ports(Collections.singletonList("8080"))
          .volume("/mnt/volume1")
          .build())
        .build();
    }

    @ParameterizedTest(name = "{index}: from {0} returnsValidJibContainerBuilder")
    @ValueSource(strings = {"quay.io/test/test-image:test-tag", "scratch"})
    void returnsJibContainerBuilder(String from) {
      // Given
      imageConfiguration = imageConfiguration.toBuilder()
        .build(imageConfiguration.getBuild().toBuilder()
          .from(from)
          .build())
        .build();
      // When
      final JibContainerBuilder jibContainerBuilder = containerFromImageConfiguration(imageConfiguration, null, null);
      // Then
      assertThat(jibContainerBuilder.toContainerBuildPlan())
        .hasFieldOrPropertyWithValue("baseImage", from)
        .hasFieldOrPropertyWithValue("labels", Collections.singletonMap("foo", "bar"))
        .hasFieldOrPropertyWithValue("entrypoint", Arrays.asList("java", "-jar", "foo.jar"))
        .hasFieldOrPropertyWithValue("exposedPorts", new HashSet<>(Collections.singletonList(Port.tcp(8080))))
        .hasFieldOrPropertyWithValue("user", "root")
        .hasFieldOrPropertyWithValue("workingDirectory", AbsoluteUnixPath.get("/home/foo"))
        .hasFieldOrPropertyWithValue("volumes", new HashSet<>(Collections.singletonList(AbsoluteUnixPath.get("/mnt/volume1"))))
        .hasFieldOrPropertyWithValue("format", ImageFormat.Docker);
    }

  }

  @Nested
  @DisplayName("toImageReference")
  class ToImageReference {

    @Test
    void withImageName() {
      // Given
      final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .name("test/test-project")
        .build();
      // When
      final ImageReference result = JibServiceUtil.toImageReference(imageConfiguration);
      // Then
      assertThat(result)
        .hasFieldOrPropertyWithValue("tag", Optional.of("latest"))
        .returns("test/test-project", ImageReference::toString);
    }

    @Test
    void withImageNameAndTag() {
      // Given
      final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .name("test/test-project:1.0.0")
        .build();
      // When
      final ImageReference result = JibServiceUtil.toImageReference(imageConfiguration);
      // Then
      assertThat(result)
        .hasFieldOrPropertyWithValue("tag", Optional.of("1.0.0"))
        .hasFieldOrPropertyWithValue("registry", "registry-1.docker.io")
        .returns("test/test-project:1.0.0", ImageReference::toString);
    }

    @Test
    void withImageNameAndRegistry() {
      // Given
      final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .name("registry.example.com/test/test-project")
        .build();
      // When
      final ImageReference result = JibServiceUtil.toImageReference(imageConfiguration);
      // Then
      assertThat(result)
        .hasFieldOrPropertyWithValue("tag", Optional.of("latest"))
        .hasFieldOrPropertyWithValue("registry", "registry.example.com")
        .returns("registry.example.com/test/test-project", ImageReference::toString);
    }

    @Test
    void withInvalidName() {
      final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .name("test/test-project:1.0.0:latest")
        .build();
      assertThatThrownBy(() -> JibServiceUtil.toImageReference(imageConfiguration))
        .isInstanceOf(JKubeException.class)
        .hasMessage("Invalid image reference: test/test-project:1.0.0:latest");
    }
  }

  @Nested
  @DisplayName("platforms")
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class Platforms {

    @Test
    void withNullPlatforms_returnsLinuxAmd64() {
      // Given
      final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .build(BuildConfiguration.builder().build())
        .build();
      // When
      final Set<Platform> result = JibServiceUtil.platforms(imageConfiguration);
      // Then
      assertThat(result).containsExactly(new Platform("amd64", "linux"));
    }

    @Test
    void withEmptyPlatforms_returnsLinuxAmd64() {
      // Given
      final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .build(BuildConfiguration.builder()
          .platforms(Collections.emptyList())
          .build())
        .build();
      // When
      final Set<Platform> result = JibServiceUtil.platforms(imageConfiguration);
      // Then
      assertThat(result).containsExactly(new Platform("amd64", "linux"));
    }

    @Test
    void withInvalidPlatform_returnsLinuxAmd64() {
      // Given
      final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .build(BuildConfiguration.builder()
          .platform("not-a-platform")
          .build())
        .build();
      // When
      final Set<Platform> result = JibServiceUtil.platforms(imageConfiguration);
      // Then
      assertThat(result).containsExactly(new Platform("amd64", "linux"));
    }

    @ParameterizedTest(name = "{index}: with platform {0} returns os {1} and arch {2}")
    @MethodSource("platforms")
    void withPlatforms(String platform, String expectedOs, String expectedArch) {
      // Given
      final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .build(BuildConfiguration.builder()
          .platforms(Collections.singletonList(platform))
          .build())
        .build();
      // When
      final Set<Platform> result = JibServiceUtil.platforms(imageConfiguration);
      // Then
      assertThat(result)
        .singleElement()
        .hasFieldOrPropertyWithValue("os", expectedOs)
        .hasFieldOrPropertyWithValue("architecture", expectedArch);
    }

    public Stream<org.junit.jupiter.params.provider.Arguments> platforms() {
      return Stream.of(
        arguments("linux/amd64", "linux", "amd64"),
        arguments("linux/arm64", "linux", "arm64"),
        arguments("linux/arm", "linux", "arm"),
        arguments("linux/arm/v5", "linux", "arm/v5"),
        arguments("linux/arm/v7", "linux", "arm/v7"),
        arguments("darwin/arm64", "darwin", "arm64")
      );
    }
  }

  @Nested
  @DisplayName("getBaseImage")
  class GetBaseImage {

    @Test
    void withNullBuildConfig() {
      assertThat(JibServiceUtil.getBaseImage(ImageConfiguration.builder().build(), null)).isEqualTo("busybox:latest");
    }

    @Test
    void withBuildConfig() {
      // Given
      final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .build(BuildConfiguration.builder()
          .from("quay.io/jkubeio/jkube-test-image:0.0.1")
          .build())
        .build();
      // When
      final String result = JibServiceUtil.getBaseImage(imageConfiguration, null);
      // Then
      assertThat(result).isEqualTo("quay.io/jkubeio/jkube-test-image:0.0.1");
    }
  }

  @Nested
  @DisplayName("layers")
  class Layers {
    @Test
    void withEmptyLayers_shouldReturnEmpty() {
      // When
      final List<FileEntriesLayer> result = JibServiceUtil.layers(null, Collections.emptyMap());
      // Then
      assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void withMultipleLayers_shouldReturnTransformedLayers(@TempDir Path temporaryFolder) throws IOException {
      // Given
      final BuildDirs buildDirs = new BuildDirs("layers-test", JKubeConfiguration.builder()
        .outputDirectory("target/docker")
        .project(JavaProject.builder().baseDirectory(temporaryFolder.toFile()).build())
        .build());
      final Map<Assembly, List<AssemblyFileEntry>> originalLayers = new LinkedHashMap<>();
      originalLayers.put(Assembly.builder().id("layer-1").build(), Arrays.asList(
        AssemblyFileEntry.builder().source(Files.createTempFile(temporaryFolder, "junit", "ext").toFile())
          .dest(buildDirs.getOutputDirectory().toPath().resolve("layer-1").resolve("l1.1.txt").toFile()).build(),
        AssemblyFileEntry.builder().source(Files.createTempFile(temporaryFolder, "junit", "ext").toFile())
          .dest(buildDirs.getOutputDirectory().toPath().resolve("layer-1").resolve("l1.2.txt").toFile()).build()
      ));
      originalLayers.put(Assembly.builder().build(), Arrays.asList(
        AssemblyFileEntry.builder().source(Files.createTempFile(temporaryFolder, "junit", "ext").toFile())
          .dest(new File(buildDirs.getOutputDirectory(), "l2.1.txt")).build(),
        AssemblyFileEntry.builder().source(Files.createTempFile(temporaryFolder, "junit", "ext").toFile())
          .dest(new File(buildDirs.getOutputDirectory(), "l2.2.txt")).build()
      ));
      // Creates a denormalized path in JDK 8
      originalLayers.put(Assembly.builder().id("jkube-generated-layer-final-artifact").build(), Collections.singletonList(
        AssemblyFileEntry.builder().source(Files.createTempFile(temporaryFolder, "junit", "ext").toFile())
          .dest(buildDirs.getOutputDirectory().toPath().resolve("jkube-generated-layer-final-artifact")
            .resolve("deployments").resolve(".").resolve("edge.case").toFile()).build()
      ));
      // When
      final List<FileEntriesLayer> result = JibServiceUtil.layers(buildDirs, originalLayers);
      // Then
      assertThat(result).hasSize(3)
        .anySatisfy(fel -> assertThat(fel)
          .hasFieldOrPropertyWithValue("name", "layer-1")
          .extracting(FileEntriesLayer::getEntries)
          .asInstanceOf(InstanceOfAssertFactories.list(FileEntry.class))
          .extracting("extractionPath.unixPath")
          .containsExactly("/l1.1.txt", "/l1.2.txt")
        )
        .anySatisfy(fel -> assertThat(fel)
          .hasFieldOrPropertyWithValue("name", "")
          .extracting(FileEntriesLayer::getEntries)
          .asInstanceOf(InstanceOfAssertFactories.list(FileEntry.class))
          .extracting("extractionPath.unixPath")
          .containsExactly("/l2.1.txt", "/l2.2.txt")
        )
        .anySatisfy(fel -> assertThat(fel)
          .hasFieldOrPropertyWithValue("name", "jkube-generated-layer-final-artifact")
          .extracting(FileEntriesLayer::getEntries)
          .asInstanceOf(InstanceOfAssertFactories.list(FileEntry.class))
          .extracting("extractionPath.unixPath")
          .containsExactly("/deployments/edge.case")
        )
        .extracting(FileEntriesLayer::getName)
        .containsExactly("layer-1", "", "jkube-generated-layer-final-artifact");
    }

  }
}
