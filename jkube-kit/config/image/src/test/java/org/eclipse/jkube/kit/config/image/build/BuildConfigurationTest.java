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
package org.eclipse.jkube.kit.config.image.build;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.jkube.kit.common.AssemblyConfiguration;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.common.archive.ArchiveCompression.bzip2;
import static org.eclipse.jkube.kit.common.archive.ArchiveCompression.gzip;
import static org.eclipse.jkube.kit.common.archive.ArchiveCompression.none;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author roland
 */
public class BuildConfigurationTest {

  @Test
  public void emptyConfigurationShouldBeValidNonDockerfileWithDefaults() {
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
  public void dockerFileShouldBeValidDockerfile() {
    BuildConfiguration config = BuildConfiguration.builder()
        .dockerFile("src/docker/Dockerfile").build();
    config.validate();
    assertThat(config)
        .hasFieldOrPropertyWithValue("dockerFileMode", true)
        .returns(new File("src/docker/Dockerfile"), BuildConfiguration::calculateDockerFilePath);
  }

  @Test
  public void contextDirShouldBeValidDockerfile() {
    BuildConfiguration config = BuildConfiguration.builder()
        .contextDir("src/docker/").build();
    config.validate();
    assertThat(config)
        .hasFieldOrPropertyWithValue("dockerFileMode", true)
        .returns(new File("src/docker/Dockerfile"), BuildConfiguration::calculateDockerFilePath);
  }

  @Test
  public void contextDirAndDockerFileShouldBeValidDockerfile() {
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
  public void getContextDirWithNoContextAndDockerFileWithNoParent() {
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
  public void getContextDirWithNoContextAndDockerFileWithParent() {
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

  @Test(expected = IllegalArgumentException.class)
  public void dockerFileAndDockerArchiveShouldBeInvalid() {
    BuildConfiguration config = BuildConfiguration.builder()
        .dockerArchive("this")
        .dockerFile("that").build();

    config.validate();

    fail("Should have failed.");
  }

  @Test
  public void dockerArchiveShouldBeValidNonDockerfile() {
    BuildConfiguration config = BuildConfiguration.builder()
        .dockerArchive("docker-archive.tar").build();
    config.validate();
    config.initAndValidate();

    assertThat(config)
        .hasFieldOrPropertyWithValue("dockerFileMode", false)
        .hasFieldOrPropertyWithValue("dockerArchive", new File("docker-archive.tar"));
  }

  @Test
  public void compressionStringGzip() {
    BuildConfiguration config = BuildConfiguration.builder()
        .compressionString("gzip").build();
    assertThat(config.getCompression()).isEqualTo(gzip);
  }

  @Test
  public void compressionStringNone() {
    BuildConfiguration config = BuildConfiguration.builder().build();
    assertThat(config.getCompression()).isEqualTo(none);
  }

  @Test
  public void compressionStringBzip2() {
    BuildConfiguration config = BuildConfiguration.builder()
        .compressionString("bzip2").build();
    assertThat(config.getCompression()).isEqualTo(bzip2);
  }

  @Test
  public void compressionStringInvalid() {
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> builder.compressionString("bzip"));
    assertThat(result)
        .hasMessageStartingWith("No enum constant")
        .hasMessageEndingWith("ArchiveCompression.bzip");
  }

  @Test
  public void isValidWindowsFileName() {
    assertFalse(BuildConfiguration.isValidWindowsFileName("/Dockerfile"));
    assertTrue(BuildConfiguration.isValidWindowsFileName("Dockerfile"));
    assertFalse(BuildConfiguration.isValidWindowsFileName("Dockerfile/"));
  }

  @Test
  public void cacheFrom() {
    final BuildConfiguration buildConfiguration = BuildConfiguration.builder()
            .addCacheFrom("foo/bar:latest")
            .build();
    assertThat(buildConfiguration.getCacheFrom())
        .containsExactly("foo/bar:latest");
  }

  @Test
  public void testBuilder(@Mocked AssemblyConfiguration mockAssemblyConfiguration) {
    // Given
    // @formatter:off
    new Expectations() {{
      mockAssemblyConfiguration.getName();
      result = "1337";
    }};
    // @formatter:on
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
  public void getAssemblies_withNoAssemblyAndNoAssemblies_shouldReturnEmptyList() {
    // Given
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder()
        .user("test");
    // When
    final BuildConfiguration result = builder.build();
    // Then
    assertThat(result.getAssemblies()).isNotNull().isEmpty();
  }

  @Test
  public void getAssemblies_withNoAssemblyAndAssemblies_shouldReturnAssemblies() {
    // Given
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder()
        .user("test")
        .assemblies(Collections.singletonList(AssemblyConfiguration.builder().name("assembly-1").build()));
    // When
    final BuildConfiguration result = builder.build();
    // Then
    assertThat(result.getAssemblies())
        .extracting("name")
        .containsExactly("assembly-1");
  }

  @Test
  public void getAssemblies_withAssemblyAndNoAssemblies_shouldReturnAssembly() {
    // Given
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder()
        .user("test")
        .assemblyDeprecated(AssemblyConfiguration.builder().name("assembly-1").build());
    // When
    final BuildConfiguration result = builder.build();
    // Then
    assertThat(result.getAssemblies())
        .extracting("name")
        .containsExactly("assembly-1");
  }

  @Test
  public void getAssemblies_withAssemblyAndAssemblies_shouldReturnAssembliesAndAssemblyLast() {
    // Given
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder()
        .user("test")
        .assembly(AssemblyConfiguration.builder().name("assembly-1").build())
        .assembly(AssemblyConfiguration.builder().name("assembly-2").build())
        .assembly(AssemblyConfiguration.builder().name("assembly-3").build())
        .assemblyDeprecated(AssemblyConfiguration.builder().name("assembly-deprecated").build());
    // When
    final BuildConfiguration result = builder.build();
    // Then
    assertThat(result.getAssemblies())
        .extracting("name")
        .containsExactly("assembly-1", "assembly-2", "assembly-3", "assembly-deprecated");
  }

  /**
   * Verifies that deserialization works for raw deserialization (Maven-Plexus) disregarding annotations.
   *
   * Especially designed to catch problems if Enum names are changed.
   */
  @Test
  public void rawDeserialization() throws IOException {
    // Given
    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(MapperFeature.USE_ANNOTATIONS, false);
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
        .hasFieldOrPropertyWithValue("assembly.name", "deprecated-single")
        .extracting(BuildConfiguration::getAssemblies).asList().extracting("name")
        .containsExactly("multi-layer-support", "not-the-last-layer", "deprecated-single");
  }
}
