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
package org.eclipse.jkube.springboot.generator;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.GeneratorMode;
import org.eclipse.jkube.generator.javaexec.FatJarDetector;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class SpringBootGeneratorIntegrationTest {
  private File targetDir;
  private Properties properties;
  @TempDir
  Path temporaryFolder;

  private GeneratorContext context;

  @BeforeEach
  void setUp() throws IOException {
    properties = new Properties();
    targetDir = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    JavaProject javaProject = JavaProject.builder()
        .baseDirectory(temporaryFolder.toFile())
        .buildDirectory(targetDir.getAbsoluteFile())
        .buildPackageDirectory(targetDir.getAbsoluteFile())
        .outputDirectory(targetDir)
        .properties(properties)
        .version("1.0.0")
        .dependency(Dependency.builder()
            .groupId("org.springframework.boot")
            .artifactId("spring-boot-web")
            .version("2.7.2")
            .build())
        .plugin(Plugin.builder()
            .groupId("org.springframework.boot")
            .artifactId("spring-boot-maven-plugin")
            .version("2.7.2")
            .build())
        .buildFinalName("sample")
        .build();
    context = GeneratorContext.builder()
        .logger(new KitLogger.SilentLogger())
        .project(javaProject)
        .build();
  }

  @Test
  @DisplayName("customize, with standard packaging, has standard image name")
  void customize_withStandardPackaging_thenImageNameContainsGroupArtifactAndLatestTag() {
    // Given
    withCustomMainClass();
    SpringBootGenerator springBootGenerator = new SpringBootGenerator(context);
    List<ImageConfiguration> images = new ArrayList<>();

    // When
    images = springBootGenerator.customize(images, false);

    // Then
    assertThat(images)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .hasFieldOrPropertyWithValue("name", "%g/%a:%l");
  }

  @Test
  @DisplayName("customize, with standard packaging, has 'spring-boot' image alias")
  void customize_withStandardPackaging_thenImageAliasSpringBoot() {
    // Given
    withCustomMainClass();
    SpringBootGenerator springBootGenerator = new SpringBootGenerator(context);
    List<ImageConfiguration> images = new ArrayList<>();

    // When
    images = springBootGenerator.customize(images, false);

    // Then
    assertThat(images)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .hasFieldOrPropertyWithValue("alias", "spring-boot");
  }

  @Test
  @DisplayName("customize, with standard packaging, has image from based on standard Java Exec generator image")
  void customize_withStandardPackaging_hasFrom() {
    // Given
    withCustomMainClass();
    SpringBootGenerator springBootGenerator = new SpringBootGenerator(context);
    List<ImageConfiguration> images = new ArrayList<>();

    // When
    images = springBootGenerator.customize(images, false);

    // Then
    assertThat(images)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getFrom)
        .asString()
        .startsWith("quay.io/jkube/jkube-java");
  }

  @Test
  @DisplayName("customize, with standard packaging, has '8080' web port")
  void customize_withStandardPackaging_thenImageHasDefaultWebPort() {
    // Given
    withCustomMainClass();
    SpringBootGenerator springBootGenerator = new SpringBootGenerator(context);
    List<ImageConfiguration> images = new ArrayList<>();

    // When
    images = springBootGenerator.customize(images, false);

    // Then
    assertThat(images)
        .singleElement()
        .extracting("buildConfiguration.ports").asList()
        .contains("8080");
  }

  @Test
  @DisplayName("customize, with standard packaging, has Jolokia port")
  void customize_withStandardPackaging_hasJolokiaPort() {
    // When
    final List<ImageConfiguration> result = new SpringBootGenerator(context).customize(new ArrayList<>(), true);
    // Then
    assertThat(result).singleElement()
        .extracting("buildConfiguration.ports").asList()
        .contains("8778");
  }

  @Test
  @DisplayName("customize, with standard packaging, has Prometheus port")
  void customize_withStandardPackaging_hasPrometheusPort() {
    // When
    final List<ImageConfiguration> result = new SpringBootGenerator(context).customize(new ArrayList<>(), true);
    // Then
    assertThat(result).singleElement()
        .extracting("buildConfiguration.ports").asList()
        .contains("9779");
  }

  @Test
  @DisplayName("customize, in Kubernetes and jar artifact, should create assembly")
  void customize_inKubernetesAndJarArtifact_shouldCreateAssembly() throws IOException {
    try (MockedConstruction<FatJarDetector> ignore = mockConstruction(FatJarDetector.class, (mock, ctx) -> {
      FatJarDetector.Result fatJarDetectorResult = mock(FatJarDetector.Result.class);
      when(mock.scan()).thenReturn(fatJarDetectorResult);
      when(fatJarDetectorResult.getArchiveFile()).thenReturn(targetDir.toPath().resolve("sample.jar").toFile());
    })) {
      // Given
      Files.createFile(targetDir.toPath().resolve("sample.jar"));

      // When
      final List<ImageConfiguration> resultImages = new SpringBootGenerator(context).customize(new ArrayList<>(), false);

      // Then
      assertThat(resultImages)
          .isNotNull()
          .singleElement()
          .extracting(ImageConfiguration::getBuild)
          .extracting(BuildConfiguration::getAssembly)
          .hasFieldOrPropertyWithValue("targetDir", "/deployments")
          .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", true)
          .extracting(AssemblyConfiguration::getLayers)
          .asList().hasSize(1)
          .satisfies(layers -> assertThat(layers).element(0).asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
              .extracting(Assembly::getFileSets)
              .asList().element(2)
              .hasFieldOrPropertyWithValue("outputDirectory", new File("."))
              .extracting("includes").asList()
              .containsExactly("sample.jar"));
    }
  }

  @Test
  @DisplayName("customize, with standard packaging, has java environment variables")
  void customize_withStandardPackaging_thenImageHasJavaMainClassAndJavaAppDirEnvVars() {
    // Given
    withCustomMainClass();
    SpringBootGenerator springBootGenerator = new SpringBootGenerator(context);
    List<ImageConfiguration> images = new ArrayList<>();

    // When
    images = springBootGenerator.customize(images, false);

    // Then
    assertThat(images)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getEnv)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("JAVA_MAIN_CLASS", "org.example.Foo")
        .containsEntry("JAVA_APP_DIR", "/deployments");
  }

  @Test
  @DisplayName("customize, with custom port in application.properties, has overridden web port in image")
  void customize_whenApplicationPortOverridden_shouldUseOverriddenWebPort() {
    // Given
    withCustomMainClass();
    context = context.toBuilder()
        .project(context.getProject().toBuilder()
            .compileClassPathElement(Objects.requireNonNull(getClass().getResource("/port-override-application-properties")).getPath())
            .build())
        .build();
    SpringBootGenerator springBootGenerator = new SpringBootGenerator(context);
    List<ImageConfiguration> images = new ArrayList<>();

    // When
    images = springBootGenerator.customize(images, false);

    // Then
    assertThat(images)
        .singleElement()
        .extracting("buildConfiguration.ports").asList()
        .contains("8081");
  }

  @Test
  @DisplayName("customize, when generator mode WATCH, then add Spring Boot Devtools environment variable to image")
  void customize_whenGeneratorModeWatch_shouldAddSpringBootDevtoolsSecretEnvVar() {
    // Given
    withCustomMainClass();
    context = context.toBuilder()
        .generatorMode(GeneratorMode.WATCH)
        .project(context.getProject().toBuilder()
            .compileClassPathElement(Objects.requireNonNull(getClass().getResource("/devtools-application-properties")).getPath())
            .build())
        .build();
    SpringBootGenerator springBootGenerator = new SpringBootGenerator(context);
    List<ImageConfiguration> images = new ArrayList<>();

    // When
    images = springBootGenerator.customize(images, false);

    // Then
    assertThat(images)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getEnv)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("SPRING_DEVTOOLS_REMOTE_SECRET", "some-secret");
  }

  @Test
  @DisplayName("customize, when color configuration provided, enables ANSI color output")
  void customize_withColorConfiguration_shouldAddAnsiEnabledPropertyToJavaOptions() {
    // Given
    properties.put("jkube.generator.spring-boot.color", "always");
    withCustomMainClass();
    List<ImageConfiguration> images = new ArrayList<>();
    SpringBootGenerator springBootGenerator = new SpringBootGenerator(context);

    // When
    images = springBootGenerator.customize(images, false);

    // Then
    assertThat(images)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getEnv)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("JAVA_OPTIONS", "-Dspring.output.ansi.enabled=always");
  }

  private void withCustomMainClass() {
    properties.put("jkube.generator.spring-boot.mainClass", "org.example.Foo");
  }
}
