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
package org.eclipse.jkube.kit.config.image.build;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.assertj.core.api.Assertions;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.common.archive.ArchiveCompression.bzip2;
import static org.eclipse.jkube.kit.common.archive.ArchiveCompression.gzip;
import static org.eclipse.jkube.kit.common.archive.ArchiveCompression.none;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author roland
 */

class BuildConfigurationTest {

  @Test
  void emptyConfigurationShouldBeValidNonDockerfileWithDefaults() {
    BuildConfiguration config = new BuildConfiguration();
    config.validate();
    assertThat(config)
        .hasFieldOrPropertyWithValue("dockerFileMode", false)
        .hasFieldOrPropertyWithValue("dockerFile", null)
        .hasFieldOrPropertyWithValue("compression", none)
        .returns(CleanupMode.TRY_TO_REMOVE, BuildConfiguration::cleanupMode)
        .returns(false, BuildConfiguration::optimise)
        .returns(false, BuildConfiguration::nocache);
  }

  @Test
  void dockerFileShouldBeValidDockerfile() {
    BuildConfiguration config = BuildConfiguration.builder()
        .dockerFile("src/docker/Dockerfile").build();
    config.validate();
    assertThat(config)
        .hasFieldOrPropertyWithValue("dockerFileMode", true)
        .returns(new File("src/docker/Dockerfile"), BuildConfiguration::calculateDockerFilePath);
  }

  @Test
  void contextDirShouldBeValidDockerfile() {
    BuildConfiguration config = BuildConfiguration.builder()
        .contextDir("src/docker/").build();
    config.validate();
    assertThat(config)
        .hasFieldOrPropertyWithValue("dockerFileMode", true)
        .returns(new File("src/docker/Dockerfile"), BuildConfiguration::calculateDockerFilePath);
  }

  @Test
  void contextDirAndDockerFileShouldBeValidDockerfile() {
    BuildConfiguration config = BuildConfiguration.builder()
        .contextDir("/tmp/")
        .dockerFile("Docker-file").build();
    config.validate();
    config.initAndValidate();
    assertThat(config)
        .hasFieldOrPropertyWithValue("dockerFileMode", true)
        .hasFieldOrPropertyWithValue("dockerFile", new File("/tmp/Docker-file"))
        .returns(new File("/tmp/Docker-file"), BuildConfiguration::calculateDockerFilePath);
  }

  @Test
  void getContextDirWithNoContextAndDockerFileWithNoParent() {
    // Given
    final BuildConfiguration config = BuildConfiguration.builder()
        .dockerFile("Docker-file").build();
    config.validate();
    config.initAndValidate();
    // When
    final File result = config.getContextDir();
    // Then
    assertThat(result).isEqualTo(new File("."));
  }

  @Test
  void getContextDirWithNoContextAndDockerFileWithParent() {
    // Given
    final BuildConfiguration config = BuildConfiguration.builder()
        .dockerFile("parent-dir/Docker-file").build();
    config.validate();
    config.initAndValidate();
    // When
    final File result = config.getContextDir();
    // Then
    assertThat(result).isEqualTo(new File("parent-dir"));
  }

  @Test
  void dockerFileAndDockerArchiveShouldBeInvalid() {
    BuildConfiguration config = BuildConfiguration.builder()
        .dockerArchive("this")
        .dockerFile("that").build();
    assertThrows(IllegalArgumentException.class, config::validate, "Should have failed.");
  }

  @Test
  void dockerArchiveShouldBeValidNonDockerfile() {
    BuildConfiguration config = BuildConfiguration.builder()
        .dockerArchive("docker-archive.tar").build();
    config.validate();
    config.initAndValidate();

    assertThat(config)
        .hasFieldOrPropertyWithValue("dockerFileMode", false)
        .hasFieldOrPropertyWithValue("dockerArchive", new File("docker-archive.tar"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("compressionTestData")
  void compressionTest(String testDesc, String compressionString, ArchiveCompression compression) {
    BuildConfiguration config = BuildConfiguration.builder()
            .compressionString(compressionString).build();
    assertThat(config.getCompression()).isEqualTo(compression);
  }

  public static Stream<Arguments> compressionTestData() {
    return Stream.of(
            Arguments.arguments("Compression String Gzip", "gzip", gzip),
            Arguments.arguments("Compression String None", null, none),
            Arguments.arguments("Compression String Bzip2", "bzip2", bzip2)
    );
  }

  @Test
  void compressionStringInvalid() {
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
    Assertions.assertThatThrownBy(() ->builder.compressionString("bzip")).
            isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("No enum constant")
            .hasMessageEndingWith("ArchiveCompression.bzip");
  }


  @ParameterizedTest(name = "{0} : should be {1}")
  @MethodSource("isValidWindowsFileNameTestData")
  void isValidWindowsFileName(String filename, boolean expected) {
    assertThat(BuildConfiguration.isValidWindowsFileName(filename)).isEqualTo(expected);
  }

  public static Stream<Arguments> isValidWindowsFileNameTestData() {
    return Stream.of(
            Arguments.arguments("/Dockerfile", false),
            Arguments.arguments("Dockerfile", true),
            Arguments.arguments("Dockerfile/", false)
    );
  }

  @Test
  void cacheFrom() {
    final BuildConfiguration buildConfiguration = BuildConfiguration.builder()
            .addCacheFrom("foo/bar:latest")
            .build();
    assertThat(buildConfiguration.getCacheFrom())
        .containsExactly("foo/bar:latest");
  }

  @Test
  void testBuilder() {
    AssemblyConfiguration mockAssemblyConfiguration = mock(AssemblyConfiguration.class);
    // Given
    when(mockAssemblyConfiguration.getName()).thenReturn("1337");
    // When
    final BuildConfiguration result = BuildConfiguration.builder()
        .assembly(mockAssemblyConfiguration)
        .user("super-user")
        .build();
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("user", "super-user")
        .hasFieldOrPropertyWithValue("assembly.name", "1337");
  }

  @Test
  void getAssembly_withNoAssembly_shouldReturnNull() {
    // Given
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder()
        .user("test");
    // When
    final BuildConfiguration result = builder.build();
    // Then
    assertThat(result.getAssembly()).isNull();
  }

  @Test
  void getAssembly_withAssembly_shouldReturnAssembly() {
    // Given
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder()
        .user("test")
        .assembly(AssemblyConfiguration.builder().name("assembly-1").build());
    // When
    final BuildConfiguration result = builder.build();
    // Then
    assertThat(result.getAssembly())
        .hasFieldOrPropertyWithValue("name", "assembly-1");
  }

  /**
   * Verifies that deserialization works for raw deserialization (Maven-Plexus) disregarding annotations.
   *
   * Especially designed to catch problems if Enum names are changed.
   */
  @Test
  void rawDeserialization() throws IOException {
    // Given
    final ObjectMapper mapper = JsonMapper.builder().configure(MapperFeature.USE_ANNOTATIONS, false).build();
    // When
    final BuildConfiguration result = mapper.readValue(
        BuildConfigurationTest.class.getResourceAsStream("/build-configuration.json"),
        BuildConfiguration.class
    );
    // Then
    assertThat(result)
        .isNotNull()
        .hasFieldOrPropertyWithValue("contextDirRaw", "context")
        .hasFieldOrPropertyWithValue("contextDir", new File("context"))
        .hasFieldOrPropertyWithValue("dockerFileRaw", "Dockerfile.jvm")
        .hasFieldOrPropertyWithValue("dockerArchiveRaw", "docker-archive.tar")
        .hasFieldOrPropertyWithValue("filter", "@")
        .hasFieldOrPropertyWithValue("from", "jkube-images/image:1337")
        .hasFieldOrPropertyWithValue("fromExt", Collections.singletonMap("name", "jkube-images/image:ext"))
        .hasFieldOrPropertyWithValue("maintainer", "A-Team")
        .hasFieldOrPropertyWithValue("ports", Collections.singletonList("8080"))
        .hasFieldOrPropertyWithValue("shell.shell", "java -version")
        .hasFieldOrPropertyWithValue("imagePullPolicy", "Always")
        .hasFieldOrPropertyWithValue("runCmds", Arrays.asList("ls -la", "sleep 1", "echo done"))
        .hasFieldOrPropertyWithValue("cleanup", "none")
        .returns(CleanupMode.NONE, BuildConfiguration::cleanupMode)
        .hasFieldOrPropertyWithValue("nocache", true)
        .hasFieldOrPropertyWithValue("optimise", false)
        .hasFieldOrPropertyWithValue("volumes", Collections.singletonList("volume 1"))
        .hasFieldOrPropertyWithValue("tags", Arrays.asList("latest", "1337"))
        .hasFieldOrPropertyWithValue("env", Collections.singletonMap("JAVA_OPTS", "-Xmx1337m"))
        .hasFieldOrPropertyWithValue("labels", Collections.singletonMap("label-1", "label"))
        .hasFieldOrPropertyWithValue("args", Collections.singletonMap("CODE_VERSION", "latest"))
        .hasFieldOrPropertyWithValue("entryPoint.exec", Collections.singletonList("java -version"))
        .hasFieldOrPropertyWithValue("workdir", "/tmp")
        .hasFieldOrPropertyWithValue("cmd.exec", Arrays.asList("sh", "-c"))
        .hasFieldOrPropertyWithValue("user", "root")
        .hasFieldOrPropertyWithValue("healthCheck.mode", HealthCheckMode.cmd)
        .hasFieldOrPropertyWithValue("skip", false)
        .hasFieldOrPropertyWithValue("compression", gzip)
        .hasFieldOrPropertyWithValue("buildOptions", Collections.singletonMap("NetworkMode", "bridge"))
        .hasFieldOrPropertyWithValue("assembly.name", "the-assembly");
  }

}
