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
package org.eclipse.jkube.quarkus.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.eclipse.jkube.generator.api.DefaultImageLookup;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

/**
 * @author jzuriaga
 */
class QuarkusGeneratorTest {

  private static final String BASE_JAVA_IMAGE = "configured/jvm:latest";
  private static final String BASE_NATIVE_IMAGE = "configured/native:latest";

  @TempDir
  Path temporaryFolder;

  private File targetDir;
  private ProcessorConfig config;
  private Properties projectProps;
  private JavaProject project;
  private GeneratorContext ctx;

  @BeforeEach
  void setUp() throws IOException {
    config = new ProcessorConfig();
    projectProps = new Properties();
    projectProps.put("jkube.generator.name", "quarkus");
    targetDir = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    project = JavaProject.builder()
      .version("0.0.1-SNAPSHOT")
      .baseDirectory(targetDir)
      .buildDirectory(targetDir.getAbsoluteFile())
      .properties(projectProps)
      .compileClassPathElements(Collections.emptyList())
      .outputDirectory(targetDir)
      .build();
    ctx = GeneratorContext.builder()
      .logger(new KitLogger.SilentLogger())
      .project(project)
      .config(config)
      .strategy(JKubeBuildStrategy.s2i)
      .build();
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("isApplicable")
  class IsApplicable {
    @Test
    @DisplayName("with no dependencies, should return false")
    void withNoDependencies_shouldReturnFalse() {
      // When
      final boolean result = new QuarkusGenerator(ctx).isApplicable(new ArrayList<>());
      // Then
      assertThat(result).isFalse();
    }

    @DisplayName("with plugin ")
    @ParameterizedTest(name = "''{0}:{1}'', should be true")
    @MethodSource("isApplicableTestData")
    void isApplicable(String groupID, String artifactID) {
      // Given
      project.setPlugins(Collections.singletonList(Plugin.builder()
        .groupId(groupID)
        .artifactId(artifactID)
        .build()));
      // When
      final boolean result = new QuarkusGenerator(ctx).isApplicable(new ArrayList<>());
      // Then
      assertThat(result).isTrue();
    }

    Stream<Arguments> isApplicableTestData() {
      return Stream.of(
        Arguments.of("io.quarkus", "quarkus-maven-plugin"),
        Arguments.of("io.quarkus.platform", "quarkus-maven-plugin"),
        Arguments.of("com.redhat.quarkus.platform", "quarkus-maven-plugin"),
        Arguments.of("io.quarkus", "io.quarkus.gradle.plugin")
      );
    }
  }

  @Nested
  @DisplayName("customize")
  class Customize {

    @Test
    @DisplayName("in OpenShift, should return image based on s2i")
    void inOpenShift_shouldReturnS2iFrom() {
      try (MockedConstruction<DefaultImageLookup> ignored = mockConstruction(DefaultImageLookup.class,
        (mock, ctx) ->  when(mock.getImageName("java.upstream.s2i")).thenReturn("quarkus/s2i"))) {
        // Given
        in(RuntimeMode.OPENSHIFT);
        // When
        final List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
        // Then
        assertThat(result).singleElement()
          .extracting("buildConfiguration.from")
          .isEqualTo("quarkus/s2i");
      }
    }

    @Test
    @DisplayName("in Kubernetes, should return image based on docker")
    void inKubernetes_shouldReturnDockerFrom() {
      try (MockedConstruction<DefaultImageLookup> ignored = mockConstruction(DefaultImageLookup.class,
        (mock, ctx) ->  when(mock.getImageName("java.upstream.docker")).thenReturn("quarkus/docker"))) {
        // Given
        in(RuntimeMode.KUBERNETES);
        // When
        final List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
        // Then
        assertThat(result).singleElement()
          .extracting("buildConfiguration.from")
          .isEqualTo("quarkus/docker");
      }
    }

    @Test
    @DisplayName("in OpenShift with native config, should return image based on ubi s2i")
    void inOpenShift_shouldReturnNativeS2iFrom() throws IOException {
      // Given
      in(RuntimeMode.OPENSHIFT);
      withNativeBinaryInTarget();
      // When
      final List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
      // Then
      assertThat(result).singleElement()
        .extracting("buildConfiguration.from")
        .isEqualTo("quay.io/quarkus/ubi-quarkus-native-binary-s2i:1.0");
    }

    @Test
    @DisplayName("in Kubernetes with native artifact, should return image based on ubi")
    void customize_inKubernetes_shouldReturnNativeUbiFrom() throws IOException {
      // Given
      in(RuntimeMode.KUBERNETES);
      withNativeBinaryInTarget();
      // When
      final List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
      // Then
      assertThat(result).singleElement()
        .extracting("buildConfiguration.from").asString()
        .startsWith("registry.access.redhat.com/ubi9/ubi-minimal:");
    }

    @Test
    @DisplayName("in Kubernetes with native artifact, should disable Jolokia")
    void customize_inKubernetes_shouldDisableJolokia() throws IOException {
      // Given
      in(RuntimeMode.KUBERNETES);
      withNativeBinaryInTarget();
      // When
      final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
      // Then
      assertThat(resultImages).singleElement()
        .extracting("buildConfiguration.env")
        .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
        .containsEntry("AB_JOLOKIA_OFF", "true");
    }

    @Test
    @DisplayName("in Kubernetes with native artifact, should disable Prometheus")
    void customize_inKubernetes_shouldDisablePrometheus() throws IOException {
      // Given
      in(RuntimeMode.KUBERNETES);
      withNativeBinaryInTarget();
      // When
      final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
      // Then
      assertThat(resultImages).singleElement()
        .extracting("buildConfiguration.env")
        .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
        .containsEntry("AB_PROMETHEUS_OFF", "true");
    }

    @Test
    @DisplayName("with configured image, should return configured image")
    void withConfiguredImage_shouldReturnConfigured() {
      // Given
      config.getConfig().put("quarkus", Collections.singletonMap("from", BASE_JAVA_IMAGE));
      // When
      List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
      // Then
      assertThat(result).singleElement()
        .extracting("buildConfiguration.from")
        .isEqualTo(BASE_JAVA_IMAGE);
    }

    @Test
    @DisplayName("with configured image and native artifact, should return configured image")
    void withConfiguredImageAndNativeArtifact_shouldReturnConfiguredNative() throws IOException {
      // Given
      config.getConfig().put("quarkus", Collections.singletonMap("from", BASE_NATIVE_IMAGE));
      withNativeBinaryInTarget();
      // When
      List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
      // Then
      assertThat(result).singleElement()
        .extracting("buildConfiguration.from")
        .isEqualTo(BASE_NATIVE_IMAGE);
    }

    @Test
    @DisplayName("with configured image in properties, should return configured image")
    void withConfiguredInProperties_shouldReturnConfigured() {
      // Given
      projectProps.put("jkube.generator.quarkus.from", BASE_JAVA_IMAGE);
      // When
      List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
      // Then
      assertThat(result).singleElement()
        .extracting("buildConfiguration.from")
        .isEqualTo(BASE_JAVA_IMAGE);
    }

    @Test
    @DisplayName("with configured image in properties and native artifact, should return configured image")
    void withConfiguredInPropertiesAndNativeArtifact_shouldReturnConfiguredNative() throws IOException {
      projectProps.put("jkube.generator.quarkus.from", BASE_NATIVE_IMAGE);
      withNativeBinaryInTarget();
      // When
      List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), true);
      // Then
      assertThat(result).singleElement()
        .extracting("buildConfiguration.from")
        .isEqualTo(BASE_NATIVE_IMAGE);
    }
  }

  @Test
  void isFatJar_withDefaults_shouldBeFalse() {
    // When
    final boolean result = new QuarkusGenerator(ctx).isFatJar();
    // Then
    assertThat(result).isFalse();
  }

  @Nested
  @DisplayName("customize (assembly)")
  class CustomizeAssembly {
    @Test
    @DisplayName("with fastJar artifact, should return fastJar assembly in image")
    void withFastJarInTarget_shouldReturnFastJarAssemblyInImage() throws IOException {
      // Given
      withFastJarInTarget();
      // When
      final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx)
        .customize(new ArrayList<>(), false);
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
          .hasFieldOrPropertyWithValue("id", "lib")
          .extracting(Assembly::getFileSets)
          .asList().singleElement()
          .hasFieldOrPropertyWithValue("outputDirectory", new File("."))
          .extracting("includes").asList()
          .containsExactly("lib"))
        .satisfies(layers -> assertThat(layers).element(1).asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
          .hasFieldOrPropertyWithValue("id", "fast-jar")
          .extracting(Assembly::getFileSets)
          .asList().singleElement()
          .hasFieldOrPropertyWithValue("outputDirectory", new File("."))
          .hasFieldOrPropertyWithValue("excludes", Arrays.asList("lib/**/*", "lib/*"))
          .extracting("includes").asList()
          .containsExactly("quarkus-run.jar", "*", "**/*"));
    }

    @Test
    @DisplayName("with all artifacts and manual native settings, should return native assembly in image")
    void manualNativeSettings_shouldReturnNativeAssemblyInImage() throws IOException {
      // Given
      projectProps.put("jkube.generator.quarkus.nativeImage", "true");
      withUberJarInTarget();
      withNativeBinaryInTarget();
      withLegacyJarInTarget();
      withFastJarInTarget();
      // When
      final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx)
        .customize(new ArrayList<>(), false);
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
        .containsExactly("sample-runner");
    }
    @Test
    @DisplayName("with native artifact, should return native assembly in image")
    void withNativeBinaryInTarget_shouldReturnNativeAssemblyInImage() throws IOException {
      // Given
      withNativeBinaryInTarget();
      // When
      final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx)
        .customize(new ArrayList<>(), false);
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
        .containsExactly("sample-runner");
    }

    @Test
    @DisplayName("with legacy-jar artifact, should return legacy-jar assembly in image")
    void withLegacyJarInTarget_shouldReturnDefaultAssemblyInImage() throws IOException {
      // Given
      withLegacyJarInTarget();
      // When
      final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx)
        .customize(new ArrayList<>(), false);
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
          .hasFieldOrPropertyWithValue("id", "lib")
          .extracting(Assembly::getFileSets)
          .asList()
          .hasSize(1)
          .flatExtracting("includes")
          .containsExactly("lib"))
        .satisfies(layers -> assertThat(layers).element(1).asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
          .hasFieldOrPropertyWithValue("id", "artifact")
          .extracting(Assembly::getFileSets)
          .asList()
          .hasSize(1)
          .flatExtracting("includes")
          .containsExactly("sample-legacy-runner.jar"));
    }

    @Test
    @DisplayName("with configured packaging and no artifact, should throw exception")
    void withConfiguredPackagingAndNoJar_shouldThrowException() {
      // Given
      projectProps.put("quarkus.package.type", "legacy-jar");
      final QuarkusGenerator quarkusGenerator = new QuarkusGenerator(ctx);
      final List<ImageConfiguration> list = new ArrayList<>();
      // When & Then
      assertThatIllegalStateException()
        .isThrownBy(() -> quarkusGenerator.customize(list, false))
        .withMessageContaining("Can't find single file with suffix '-runner.jar'");
    }

    @Test
    @DisplayName("with uber-jar artifact, should return uber-jar assembly in image")
    void withUberJarInTarget_shouldReturnAssemblyWithSingleJar() throws IOException {
      // Given
      withUberJarInTarget();
      // When
      final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(resultImages)
        .isNotNull()
        .singleElement()
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getAssembly)
        .hasFieldOrPropertyWithValue("targetDir", "/deployments")
        .extracting(AssemblyConfiguration::getLayers)
        .asList().singleElement().asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
        .extracting(Assembly::getFileSets)
        .asList()
        .hasSize(1)
        .flatExtracting("includes")
        .containsExactly("sample-runner.jar");
    }

    @Test
    void withManualConfigAndFastJarAndLegacyInTarget_shouldReturnAssemblyForQuarkusAppInImage() throws IOException {
      // Given
      projectProps.put("quarkus.package.type", "fast-jar");
      withFastJarInTarget();
      withLegacyJarInTarget();
      // When
      final List<ImageConfiguration> resultImages = new QuarkusGenerator(ctx)
        .customize(new ArrayList<>(), false);
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
          .hasFieldOrPropertyWithValue("id", "lib")
          .extracting(Assembly::getFileSets)
          .asList().singleElement()
          .hasFieldOrPropertyWithValue("outputDirectory", new File("."))
          .extracting("includes").asList()
          .containsExactly("lib"))
        .satisfies(layers -> assertThat(layers).element(1).asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
          .hasFieldOrPropertyWithValue("id", "fast-jar")
          .extracting(Assembly::getFileSets)
          .asList().singleElement()
          .hasFieldOrPropertyWithValue("outputDirectory", new File("."))
          .hasFieldOrPropertyWithValue("excludes", Arrays.asList("lib/**/*", "lib/*"))
          .extracting("includes").asList()
          .containsExactly("quarkus-run.jar", "*", "**/*"));
    }

    @Test
    @DisplayName("with configured fast-jar packaging and legacy-jar artifact, should throw exception")
    void withManualFastJarConfigAndLegacyInTarget_shouldThrowException() throws IOException {
      // Given
      projectProps.put("quarkus.package.type", "fast-jar");
      withLegacyJarInTarget();
      final QuarkusGenerator qg = new QuarkusGenerator(ctx);
      final List<ImageConfiguration> configs = Collections.emptyList();
      // When & Then
      assertThatIllegalStateException()
        .isThrownBy(() -> qg.customize(configs, false))
        .withMessageContaining("The quarkus-app directory required in Quarkus Fast Jar mode was not found");
    }
  }

  private void withNativeBinaryInTarget() throws IOException {
    Files.createFile(targetDir.toPath().resolve("sample-runner"));
  }

  private void withUberJarInTarget() throws IOException {
    Files.createFile(targetDir.toPath().resolve("sample-runner.jar"));
  }

  private void withLegacyJarInTarget() throws IOException {
    Files.createFile(targetDir.toPath().resolve("sample-legacy-runner.jar"));
    final Path lib = targetDir.toPath().resolve("lib");
    Files.createDirectory(lib);
    Files.createFile(lib.resolve("dependency.jar"));
  }

  private void withFastJarInTarget() throws IOException {
    final Path quarkusApp = targetDir.toPath().resolve("quarkus-app");
    Files.createDirectory(quarkusApp);
    Files.createDirectory(quarkusApp.resolve("app"));
    Files.createDirectory(quarkusApp.resolve("lib"));
    Files.createDirectory(quarkusApp.resolve("quarkus"));
    Files.createFile(quarkusApp.resolve("quarkus-run.jar"));
  }

  private void in(RuntimeMode runtimeMode) {
    ctx = ctx.toBuilder().runtimeMode(runtimeMode).build();
  }

}
