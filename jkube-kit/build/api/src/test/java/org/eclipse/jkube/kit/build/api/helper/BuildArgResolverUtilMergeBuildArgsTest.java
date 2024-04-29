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
package org.eclipse.jkube.kit.build.api.helper;

import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JKubeException;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.EnvUtil;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class BuildArgResolverUtilMergeBuildArgsTest {
  private ImageConfiguration imageConfiguration;
  private JKubeConfiguration jKubeConfiguration;
  private Properties projectProperties;
  private Map<String, String> buildArgFromPluginConfiguration;

  @BeforeEach
  void setUp() {
    projectProperties = new Properties();
    buildArgFromPluginConfiguration = new HashMap<>();
    jKubeConfiguration = JKubeConfiguration.builder()
        .project(JavaProject.builder()
            .properties(projectProperties)
            .build())
        .buildArgs(buildArgFromPluginConfiguration)
        .build();
    imageConfiguration = ImageConfiguration.builder()
        .name("image-name")
        .build(BuildConfiguration.builder()
            .build())
        .build();
  }

  @Test
  @DisplayName("build args in image config and project properties")
  void whenBuildArgsFromImageConfigAndFromProjectProperties_shouldMergeBuildArgs() {
    // Given
    projectProperties.setProperty("docker.buildArg.VERSION", "latest");
    projectProperties.setProperty("docker.buildArg.FULL_IMAGE", "busybox:latest");
    Map<String, String> buildArgImageConfiguration = new HashMap<>();
    buildArgImageConfiguration.put("REPO_1", "docker.io/library");
    buildArgImageConfiguration.put("IMAGE-1", "openjdk");
    imageConfiguration = imageConfiguration.toBuilder()
        .build(imageConfiguration.getBuild().toBuilder().args(buildArgImageConfiguration).build())
        .build();

    // When
    Map<String, String> mergedBuildArgs = BuildArgResolverUtil.mergeBuildArgsIncludingLocalDockerConfigProxySettings(imageConfiguration, jKubeConfiguration);

    // Then
    assertThat(mergedBuildArgs)
        .containsEntry("VERSION", "latest")
        .containsEntry("FULL_IMAGE", "busybox:latest")
        .containsEntry("REPO_1", "docker.io/library")
        .containsEntry("IMAGE-1", "openjdk");
  }

  @Test
  @DisplayName("build args in image config, project properties, system properties, plugin configuration")
  void fromAllSourcesWithDifferentKeys_shouldMergeBuildArgs() {
    // Given
    givenBuildArgsFromImageConfiguration("VERSION", "latest");
    System.setProperty("docker.buildArg.IMAGE-1", "openjdk");
    projectProperties.setProperty("docker.buildArg.REPO_1", "docker.io/library");
    givenBuildArgsFromJKubeConfiguration("FULL_IMAGE", "busybox:latest");

    // When
    Map<String, String> mergedBuildArgs = BuildArgResolverUtil.mergeBuildArgsIncludingLocalDockerConfigProxySettings(imageConfiguration, jKubeConfiguration);

    // Then
    assertThat(mergedBuildArgs)
        .containsEntry("VERSION", "latest")
        .containsEntry("FULL_IMAGE", "busybox:latest")
        .containsEntry("REPO_1", "docker.io/library")
        .containsEntry("IMAGE-1", "openjdk");
  }

  @Test
  @DisplayName("build args in image config and system properties with same key, should throw exception")
  void fromBuildConfigurationAndSystemPropertiesWithSameKey_shouldNotMergeBuildArgs() {
    // Given
    givenBuildArgsFromImageConfiguration("VERSION", "latest");
    System.setProperty("docker.buildArg.VERSION", "1.0.0");

    // When & Then
    assertThatExceptionOfType(JKubeException.class)
        .isThrownBy(() -> BuildArgResolverUtil.mergeBuildArgsIncludingLocalDockerConfigProxySettings(imageConfiguration, jKubeConfiguration))
        .withMessage("Multiple Build Args with the same key: VERSION=latest and VERSION=1.0.0");
  }

  @Test
  @DisplayName("build args in image config and project properties with same key, should throw exception")
  void fromBuildConfigurationAndProjectPropertiesWithSameKey_shouldNotMergeBuildArgs() {
    // Given
    givenBuildArgsFromImageConfiguration("VERSION", "latest");
    projectProperties.setProperty("docker.buildArg.VERSION", "1.0.0");

    // When & Then
    assertThatExceptionOfType(JKubeException.class)
        .isThrownBy(() -> BuildArgResolverUtil.mergeBuildArgsIncludingLocalDockerConfigProxySettings(imageConfiguration, jKubeConfiguration))
        .withMessage("Multiple Build Args with the same key: VERSION=latest and VERSION=1.0.0");
  }

  @Test
  @DisplayName("build args in image config and plugin config with same key, should throw exception")
  void fromBuildConfigurationAndJKubeConfigurationWithSameKey_shouldNotMergeBuildArgs() {
    // Given
    givenBuildArgsFromImageConfiguration("VERSION", "latest");
    givenBuildArgsFromJKubeConfiguration("VERSION", "1.0.0");

    // When & Then
    assertThatExceptionOfType(JKubeException.class)
        .isThrownBy(() -> BuildArgResolverUtil.mergeBuildArgsIncludingLocalDockerConfigProxySettings(imageConfiguration, jKubeConfiguration))
        .withMessage("Multiple Build Args with the same key: VERSION=latest and VERSION=1.0.0");
  }

  @Nested
  @DisplayName("local ~/.docker/config.json contains proxy settings")
  class LocalDockerConfigContainsProxySettings {
    @TempDir
    private File temporaryFolder;

    @BeforeEach
    void setUp() throws IOException {
      Path dockerConfig = temporaryFolder.toPath();
      final Map<String, String> env = Collections.singletonMap("DOCKER_CONFIG", dockerConfig.toFile().getAbsolutePath());
      EnvUtil.overrideEnvGetter(env::get);
      Files.write(dockerConfig.resolve("config.json"), ("{\"proxies\": {\"default\": {\n" +
          "     \"httpProxy\": \"http://proxy.example.com:3128\",\n" +
          "     \"httpsProxy\": \"https://proxy.example.com:3129\",\n" +
          "     \"noProxy\": \"*.test.example.com,.example.org,127.0.0.0/8\"\n" +
          "   }}}").getBytes());

    }

    @Test
    @DisplayName("mergeBuildArgsIncludingLocalDockerConfigProxySettings, should add proxy build args for docker build strategy")
    void shouldAddBuildArgsFromDockerConfigInDockerBuild() {
      // When
      final Map<String, String> mergedBuildArgs = BuildArgResolverUtil.mergeBuildArgsIncludingLocalDockerConfigProxySettings(imageConfiguration, jKubeConfiguration);
      // Then
      assertThat(mergedBuildArgs)
          .containsEntry("docker.buildArg.http_proxy", "http://proxy.example.com:3128")
          .containsEntry("docker.buildArg.https_proxy", "https://proxy.example.com:3129")
          .containsEntry("docker.buildArg.no_proxy", "*.test.example.com,.example.org,127.0.0.0/8");
    }

    @Test
    @DisplayName("mergeBuildArgsWithoutIncludingLocalDockerConfigProxySettings, should not add proxy build args for OpenShift build strategy")
    void shouldNotAddBuildArgsFromDockerConfig() {
      // When
      final Map<String, String> mergedBuildArgs = BuildArgResolverUtil.mergeBuildArgsWithoutLocalDockerConfigProxySettings(imageConfiguration, jKubeConfiguration);
      // Then
      assertThat(mergedBuildArgs)
          .doesNotContainEntry("docker.buildArg.http_proxy", "http://proxy.example.com:3128")
          .doesNotContainEntry("docker.buildArg.https_proxy", "https://proxy.example.com:3129")
          .doesNotContainEntry("docker.buildArg.no_proxy", "*.test.example.com,.example.org,127.0.0.0/8");
    }

    @AfterEach
    void tearDown() {
      EnvUtil.overrideEnvGetter(System::getenv);
    }
  }

  private void givenBuildArgsFromImageConfiguration(String key, String value) {
    imageConfiguration = imageConfiguration.toBuilder()
        .build(BuildConfiguration.builder()
            .args(
                Collections.singletonMap(key, value))
            .build())
        .build();
  }

  private void givenBuildArgsFromJKubeConfiguration(String key, String value) {
    buildArgFromPluginConfiguration.put(key, value);
  }

  @AfterEach
  void clearSystemPropertiesUsedInTests() {
    System.clearProperty("docker.buildArg.IMAGE-1");
    System.clearProperty("docker.buildArg.VERSION");
  }
}
