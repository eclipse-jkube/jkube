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
package org.eclipse.jkube.kit.build.service.docker;

import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BuildServiceTest {
  private DockerAccess mockedDockerAccess;
  private BuildService buildService;
  private ImageConfiguration imageConfiguration;
  private ImagePullManager mockedImagePullManager;
  private JKubeConfiguration mockedJKubeConfiguration;
  private RegistryService mockedRegistryService;

  @BeforeEach
  void setUp() {
    mockedDockerAccess = mock(DockerAccess.class, RETURNS_DEEP_STUBS);
    ArchiveService mockedArchiveService = mock(ArchiveService.class, RETURNS_DEEP_STUBS);
    mockedRegistryService = mock(RegistryService.class, RETURNS_DEEP_STUBS);
    KitLogger mockedLog = mock(KitLogger.SilentLogger.class, RETURNS_DEEP_STUBS);
    mockedImagePullManager = mock(ImagePullManager.class, RETURNS_DEEP_STUBS);
    mockedJKubeConfiguration = mock(JKubeConfiguration.class, RETURNS_DEEP_STUBS);
    QueryService mockedQueryService = new QueryService(mockedDockerAccess);
    buildService = new BuildService(mockedDockerAccess, mockedQueryService, mockedRegistryService, mockedArchiveService, mockedLog);
    imageConfiguration = ImageConfiguration.builder()
        .name("image-name")
        .build(BuildConfiguration.builder()
            .from("from")
            .tags(Collections.singletonList("latest"))
            .build()
        ).build();
  }

  @Test
  void buildImage_whenValidImageConfigurationProvidedAndDockerDaemonReturnsValidId_shouldBuildImage() throws IOException {
    // Given
    when(mockedDockerAccess.getImageId("image-name")).thenReturn("c8003cb6f5db");

    // When
    buildService.buildImage(imageConfiguration, mockedImagePullManager, mockedJKubeConfiguration);

    // Then
    verify(mockedDockerAccess, times(1))
        .buildImage(eq("image-name"), any(), any());
  }

  @Test
  void buildImage_whenValidImageConfigurationProvidedAndDockerDaemonReturnsNull_shouldBuildImage() throws IOException {
    // Given
    when(mockedDockerAccess.getImageId("image-name")).thenReturn(null);
    // When & Then
    assertThatIllegalStateException()
            .isThrownBy(() -> buildService.buildImage(imageConfiguration, mockedImagePullManager, mockedJKubeConfiguration))
            .withMessage("Failure in building image, unable to find image built with name image-name");
  }

  @Test
  void tagImage_whenValidImageConfigurationProvided_shouldTagImage() throws DockerAccessException {
    // When
    buildService.tagImage("image-name", imageConfiguration);

    // Then
    verify(mockedDockerAccess, times(1))
        .tag("image-name", "image-name:latest", true);
  }

  @Test
  void mergeBuildArgs_whenBuildArgsFromImageConfigAndFromProjectProperties_shouldMergeBuildArgs() {
    // Given
    Properties props = new Properties();
    props.setProperty("docker.buildArg.VERSION", "latest");
    props.setProperty("docker.buildArg.FULL_IMAGE", "busybox:latest");
    when(mockedJKubeConfiguration.getProject().getProperties()).thenReturn(props);

    Map<String, String> imgConfigBuildArg = new HashMap<>();
    imgConfigBuildArg.put("REPO_1", "docker.io/library");
    imgConfigBuildArg.put("IMAGE-1", "openjdk");
    imageConfiguration = ImageConfiguration.builder()
        .name("image-name")
        .build(BuildConfiguration.builder()
            .args(imgConfigBuildArg)
            .build())
        .build();

    // When
    Map<String, String> mergedBuildArgs = BuildService.mergeBuildArgs(imageConfiguration, mockedJKubeConfiguration);

    // Then
    assertThat(mergedBuildArgs)
        .containsEntry("VERSION", "latest")
        .containsEntry("FULL_IMAGE", "busybox:latest")
        .containsEntry("REPO_1", "docker.io/library")
        .containsEntry("IMAGE-1", "openjdk");
  }

  @Nested
  @DisplayName("mergeBuildArgs with BuildArgs")
  class MergeBuildArgs {
    @Test
    void fromAllSourcesWithDifferentKeys_shouldMergeBuildArgs() {
      // Given
      givenBuildArgsFromImageConfiguration("VERSION", "latest");
      givenBuildArgsFromSystemProperties("docker.buildArg.IMAGE-1", "openjdk");
      givenBuildArgsFromProjectProperties("docker.buildArg.REPO_1", "docker.io/library");
      givenBuildArgsFromJKubeConfiguration("FULL_IMAGE", "busybox:latest");

      // When
      Map<String, String> mergedBuildArgs = BuildService.mergeBuildArgs(imageConfiguration, mockedJKubeConfiguration);

      // Then
      assertThat(mergedBuildArgs)
          .containsEntry("VERSION", "latest")
          .containsEntry("FULL_IMAGE", "busybox:latest")
          .containsEntry("REPO_1", "docker.io/library")
          .containsEntry("IMAGE-1", "openjdk");
    }

    @Test
    void fromBuildConfigurationAndSystemPropertiesWithSameKey_shouldNotMergeBuildArgs() {
      // Given
      givenBuildArgsFromImageConfiguration("VERSION", "latest");
      givenBuildArgsFromSystemProperties("docker.buildArg.VERSION", "1.0.0");

      // When & Then
      assertThatIllegalArgumentException()
          .isThrownBy(() -> BuildService.mergeBuildArgs(imageConfiguration, mockedJKubeConfiguration))
          .withMessage("Multiple entries with same key: VERSION=latest and VERSION=1.0.0");
    }

    @Test
    void fromBuildConfigurationAndProjectPropertiesWithSameKey_shouldNotMergeBuildArgs() {
      // Given
      givenBuildArgsFromImageConfiguration("VERSION", "latest");
      givenBuildArgsFromProjectProperties("docker.buildArg.VERSION", "1.0.0");

      // When & Then
      assertThatIllegalArgumentException()
          .isThrownBy(() -> BuildService.mergeBuildArgs(imageConfiguration, mockedJKubeConfiguration))
          .withMessage("Multiple entries with same key: VERSION=latest and VERSION=1.0.0");
    }

    @Test
    void fromBuildConfigurationAndJKubeConfigurationWithSameKey_shouldNotMergeBuildArgs() {
      // Given
      givenBuildArgsFromImageConfiguration("VERSION", "latest");
      givenBuildArgsFromJKubeConfiguration("VERSION", "1.0.0");

      // When & Then
      assertThatIllegalArgumentException()
          .isThrownBy(() -> BuildService.mergeBuildArgs(imageConfiguration, mockedJKubeConfiguration))
          .withMessage("Multiple entries with same key: VERSION=latest and VERSION=1.0.0");
    }

    private void givenBuildArgsFromImageConfiguration(String key, String value) {
      imageConfiguration = ImageConfiguration.builder()
          .name("image-name")
          .build(BuildConfiguration.builder()
              .args(
                  Collections.singletonMap(key, value))
              .build())
          .build();
    }

    private void givenBuildArgsFromSystemProperties(String key, String value) {
      System.setProperty(key, value);
    }

    private void givenBuildArgsFromProjectProperties(String key, String value) {
      Properties props = new Properties();
      props.setProperty(key, value);
      when(mockedJKubeConfiguration.getProject().getProperties())
          .thenReturn(props);
    }

    private void givenBuildArgsFromJKubeConfiguration(String key, String value) {
      when(mockedJKubeConfiguration.getBuildArgs())
          .thenReturn(Collections.singletonMap(key, value));
    }

    @AfterEach
    void clearSystemPropertiesUsedInTests() {
      System.clearProperty("docker.buildArg.IMAGE-1");
      System.clearProperty("docker.buildArg.VERSION");
    }
  }

  @Test
  void buildImage_whenMultiStageDockerfileWithBuildArgs_shouldPrepullImages() throws IOException {
    // Given
    when(mockedDockerAccess.getImageId("image-name")).thenReturn("c8003cb6f5db");
    when(mockedJKubeConfiguration.getSourceDirectory()).thenReturn(tempDir.getAbsolutePath());
    when(mockedJKubeConfiguration.getProject().getBaseDirectory()).thenReturn(tempDir);
    File multistageDockerfile = copyToTempDir("Dockerfile_multi_stage_with_args_no_default");
    imageConfiguration = ImageConfiguration.builder()
        .name("image-name")
        .build(BuildConfiguration.builder()
            .dockerFile(multistageDockerfile.getPath())
            .dockerFileFile(multistageDockerfile)
            .build())
        .build();

    Properties props = new Properties();
    props.setProperty("docker.buildArg.VERSION", "latest");
    props.setProperty("docker.buildArg.FULL_IMAGE", "busybox:latest");
    props.setProperty("docker.buildArg.REPO_1", "docker.io/library");
    props.setProperty("docker.buildArg.IMAGE-1", "openjdk");
    when(mockedJKubeConfiguration.getProject().getProperties()).thenReturn(props);

    // When
    buildService.buildImage(imageConfiguration, mockedImagePullManager, mockedJKubeConfiguration);

    // Then
    verify(mockedRegistryService, times(1)).pullImageWithPolicy(eq("fabric8/s2i-java:latest"), any(), any(), any());
    verify(mockedRegistryService, times(1)).pullImageWithPolicy(eq("busybox:latest"), any(), any(), any());
    verify(mockedRegistryService, times(1)).pullImageWithPolicy(eq("docker.io/library/openjdk:latest"), any(), any(),
        any());
  }

  @TempDir
  private File tempDir;

  private File copyToTempDir(String resource) throws IOException {
    File ret = new File(tempDir, "Dockerfile");
    try (OutputStream os = Files.newOutputStream(ret.toPath())) {
      FileUtils.copyFile(new File(getClass().getResource(resource).getPath()), os);
    }
    return ret;
  }
}
