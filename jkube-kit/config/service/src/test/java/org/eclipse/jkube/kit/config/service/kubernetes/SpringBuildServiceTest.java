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

import org.eclipse.jkube.kit.build.service.docker.DockerServiceHub;
import org.eclipse.jkube.kit.build.service.docker.RegistryService;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedConstruction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringBuildServiceTest {
  private SpringBuildService springBuildService;
  private JKubeServiceHub jKubeServiceHub;
  private DockerServiceHub dockerServiceHub;
  private RegistryService registryService;
  private KitLogger kitLogger;

  @BeforeEach
  void setup() {
    kitLogger = new KitLogger.SilentLogger();
    dockerServiceHub = mock(DockerServiceHub.class);
    registryService = mock(RegistryService.class);
    when(dockerServiceHub.getRegistryService()).thenReturn(registryService);
  }

  private static Stream<Arguments> isApplicableWithRuntimeModeAndBuildStrategyParams() {
    return Stream.of(
        arguments(RuntimeMode.OPENSHIFT, JKubeBuildStrategy.spring),
        arguments(RuntimeMode.OPENSHIFT, JKubeBuildStrategy.jib),
        arguments(RuntimeMode.OPENSHIFT, JKubeBuildStrategy.s2i),
        arguments(RuntimeMode.OPENSHIFT, null),
        arguments(RuntimeMode.KUBERNETES, JKubeBuildStrategy.jib),
        arguments(RuntimeMode.KUBERNETES, JKubeBuildStrategy.docker),
        arguments(RuntimeMode.KUBERNETES, JKubeBuildStrategy.spring),
        arguments(RuntimeMode.KUBERNETES, null)
    );
  }

  @ParameterizedTest(name = "given runtime mode = {0}, build strategy = {1}, then return {2}")
  @MethodSource("isApplicableWithRuntimeModeAndBuildStrategyParams")
  void isApplicable_whenRuntimeModeAndBuildStrategyProvidedButNoSpringBoot_thenReturnFalse(RuntimeMode runtimeMode, JKubeBuildStrategy buildStrategy) {
    // Given
    springBuildService = createNewSpringBuildService(runtimeMode, buildStrategy);

    // When
    boolean result = springBuildService.isApplicable();

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("runtimeMode Kubernetes and jkubeBuildStrategy spring and SpringBoot3 project should return true")
  void isApplicable_whenRuntimeModeKubernetesAndBuildStrategySpringAndSpringBoot3Project_thenReturnTrue() {
    // Given
    springBuildService = createNewSpringBuildServiceWithSpringBootVersion("3.1.2");

    // When
    boolean result = springBuildService.isApplicable();

    // Then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("runtimeMode Kubernetes and jkubeBuildStrategy spring and SpringBoot 2.2.9 project should return false")
  void isApplicable_whenRuntimeModeKubernetesAndBuildStrategySpringAndSpringBoot2_2_9Project_thenReturnFalse() {
    // Given
    springBuildService = createNewSpringBuildServiceWithSpringBootVersion("2.2.9.RELEASE");

    // When
    boolean result = springBuildService.isApplicable();

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("pushSingleImage in spring buildStrategy should delegate to Docker Service")
  void pushSingleImage_whenInvoked_shouldDelegateToDockerService() throws JKubeServiceException, IOException {
    // Given
    springBuildService = createNewSpringBuildServiceWithSpringBootVersion("2.2.9.RELEASE");
    RegistryConfig registryConfig = createRegistryConfig();
    ImageConfiguration imageConfiguration = createNewImageConfiguration();

    // When
    springBuildService.pushSingleImage(imageConfiguration, 0, registryConfig, true);

    // Then
    verify(registryService).pushImage(imageConfiguration, 0, registryConfig, true);
  }

  @Test
  @DisplayName("pushSingleImage throws exception when push fails")
  void pushSingleImage_whenPushFails_thenThrowException() throws IOException {
    // Given
    springBuildService = createNewSpringBuildServiceWithSpringBootVersion("2.2.9.RELEASE");
    RegistryConfig registryConfig = createRegistryConfig();
    ImageConfiguration imageConfiguration = createNewImageConfiguration();
    doThrow(new IOException("I/O error"))
        .when(registryService).pushImage(imageConfiguration, 0, registryConfig, true);

    // When + Then
    assertThatExceptionOfType(JKubeServiceException.class)
        .isThrownBy(() -> springBuildService.pushSingleImage(imageConfiguration, 0, registryConfig, true))
        .withMessage("Error while trying to push the image: I/O error");
  }

  @Nested
  @DisplayName("maven-goal")
  class MavenGoal {
    @Test
    @DisplayName("maven goal exec args")
    void getArgs(@TempDir File temporaryFolder) {
      // Given
      String imageName = "sample/foo:latest";
      SpringBuildService.MavenSpringBootBuildImageTaskCommand externalCommand = new SpringBuildService.MavenSpringBootBuildImageTaskCommand(kitLogger, temporaryFolder, imageName, Collections.emptyList());

      // When
      assertThat(externalCommand.getArgs())
          .containsExactly("mvn", "org.springframework.boot:spring-boot-maven-plugin:build-image", "-f", temporaryFolder.getAbsolutePath(), "-Dspring-boot.build-image.imageName=" + imageName);
    }

    @Test
    @DisplayName("maven goal exec args with active profiles")
    void getArgsWithActiveProfiles(@TempDir File temporaryFolder) {
      // Given
      String imageName = "sample/foo:latest";
      SpringBuildService.MavenSpringBootBuildImageTaskCommand externalCommand = new SpringBuildService.MavenSpringBootBuildImageTaskCommand(kitLogger, temporaryFolder, imageName, Collections.singletonList("-Pnative"));

      // When
      assertThat(externalCommand.getArgs())
          .containsExactly("mvn", "org.springframework.boot:spring-boot-maven-plugin:build-image", "-f", temporaryFolder.getAbsolutePath(), "-Dspring-boot.build-image.imageName=" + imageName, "-Pnative");
    }

    @Test
    @DisplayName("maven wrapper goal exec args")
    void getArgsWithMavenWrapper(@TempDir File temporaryFolder) throws IOException {
      // Given
      String imageName = "sample/foo:latest";
      File gradleWrapper = new File(temporaryFolder, "mvnw");
      Files.createFile(gradleWrapper.toPath());
      SpringBuildService.MavenSpringBootBuildImageTaskCommand externalCommand = new SpringBuildService.MavenSpringBootBuildImageTaskCommand(kitLogger, temporaryFolder, imageName, Collections.emptyList());

      // When
      assertThat(externalCommand.getArgs())
          .contains("./mvnw", "org.springframework.boot:spring-boot-maven-plugin:build-image", "-Dspring-boot.build-image.imageName=" + imageName);
    }

    @Test
    @DisplayName("buildSingleImage should run spring-boot-maven-plugin:build-image if maven plugin is detected")
    void buildSingleImage_whenSpringBootMavenPluginPresent_thenRunMavenGoal() throws IOException {
      try (MockedConstruction<SpringBuildService.MavenSpringBootBuildImageTaskCommand> externalCommandMockedConstruction = mockConstruction(SpringBuildService.MavenSpringBootBuildImageTaskCommand.class)) {
        // Given
        springBuildService = createNewSpringBuildServiceWithMavenProjectAndSpringBoot();
        ImageConfiguration imageConfiguration = createNewImageConfiguration();

        // When
        springBuildService.buildSingleImage(imageConfiguration);

        // Then
        assertThat(externalCommandMockedConstruction.constructed()).hasSize(1);
        verify(externalCommandMockedConstruction.constructed().get(0)).execute();
      }
    }

    @Test
    @DisplayName("buildSingleImage should throw exception maven goal exec fails")
    void buildSingleImage_whenMavenGoalFailed_thenThrowException() throws IOException {
      try (MockedConstruction<SpringBuildService.MavenSpringBootBuildImageTaskCommand> externalCommandMockedConstruction = mockConstruction(SpringBuildService.MavenSpringBootBuildImageTaskCommand.class, (mock, ctx) -> {
        doThrow(new IOException("Failure")).when(mock).execute();
      })) {
        // Given
        springBuildService = createNewSpringBuildServiceWithMavenProjectAndSpringBoot();
        ImageConfiguration imageConfiguration = createNewImageConfiguration();

        // When + Then
        assertThatIllegalStateException()
            .isThrownBy(() -> springBuildService.buildSingleImage(imageConfiguration))
            .withMessage("Failure in executing Spring Boot MAVEN Plugin org.springframework.boot:spring-boot-maven-plugin:build-image");
        assertThat(externalCommandMockedConstruction.constructed()).hasSize(1);
        verify(externalCommandMockedConstruction.constructed().get(0)).execute();
      }
    }
  }

  @Nested
  @DisplayName("gradle-task")
  class GradleTask {
    @Test
    @DisplayName("gradle task exec args")
    void getArgs(@TempDir File temporaryFolder) {
      // Given
      String imageName = "sample/foo:latest";
      SpringBuildService.GradleSpringBootBuildImageTaskCommand externalCommand = new SpringBuildService.GradleSpringBootBuildImageTaskCommand(kitLogger, temporaryFolder, imageName, Collections.emptyList());

      // When
      assertThat(externalCommand.getArgs())
          .contains("gradle", "bootBuildImage", "--imageName=" + imageName);
    }

    @Test
    @DisplayName("gradle wrapper task exec args")
    void getArgsWithGradleWrapper(@TempDir File temporaryFolder) throws IOException {
      // Given
      String imageName = "sample/foo:latest";
      File gradleWrapper = new File(temporaryFolder, "gradlew");
      Files.createFile(gradleWrapper.toPath());
      SpringBuildService.GradleSpringBootBuildImageTaskCommand externalCommand = new SpringBuildService.GradleSpringBootBuildImageTaskCommand(kitLogger, temporaryFolder, imageName, Collections.emptyList());

      // When
      assertThat(externalCommand.getArgs())
          .contains("./gradlew", "bootBuildImage", "--imageName=" + imageName);
    }

    @Test
    @DisplayName("buildSingleImage should run bootBuildImage task if gradle plugin is detected")
    void buildSingleImage_whenSpringBootGradlePluginPresent_thenRunGradleTask() throws IOException {
      try (MockedConstruction<SpringBuildService.GradleSpringBootBuildImageTaskCommand> externalCommandMockedConstruction = mockConstruction(SpringBuildService.GradleSpringBootBuildImageTaskCommand.class)) {
        // Given
        springBuildService = createNewSpringBuildServiceWithGradleProjectAndSpringBoot();
        ImageConfiguration imageConfiguration = createNewImageConfiguration();

        // When
        springBuildService.buildSingleImage(imageConfiguration);

        // Then
        assertThat(externalCommandMockedConstruction.constructed()).hasSize(1);
        verify(externalCommandMockedConstruction.constructed().get(0)).execute();
      }
    }

    @Test
    @DisplayName("buildSingleImage should throw exception if gradle task exec command fails")
    void buildSingleImage_whenGradleTaskFailed_thenThrowException() throws IOException {
      try (MockedConstruction<SpringBuildService.GradleSpringBootBuildImageTaskCommand> externalCommandMockedConstruction = mockConstruction(SpringBuildService.GradleSpringBootBuildImageTaskCommand.class, (mock, ctx) -> {
        doThrow(new IOException("Failure")).when(mock).execute();
      })) {
        // Given
        springBuildService = createNewSpringBuildServiceWithGradleProjectAndSpringBoot();
        ImageConfiguration imageConfiguration = createNewImageConfiguration();

        // When + Then
        assertThatIllegalStateException()
            .isThrownBy(() -> springBuildService.buildSingleImage(imageConfiguration))
            .withMessage("Failure in executing Spring Boot Gradle Plugin bootBuildImage");
        assertThat(externalCommandMockedConstruction.constructed()).hasSize(1);
        verify(externalCommandMockedConstruction.constructed().get(0)).execute();
      }
    }
  }

  private SpringBuildService createNewSpringBuildServiceWithMavenProjectAndSpringBoot() {
    jKubeServiceHub = createJKubeServiceHubBuilder(RuntimeMode.KUBERNETES, JKubeBuildStrategy.spring, Collections.singletonList(Dependency.builder()
            .groupId("org.springframework.boot")
            .artifactId("spring-boot-web")
            .version("3.1.2")
            .build()),
        Collections.singletonList(Plugin.builder()
            .groupId("org.springframework.boot")
            .artifactId("spring-boot-maven-plugin")
            .version("3.1.2")
            .build()))
        .build();
    return new SpringBuildService(jKubeServiceHub);

  }

  private SpringBuildService createNewSpringBuildServiceWithGradleProjectAndSpringBoot() {
    jKubeServiceHub = createJKubeServiceHubBuilder(RuntimeMode.KUBERNETES, JKubeBuildStrategy.spring, Collections.singletonList(Dependency.builder()
            .groupId("org.springframework.boot")
            .artifactId("spring-boot-web")
            .version("3.1.2")
            .build()),
        Collections.singletonList(Plugin.builder()
            .groupId("org.springframework.boot")
            .artifactId("org.springframework.boot.gradle.plugin")
            .version("3.1.2")
            .build()))
        .build();
    return new SpringBuildService(jKubeServiceHub);
  }

  private SpringBuildService createNewSpringBuildServiceWithSpringBootVersion(String springBootVersion) {
    jKubeServiceHub = createJKubeServiceHubBuilder(RuntimeMode.KUBERNETES, JKubeBuildStrategy.spring, Collections.singletonList(Dependency.builder()
        .groupId("org.springframework.boot")
        .artifactId("spring-boot-web")
        .version(springBootVersion)
        .build()), Collections.emptyList()).build();
    return new SpringBuildService(jKubeServiceHub);
  }

  private RegistryConfig createRegistryConfig() {
    return RegistryConfig.builder()
        .registry("example.org")
        .build();
  }

  private ImageConfiguration createNewImageConfiguration() {
    return ImageConfiguration.builder()
        .name("example.com/foo/bar:latest")
        .build(BuildConfiguration.builder()
            .from("example.com/base:latest")
            .build())
        .build();
  }

  private SpringBuildService createNewSpringBuildService(RuntimeMode runtimeMode, JKubeBuildStrategy jKubeBuildStrategy) {
    jKubeServiceHub = createJKubeServiceHubBuilder(runtimeMode, jKubeBuildStrategy).build();
    return new SpringBuildService(jKubeServiceHub);
  }

  private JKubeServiceHub.JKubeServiceHubBuilder createJKubeServiceHubBuilder(RuntimeMode runtimeMode, JKubeBuildStrategy jKubeBuildStrategy) {
    return createJKubeServiceHubBuilder(runtimeMode, jKubeBuildStrategy, Collections.emptyList(), Collections.emptyList());
  }

  private JKubeServiceHub.JKubeServiceHubBuilder createJKubeServiceHubBuilder(RuntimeMode runtimeMode, JKubeBuildStrategy jKubeBuildStrategy, List<Dependency> dependencyList, List<Plugin> pluginList) {
    return JKubeServiceHub.builder()
        .log(kitLogger)
        .platformMode(runtimeMode)
        .configuration(JKubeConfiguration.builder()
            .project(JavaProject.builder()
                .groupId("org.eclipse.jkube")
                .artifactId("sample")
                .version("1.0.0")
                .dependencies(dependencyList)
                .plugins(pluginList)
                .properties(new Properties())
                .build())
            .build())
        .buildServiceConfig(BuildServiceConfig.builder()
            .jKubeBuildStrategy(jKubeBuildStrategy)
            .build())
        .dockerServiceHub(dockerServiceHub);
  }
}
