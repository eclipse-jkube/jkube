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
package org.eclipse.jkube.kit.enricher.api;

import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.common.util.ClassUtil;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.common.util.ProjectClassLoaders;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class JKubeEnricherContextTest {

  private JavaProject javaProject;

  @BeforeEach
  void setUp() {
    javaProject = JavaProject.builder()
            .groupId("org.eclipse.jkube").artifactId("test-project").version("0.0.1")
            .build();
  }

  @Test
  void builder_whenInvoked_shouldConstructJKubeEnricherContext() {
    // Given + When
    JKubeEnricherContext jKubeEnricherContext = JKubeEnricherContext.builder()
        .jKubeBuildStrategy(JKubeBuildStrategy.jib)
        .processingInstruction("foo", "bar")
        .image(ImageConfiguration.builder().name("foo:latest").build())
        .project(JavaProject.builder().groupId("org.eclipse.jkube").artifactId("test-project").build())
        .resources(ResourceConfig.builder().imagePullPolicy("Never").build())
        .setting(RegistryServerConfiguration.builder().id("test").username("xyz").password("secret").build())
        .build();

    // Then
    assertThat(jKubeEnricherContext)
        .hasFieldOrPropertyWithValue("configuration.jKubeBuildStrategy", JKubeBuildStrategy.jib)
        .hasFieldOrPropertyWithValue("settings", Collections.singletonList(RegistryServerConfiguration.builder().id("test").username("xyz").password("secret").build()))
        .hasFieldOrPropertyWithValue("processingInstructions", Collections.singletonMap("foo", "bar"))
        .hasFieldOrPropertyWithValue("images", Collections.singletonList(ImageConfiguration.builder().name("foo:latest").build()))
        .hasFieldOrPropertyWithValue("project", JavaProject.builder().groupId("org.eclipse.jkube").artifactId("test-project").build())
        .hasFieldOrPropertyWithValue("resources", ResourceConfig.builder().imagePullPolicy("Never").build());
  }

  @Test
  void getGav_whenInvoked_shouldReturnExpectedGroupArtifactVersion() {
    // Given + When
    JKubeEnricherContext jKubeEnricherContext = JKubeEnricherContext.builder()
        .project(javaProject)
        .build();

    // Then
    assertThat(jKubeEnricherContext.getGav())
        .hasFieldOrPropertyWithValue("groupId", "org.eclipse.jkube")
        .hasFieldOrPropertyWithValue("artifactId", "test-project")
        .hasFieldOrPropertyWithValue("version", "0.0.1");
  }

  @Test
  void getDockerJsonConfigString_whenNoServerPresent_shouldReturnBlankString() {
    // Given
    JKubeEnricherContext jKubeEnricherContext = JKubeEnricherContext.builder()
        .project(javaProject)
        .build();

    // When
    String dockerConfigJson = jKubeEnricherContext.getDockerJsonConfigString(Collections.emptyList(), "server1");

    // Then
    assertThat(dockerConfigJson).isEmpty();
  }

  @Test
  void getDockerJsonConfigString_whenServerPresent_shouldReturnDockerJsonString() {
    // Given
    JKubeEnricherContext jKubeEnricherContext = JKubeEnricherContext.builder()
        .project(javaProject)
        .build();

    // When
    String dockerConfigJson = jKubeEnricherContext.getDockerJsonConfigString(Collections.singletonList(RegistryServerConfiguration.builder()
            .id("server1")
            .username("user1")
            .password("secret")
            .configuration(Collections.singletonMap("email", "info@example.com"))
            .build())
        , "server1");

    // Then
    assertThat(dockerConfigJson).isEqualTo("{\"server1\":{\"username\":\"user1\",\"password\":\"secret\",\"email\":\"info@example.com\"}}");
  }

  @Test
  void getProperty_whenPropertyPresent_shouldReturnValue() {
    // Given
    Properties properties = new Properties();
    properties.put("key1", "value1");
    JKubeEnricherContext jKubeEnricherContext = JKubeEnricherContext.builder()
        .project(javaProject.toBuilder().properties(properties).build())
        .build();

    // When
    String value = jKubeEnricherContext.getProperty("key1");

    // Then
    assertThat(value).isEqualTo("value1");
  }

  @Test
  void getProperty_whenPropertyAbsent_shouldReturnNull() {
    // Given
    Properties properties = new Properties();
    JKubeEnricherContext jKubeEnricherContext = JKubeEnricherContext.builder()
        .project(javaProject.toBuilder().properties(properties).build())
        .build();

    // When
    String value = jKubeEnricherContext.getProperty("key1");

    // Then
    assertThat(value).isNull();
  }

  @Test
  void getBuildStrategy_whenBuildStrategyPresent_shouldReturnBuildStrategy() {
    // Given
    JKubeEnricherContext jKubeEnricherContext = JKubeEnricherContext.builder()
        .project(javaProject)
        .jKubeBuildStrategy(JKubeBuildStrategy.docker)
        .build();

    // When
    JKubeBuildStrategy jKubeBuildStrategy = jKubeEnricherContext.getConfiguration().getJKubeBuildStrategy();

    // Then
    assertThat(jKubeBuildStrategy).isEqualTo(JKubeBuildStrategy.docker);
  }

  @Test
  void getDependencies_whenTransitiveTrue_shouldGetTransitiveDeps() {
    // Given
    List<Dependency> deps = new ArrayList<>(javaProject.getDependencies());
    deps.add(Dependency.builder()
            .groupId("org.eclipse.jkube").artifactId("test-project").version("0.0.1").build());

    javaProject.setDependenciesWithTransitive(deps);
    JKubeEnricherContext jKubeEnricherContext = JKubeEnricherContext.builder()
        .project(javaProject)
        .build();

    // When
    List<Dependency> jKubeEnricherContextDependencies = jKubeEnricherContext.getDependencies(true);


    // Then
    assertThat(jKubeEnricherContextDependencies).hasSize(1);
  }

  @Test
  void getDependencies_whenTransitiveFalse_shouldGetDeps() {
    // Given
    List<Dependency> deps = new ArrayList<>(javaProject.getDependencies());
    deps.add(Dependency.builder()
            .groupId("org.eclipse.jkube").artifactId("test-project").version("0.0.1").build());

    javaProject.setDependenciesWithTransitive(deps);
    JKubeEnricherContext jKubeEnricherContext = JKubeEnricherContext.builder()
        .project(javaProject)
        .build();

    // When
    List<Dependency> jKubeEnricherContextDependencies = jKubeEnricherContext.getDependencies(false);

    // Then
    assertThat(jKubeEnricherContextDependencies).isEmpty();
  }

  @Test
  void hasPlugin_withNullGroup_shouldSearchPluginWithArtifactId() {
    try (MockedStatic<JKubeProjectUtil> jKubeProjectUtilMockedStatic = mockStatic(JKubeProjectUtil.class)) {
      // Given
      JKubeEnricherContext jKubeEnricherContext = JKubeEnricherContext.builder()
          .project(javaProject)
          .build();

      // When
      jKubeEnricherContext.hasPlugin(null, "test-plugin");

      // Then
      jKubeProjectUtilMockedStatic.verify(() -> JKubeProjectUtil.getPlugin(javaProject, "test-plugin"));
    }
  }

  @Test
  void hasPlugin_withGroup_shouldSearchPluginWithArtifactId() {
    try (MockedStatic<JKubeProjectUtil> jKubeProjectUtilMockedStatic = mockStatic(JKubeProjectUtil.class)) {
      // Given
      JKubeEnricherContext jKubeEnricherContext = JKubeEnricherContext.builder()
          .project(javaProject)
          .build();

      // When
      jKubeEnricherContext.hasPlugin("org.test", "test-plugin");

      // Then
      jKubeProjectUtilMockedStatic.verify(() -> JKubeProjectUtil.hasPlugin(javaProject, "org.test", "test-plugin"));
    }
  }

  @Test
  void getProjectClassLoaders_whenInvoked_shouldCreateClassLoaderFromCompileClasspathElements() {
    try (MockedStatic<ClassUtil> classUtilMockedStatic = mockStatic(ClassUtil.class)) {
      // Given
      File targetDir = new File("target");
      JKubeEnricherContext jKubeEnricherContext = JKubeEnricherContext.builder()
          .project(javaProject.toBuilder()
                  .compileClassPathElements(Collections.singletonList("/test/foo.jar"))
                  .outputDirectory(targetDir)
                  .build())
          .build();

      // When
      ProjectClassLoaders projectClassLoaders = jKubeEnricherContext.getProjectClassLoaders();

      // Then
      assertThat(projectClassLoaders).isNotNull();
      classUtilMockedStatic.verify(() -> ClassUtil.createClassLoader(Collections.singletonList("/test/foo.jar"), targetDir.getAbsolutePath()));
    }
  }

}
