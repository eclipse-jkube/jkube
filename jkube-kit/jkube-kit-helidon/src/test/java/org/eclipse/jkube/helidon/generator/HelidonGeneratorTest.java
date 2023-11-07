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
package org.eclipse.jkube.helidon.generator;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class HelidonGeneratorTest {
  @TempDir
  Path temporaryFolder;

  private File targetDir;
  private Properties projectProps;
  private JavaProject project;
  private GeneratorContext ctx;

  @BeforeEach
  public void setUp() throws IOException {
    ProcessorConfig config = new ProcessorConfig();
    projectProps = new Properties();
    projectProps.put("jkube.generator.name", "helidon");
    targetDir = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    project = JavaProject.builder()
        .version("0.0.1-SNAPSHOT")
        .baseDirectory(targetDir)
        .buildDirectory(targetDir.getAbsoluteFile())
        .buildPackageDirectory(targetDir.getAbsoluteFile())
        .properties(projectProps)
        .compileClassPathElements(Collections.emptyList())
        .outputDirectory(targetDir)
        .buildFinalName("sample")
        .packaging("jar")
        .build();
    ctx = GeneratorContext.builder()
        .logger(new KitLogger.SilentLogger())
        .project(project)
        .config(config)
        .strategy(JKubeBuildStrategy.s2i)
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
    boolean result = new HelidonGenerator(ctx).isApplicable(configs);
    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("isApplicable, with no valid ImageConfiguration and no Helidon dependency, returns false")
  void isApplicable_noHelidonDependencyNoImageConfiguration_returnsFalse() {
    // Given
    final List<ImageConfiguration> configs = Collections.emptyList();
    // When
    boolean result = new HelidonGenerator(ctx).isApplicable(configs);
    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("isApplicable, with no valid ImageConfiguration and Helidon dependency, returns true")
  void isApplicable_helidonDependencyNoImageConfiguration_returnsTrue() {
    // Given
    project.setDependenciesWithTransitive(Collections.singletonList(Dependency.builder()
        .groupId("io.helidon.webserver")
        .artifactId("helidon-webserver")
        .build()));
    final List<ImageConfiguration> configs = Collections.emptyList();
    // When
    boolean result = new HelidonGenerator(ctx).isApplicable(configs);
    // Then
    assertThat(result).isTrue();
  }

  @ParameterizedTest(name = "{index}: customize, standard packaging in ''{0}'' mode, should return ''{1}'' as from image")
  @MethodSource("customize_withStandardPackaging_fromData")
  void customize_withStandardPackaging_from(RuntimeMode runtimeMode, String expectedFromStartsWith) {
    // Given
    in(runtimeMode);
    // When
    final List<ImageConfiguration> result = new HelidonGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertThat(result).singleElement()
      .extracting("buildConfiguration.from").asString()
      .startsWith(expectedFromStartsWith);
  }

  static Stream<Arguments> customize_withStandardPackaging_fromData() {
    return Stream.of(
      Arguments.of(RuntimeMode.KUBERNETES, "quay.io/jkube/jkube-java:"),
      Arguments.of(RuntimeMode.OPENSHIFT, "quay.io/jkube/jkube-java:")
    );
  }

  @ParameterizedTest(name = "{index}: customize, native packaging in ''{0}'' mode, should return ''{1}'' as from image")
  @MethodSource("customize_withNativePackaging_fromData")
  void customize_withNativePackaging_from(RuntimeMode runtimeMode, String expectedFromStartsWith) {
    // Given
    withNativeExtensionDependencyInTarget();
    in(runtimeMode);
    // When
    final List<ImageConfiguration> result = new HelidonGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertThat(result).singleElement()
      .extracting("buildConfiguration.from").asString()
      .startsWith(expectedFromStartsWith);
  }

  static Stream<Arguments> customize_withNativePackaging_fromData() {
    return Stream.of(
      Arguments.of(RuntimeMode.KUBERNETES, "registry.access.redhat.com/ubi9/ubi-minimal:"),
      Arguments.of(RuntimeMode.OPENSHIFT, "registry.access.redhat.com/ubi9/ubi-minimal:")
    );
  }

  @Test
  @DisplayName("customize, with standard packaging, should set workDir to targetDir")
  void customize_withStandardPackaging_workDir(){
    // When
    final List<ImageConfiguration> result = new HelidonGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertThat(result).singleElement()
      .extracting("buildConfiguration.workdir").asString()
      .startsWith("/deployments");
  }

  @Test
  @DisplayName("customize, with native packaging, should set workDir to root directory")
  void customize_withNativePackaging_workDir(){
    // Given
    withNativeExtensionDependencyInTarget();
    // When
    final List<ImageConfiguration> result = new HelidonGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertThat(result).singleElement()
      .extracting("buildConfiguration.workdir").asString()
      .startsWith("/");
  }

  @Test
  @DisplayName("customize, with standard packaging, has Jolokia port")
  void customize_withStandardPackaging_hasJolokiaPort(){
    // When
    final List<ImageConfiguration> result = new HelidonGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertThat(result).singleElement()
      .extracting("buildConfiguration.ports").asList()
      .contains("8778");
  }

  @Test
  @DisplayName("customize, with native packaging, disables Jolokia")
  void customize_withNativePackaging_disablesJolokia(){
    // Given
    withNativeExtensionDependencyInTarget();
    // When
    final List<ImageConfiguration> result = new HelidonGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertThat(result).singleElement()
      .extracting("buildConfiguration.env")
      .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
      .containsEntry("AB_JOLOKIA_OFF", "true");
  }

  @Test
  @DisplayName("customize, with standard packaging, has Prometheus port")
  void customize_withStandardPackaging_hasPrometheusPort(){
    // When
    final List<ImageConfiguration> result = new HelidonGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertThat(result).singleElement()
      .extracting("buildConfiguration.ports").asList()
      .contains("9779");
  }

  @Test
  @DisplayName("customize, with native packaging, disables Prometheus")
  void customize_withNativePackaging_disablesPrometheus(){
    // Given
    withNativeExtensionDependencyInTarget();
    // When
    final List<ImageConfiguration> result = new HelidonGenerator(ctx).customize(new ArrayList<>(), true);
    // Then
    assertThat(result).singleElement()
      .extracting("buildConfiguration.env")
      .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
      .containsEntry("AB_PROMETHEUS_OFF", "true");
  }

  @Test
  @DisplayName("customize, in Kubernetes and jar artifact, should create assembly")
  void customize_inKubernetesAndJarArtifact_shouldCreateAssembly() throws IOException {
    // Given
    in(RuntimeMode.KUBERNETES);
    projectProps.put("jkube.generator.helidon.mainClass", "org.example.Main");
    whenStandardJarInTarget();

    // When
    final List<ImageConfiguration> resultImages = new HelidonGenerator(ctx).customize(new ArrayList<>(), false);

    // Then
    assertThat(resultImages)
        .isNotNull()
        .singleElement()
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getAssembly)
        .hasFieldOrPropertyWithValue("targetDir", "/deployments")
        .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", true)
        .extracting(AssemblyConfiguration::getLayers)
        .asList().hasSize(2)
        .satisfies(layers -> assertThat(layers).first().asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
            .hasFieldOrPropertyWithValue("id", "libs")
            .extracting(Assembly::getFileSets)
            .asList().singleElement()
            .hasFieldOrPropertyWithValue("outputDirectory", new File("."))
            .extracting("includes").asList()
            .containsExactly("libs"))
        .satisfies(layers -> assertThat(layers).element(1).asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
            .hasFieldOrPropertyWithValue("id", "artifact")
            .extracting(Assembly::getFileSets)
            .asList().singleElement()
            .hasFieldOrPropertyWithValue("outputDirectory", new File("."))
            .extracting("includes").asList()
            .containsExactly("sample.jar"));
  }

  @Test
  @DisplayName("customize, in Kubernetes and jar artifact, should create assembly")
  void customize_inKubernetesAndNativeArtifact_shouldCreateNativeAssembly() throws IOException {
    // Given
    in(RuntimeMode.KUBERNETES);
    projectProps.put("jkube.generator.helidon.mainClass", "org.example.Main");
    withNativeExtensionDependencyInTarget();
    withNativeBinaryInTarget();

    // When
    final List<ImageConfiguration> resultImages = new HelidonGenerator(ctx).customize(new ArrayList<>(), false);

    // Then
    assertThat(resultImages)
        .isNotNull()
        .singleElement()
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getAssembly)
        .hasFieldOrPropertyWithValue("targetDir", "/")
        .extracting(AssemblyConfiguration::getLayers)
        .asList().singleElement().asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
        .extracting(Assembly::getFileSets)
        .asList()
        .hasSize(1)
        .flatExtracting("includes")
        .containsExactly("sample");
  }

  private void in(RuntimeMode runtimeMode) {
    ctx = ctx.toBuilder().runtimeMode(runtimeMode).build();
  }

  private void withNativeExtensionDependencyInTarget() {
    project.setDependencies(Collections.singletonList(Dependency.builder()
        .groupId("io.helidon.integrations.graal")
        .artifactId("helidon-graal-native-image-extension")
        .build()));
  }

  private void whenStandardJarInTarget() throws IOException {
    Files.createFile(targetDir.toPath().resolve("sample.jar"));
    final Path lib = targetDir.toPath().resolve("libs");
    Files.createDirectory(lib);
    Files.createFile(lib.resolve("dependency.jar"));
  }

  private void withNativeBinaryInTarget() throws IOException {
    Files.createFile(targetDir.toPath().resolve("sample"));
  }
}
