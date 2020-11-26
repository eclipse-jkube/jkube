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
import java.util.Collections;

import org.eclipse.jkube.kit.common.AssemblyConfiguration;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import static org.eclipse.jkube.kit.common.archive.ArchiveCompression.bzip2;
import static org.eclipse.jkube.kit.common.archive.ArchiveCompression.gzip;
import static org.eclipse.jkube.kit.common.archive.ArchiveCompression.none;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
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
    assertFalse(config.isDockerFileMode());
    assertThat(config.getDockerFile(), nullValue());
    assertThat(config.getCompression(), is(none));
    assertThat(config.cleanupMode(), is(CleanupMode.TRY_TO_REMOVE));
    assertThat(config.optimise(), is(false));
    assertThat(config.nocache(), is(false));
  }

  @Test
  public void dockerFileShouldBeValidDockerfile() {
    BuildConfiguration config = BuildConfiguration.builder()
        .dockerFile("src/docker/Dockerfile").build();
    config.validate();
    assertTrue(config.isDockerFileMode());
    assertEquals(config.calculateDockerFilePath(),new File("src/docker/Dockerfile"));
  }

  @Test
  public void contextDirShouldBeValidDockerfile() {
    BuildConfiguration config = BuildConfiguration.builder()
        .contextDir("src/docker/").build();
    config.validate();
    assertTrue(config.isDockerFileMode());
    assertEquals(config.calculateDockerFilePath(), new File("src/docker/Dockerfile"));
  }

  @Test
  public void contextDirAndDockerFileShouldBeValidDockerfile() {
    BuildConfiguration config = BuildConfiguration.builder()
        .contextDir("/tmp/")
        .dockerFile("Docker-file").build();
    config.validate();
    config.initAndValidate();
    assertTrue(config.isDockerFileMode());
    assertEquals(config.getDockerFile(), new File("/tmp/Docker-file"));
    assertEquals(config.calculateDockerFilePath(), new File("/tmp/Docker-file"));
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

    assertFalse(config.isDockerFileMode());
    assertEquals(new File("docker-archive.tar"), config.getDockerArchive());
  }

  @Test
  public void compressionStringGzip() {
    BuildConfiguration config = BuildConfiguration.builder()
        .compressionString("gzip").build();
    assertEquals(gzip, config.getCompression());
  }

  @Test
  public void compressionStringNone() {
    BuildConfiguration config = BuildConfiguration.builder().build();
    assertEquals(none, config.getCompression());
  }

  @Test
  public void compressionStringBzip2() {
    BuildConfiguration config = BuildConfiguration.builder()
        .compressionString("bzip2").build();
    assertEquals(bzip2, config.getCompression());
  }

  @Test
  public void compressionStringInvalid() {
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> builder.compressionString("bzip"));
    assertThat(result.getMessage(), allOf(startsWith("No enum constant"), endsWith("ArchiveCompression.bzip")));
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
            .cacheFrom(Collections.singletonList("foo/bar:latest"))
            .build();
    assertEquals(Collections.singletonList("foo/bar:latest"), buildConfiguration.getCacheFrom());
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
    assertThat(result.getUser(), equalTo("super-user"));
    assertThat(result.getAssembly().getName(), equalTo("1337"));
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
    assertThat(result, notNullValue());
    assertThat(result.getContextDirRaw(), is("context"));
    assertThat(result.getContextDir(), is(new File("context")));
    assertThat(result.getDockerFileRaw(), is("Dockerfile.jvm"));
    assertThat(result.getDockerArchiveRaw(), is("docker-archive.tar"));
    assertThat(result.getFilter(), is("@"));
    assertThat(result.getFrom(), is("jkube-images/image:1337"));
    assertThat(result.getFromExt().values(), hasSize(1));
    assertThat(result.getFromExt(), hasEntry("name", "jkube-images/image:ext"));
    assertThat(result.getMaintainer(), is("A-Team"));
    assertThat(result.getPorts(), is(Collections.singletonList("8080")));
    assertThat(result.getShell().getShell(), is("java -version"));
    assertThat(result.getImagePullPolicy(), is("Always"));
    assertThat(result.getRunCmds(), contains("ls -la", "sleep 1", "echo done"));
    assertThat(result.getCleanup(), is("none"));
    assertThat(result.cleanupMode(), is(CleanupMode.NONE));
    assertThat(result.getNocache(), is(true));
    assertThat(result.getOptimise(), is(false));
    assertThat(result.getVolumes(), contains("volume 1"));
    assertThat(result.getTags(), contains("latest", "1337"));
    assertThat(result.getEnv().values(), hasSize(1));
    assertThat(result.getEnv(), hasEntry("JAVA_OPTS", "-Xmx1337m"));
    assertThat(result.getLabels(), hasEntry("label-1", "label"));
    assertThat(result.getArgs(), hasEntry("CODE_VERSION", "latest"));
    assertThat(result.getEntryPoint().getExec(), contains("java -version"));
    assertThat(result.getWorkdir(), is("/tmp"));
    assertThat(result.getCmd().getExec(), contains("sh", "-c"));
    assertThat(result.getUser(), is("root"));
    assertThat(result.getHealthCheck(), notNullValue());
    assertThat(result.getAssembly(), notNullValue());
    assertThat(result.getSkip(), is(false));
    assertThat(result.getCompression(), is(gzip));
    assertThat(result.getBuildOptions(), hasEntry("NetworkMode", "bridge"));
  }
}
