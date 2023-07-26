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
package org.eclipse.jkube.springboot.generator;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.GeneratorMode;
import org.eclipse.jkube.generator.javaexec.FatJarDetector;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.springboot.SpringBootLayeredJarExecUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
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
      createDummyFatJar(targetDir.toPath().resolve("sample.jar").toFile());

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
  @DisplayName("customize, in Kubernetes and layered jar artifact, should create assembly layers")
  void customize_inKubernetesAndLayeredJarArtifact_shouldCreateAssemblyLayers() throws IOException {
    File layeredJar = targetDir.toPath().resolve("layered.jar").toFile();
    try (
        MockedStatic<SpringBootLayeredJarExecUtils> springBootLayeredJarExecUtilsMockedStatic = mockStatic(SpringBootLayeredJarExecUtils.class);
        MockedConstruction<FatJarDetector> ignore = mockConstruction(FatJarDetector.class, (mock, ctx) -> {
          FatJarDetector.Result fatJarDetectorResult = mock(FatJarDetector.Result.class);
          when(mock.scan()).thenReturn(fatJarDetectorResult);
          when(fatJarDetectorResult.getArchiveFile()).thenReturn(layeredJar);
        })) {
      // Given
      createDummyLayeredJar(layeredJar);
      springBootLayeredJarExecUtilsMockedStatic.when(() -> SpringBootLayeredJarExecUtils.listLayers(any(), any(File.class)))
              .thenReturn(Arrays.asList("dependencies", "spring-boot-loader", "snapshot-dependencies", "application"));
      createExtractedLayers(targetDir);

      // When
      final List<ImageConfiguration> resultImages = new SpringBootGenerator(context).customize(new ArrayList<>(), false);
      // Then
      assertThat(resultImages)
          .isNotNull()
          .singleElement()
          .extracting(ImageConfiguration::getBuild)
          .satisfies(b -> assertThat(b.getEnv())
              .containsEntry("JAVA_MAIN_CLASS", "org.springframework.boot.loader.JarLauncher"))
          .extracting(BuildConfiguration::getAssembly)
          .hasFieldOrPropertyWithValue("targetDir", "/deployments")
          .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", true)
          .extracting(AssemblyConfiguration::getLayers)
          .asList()
          .hasSize(5)
          .contains(
              Assembly.builder()
                  .id("jkube-includes")
                  .fileSet(AssemblyFileSet.builder()
                      .directory(new File("src/main/jkube-includes/bin"))
                      .outputDirectory(new File("bin"))
                      .fileMode("0755")
                      .build())
                  .fileSet(AssemblyFileSet.builder()
                      .directory(new File("src/main/jkube-includes"))
                      .outputDirectory(new File("."))
                      .fileMode("0644")
                      .build())
                  .build(),
              Assembly.builder()
                  .id("dependencies")
                  .fileSet(AssemblyFileSet.builder()
                      .outputDirectory(new File("."))
                      .directory(new File("target/dependencies"))
                      .exclude("*")
                      .fileMode("0640")
                      .build())
                  .build(),
              Assembly.builder()
                  .id("spring-boot-loader")
                  .fileSet(AssemblyFileSet.builder()
                      .outputDirectory(new File("."))
                      .directory(new File("target/spring-boot-loader"))
                      .exclude("*")
                      .fileMode("0640")
                      .build())
                  .build(),
              Assembly.builder()
                  .id("snapshot-dependencies")
                  .fileSet(AssemblyFileSet.builder()
                      .outputDirectory(new File("."))
                      .directory(new File("target/snapshot-dependencies"))
                      .exclude("*")
                      .fileMode("0640")
                      .build())
                  .build(),
              Assembly.builder()
                  .id("application")
                  .fileSet(AssemblyFileSet.builder()
                      .outputDirectory(new File("."))
                      .directory(new File("target/application"))
                      .exclude("*")
                      .fileMode("0640")
                      .build())
                  .build()
              );
    }
  }

  private void createExtractedLayers(File targetDir) throws IOException {
    File applicationLayer = new File(targetDir, "application");
    File dependencies = new File(targetDir, "dependencies");
    File snapshotDependencies = new File(targetDir, "snapshot-dependencies");
    File springBootLoader = new File(targetDir, "spring-boot-loader");
    Files.createDirectories(new File(applicationLayer, "BOOT-INF/classes").toPath());
    Files.createDirectory(applicationLayer.toPath().resolve("META-INF"));
    Files.createFile(applicationLayer.toPath().resolve("BOOT-INF").resolve("classes").resolve("application.properties"));
    Files.createDirectories(dependencies.toPath().resolve("BOOT-INF").resolve("lib"));
    Files.createFile(dependencies.toPath().resolve("BOOT-INF").resolve("lib").resolve("spring-core.jar"));
    Files.createDirectories(snapshotDependencies.toPath().resolve("BOOT-INF").resolve("lib"));
    Files.createFile(snapshotDependencies.toPath().resolve("BOOT-INF").resolve("lib").resolve("test-SNAPSHOT.jar"));
    Files.createDirectories(springBootLoader.toPath().resolve("org").resolve("springframework").resolve("boot").resolve("loader"));
    Files.createFile(springBootLoader.toPath().resolve("org").resolve("springframework").resolve("boot").resolve("loader").resolve("Launcher.class"));
  }

  private void createDummyLayeredJar(File layeredJar) throws IOException {
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "org.springframework.boot.loader.JarLauncher");
    try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(layeredJar.toPath()), manifest)) {
      jarOutputStream.putNextEntry(new JarEntry("META-INF/"));
      jarOutputStream.putNextEntry(new JarEntry("org/"));
      jarOutputStream.putNextEntry(new JarEntry("org/springframework/"));
      jarOutputStream.putNextEntry(new JarEntry("org/springframework/boot/"));
      jarOutputStream.putNextEntry(new JarEntry("org/springframework/boot/loader/"));
      jarOutputStream.putNextEntry(new JarEntry("org/springframework/boot/loader/ClassPathIndexFile.class"));
      jarOutputStream.putNextEntry(new JarEntry("org/springframework/boot/loader/JarLauncher.class"));
      jarOutputStream.putNextEntry(new JarEntry("BOOT-INF/"));
      jarOutputStream.putNextEntry(new JarEntry("BOOT-INF/classes/"));
      jarOutputStream.putNextEntry(new JarEntry("BOOT-INF/layers.idx"));
    }
  }

  private void createDummyFatJar(File layeredJar) throws IOException {
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "org.springframework.boot.loader.JarLauncher");
    try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(layeredJar.toPath()), manifest)) {
      jarOutputStream.putNextEntry(new JarEntry("META-INF/"));
      jarOutputStream.putNextEntry(new JarEntry("org/"));
      jarOutputStream.putNextEntry(new JarEntry("org/springframework/"));
      jarOutputStream.putNextEntry(new JarEntry("org/springframework/boot/"));
      jarOutputStream.putNextEntry(new JarEntry("org/springframework/boot/loader/"));
      jarOutputStream.putNextEntry(new JarEntry("org/springframework/boot/loader/ClassPathIndexFile.class"));
      jarOutputStream.putNextEntry(new JarEntry("org/springframework/boot/loader/JarLauncher.class"));
      jarOutputStream.putNextEntry(new JarEntry("BOOT-INF/"));
      jarOutputStream.putNextEntry(new JarEntry("BOOT-INF/classes/"));
      jarOutputStream.putNextEntry(new JarEntry("BOOT-INF/classpath.idx"));
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
