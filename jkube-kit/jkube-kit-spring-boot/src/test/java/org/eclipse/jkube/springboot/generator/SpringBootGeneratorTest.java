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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.GeneratorMode;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class SpringBootGeneratorTest {

  private GeneratorContext context;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    context = GeneratorContext.builder()
      .logger(new KitLogger.SilentLogger())
      .project(JavaProject.builder()
        .outputDirectory(Files.createDirectory(temporaryFolder.resolve("target")).toFile())
        .version("1.0.0")
        .build())
      .build();
  }

  @Test
  @DisplayName("isApplicable, when valid ImageConfiguration present, then returns false")
  void isApplicable_whenImageConfigurationPresent_thenReturnFalse() {
    // Given
    final List<ImageConfiguration> configs = Collections.singletonList(ImageConfiguration.builder()
        .name("foo:latest")
        .build(BuildConfiguration.builder()
            .from("foo-base:latest")
            .build())
        .build());
    // When
    boolean result = new SpringBootGenerator(context).isApplicable(configs);
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isApplicable_withNoImageConfigurations_shouldReturnFalse() {
    // When
    final boolean result = new SpringBootGenerator(context).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isApplicable_withNoImageConfigurationsAndMavenPlugin_shouldReturnTrue() {
    // Given
    withPlugin(Plugin.builder()
        .groupId("org.springframework.boot")
        .artifactId("spring-boot-maven-plugin")
        .build());
    // When
    final boolean result = new SpringBootGenerator(context).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isApplicable_withNoImageConfigurationsAndGradlePlugin_shouldReturnTrue() {
    // Given
    withPlugin(Plugin.builder()
        .groupId("org.springframework.boot")
        .artifactId("org.springframework.boot.gradle.plugin")
        .build());
    // When
    final boolean result = new SpringBootGenerator(context).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void getExtraJavaOptions_withDefaults_shouldBeEmpty() {
    // When
    final List<String> result = new SpringBootGenerator(context).getExtraJavaOptions();
    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  void customize_withEmptyList_shouldReturnAddedImage() {
    // When
    final List<ImageConfiguration> configs = new SpringBootGenerator(context).customize(new ArrayList<>(), true);
    // Then
    assertThat(configs)
        .singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .hasFieldOrPropertyWithValue("ports", Arrays.asList("8080", "8778", "9779"))
        .extracting(BuildConfiguration::getEnv)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .hasSize(1)
        .containsEntry("JAVA_APP_DIR", "/deployments");
  }

  @Test
  void isFatJar_whenSpringBootRepackageAndNoMainClass_thenReturnTrue() {
    // Given
    context = context.toBuilder()
        .project(context.getProject().toBuilder().plugin(Plugin.builder()
                .groupId("org.springframework.boot")
                .artifactId("spring-boot-maven-plugin")
                .executions(Collections.singletonList("repackage"))
                .build())
            .build())
        .build();
    SpringBootGenerator springBootGenerator = new SpringBootGenerator(context);

    // When
    boolean result = springBootGenerator.isFatJar();

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void getEnv_whenGeneratorModeWatch_thenAddSpringBootDevtoolsEnvVar() {
    // Given
    context = context.toBuilder().generatorMode(GeneratorMode.WATCH)
        .project(context.getProject().toBuilder().compileClassPathElement(Objects.requireNonNull(getClass().getResource("/devtools-application-properties")).getPath())
            .build())
        .build();
    SpringBootGenerator springBootGenerator = new SpringBootGenerator(context);

    // When
    Map<String, String> result = springBootGenerator.getEnv(true);

    // Then
    assertThat(result)
        .containsEntry("SPRING_DEVTOOLS_REMOTE_SECRET", "some-secret");
  }

  @Test
  void customize_whenGeneratorModeWatchAndNoDevtoolsRemoteSecret_shouldThrowException(@TempDir File temporaryFolder) {
    // Given
    context = context.toBuilder()
        .project(context.getProject().toBuilder()
            .baseDirectory(temporaryFolder)
            .build())
        .generatorMode(GeneratorMode.WATCH)
        .build();

    // When + Then
    assertThatIllegalStateException()
        .isThrownBy(() -> new SpringBootGenerator(context).customize(new ArrayList<>(), false))
        .withMessage("No spring.devtools.remote.secret found in application.properties. Plugin has added it, please re-run goals");
  }

  @Test
  void customize_whenGeneratorModeWatchAndDevtoolsSecretPresent_thenAddDevtoolsSecretEnvVar() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.generator.spring-boot.mainClass", "org.example.Foo");
    context = context.toBuilder()
        .generatorMode(GeneratorMode.WATCH)
        .project(context.getProject().toBuilder()
            .properties(properties)
            .compileClassPathElement(Objects.requireNonNull(getClass().getResource("/devtools-application-properties")).getPath())
            .build())
        .build();

    // When
    List<ImageConfiguration> imageConfigurations = new SpringBootGenerator(context).customize(new ArrayList<>(), false);

    // Then
    assertThat(imageConfigurations)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getEnv)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("SPRING_DEVTOOLS_REMOTE_SECRET", "some-secret");
  }

  @Test
  void getExtraJavaOptions_whenColorEnabled_thenAddColorOption() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.generator.spring-boot.color", "detect");
    context = context.toBuilder().project(context.getProject().toBuilder().properties(properties).build()).build();
    SpringBootGenerator springBootGenerator = new SpringBootGenerator(context);

    // When
    List<String> javaOptions = springBootGenerator.getExtraJavaOptions();

    // Then
    assertThat(javaOptions).contains("-Dspring.output.ansi.enabled=detect");
  }

  private void withPlugin(Plugin plugin) {
    context = context.toBuilder()
      .project(JavaProject.builder().plugin(plugin).build())
      .build();
  }
}
