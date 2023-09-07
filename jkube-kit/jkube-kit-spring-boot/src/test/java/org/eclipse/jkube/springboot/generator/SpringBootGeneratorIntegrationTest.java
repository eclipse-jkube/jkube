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
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;

class SpringBootGeneratorIntegrationTest {

  private static final String SPRING_VERSION = "2.7.2";
  private File targetDir;
  private Properties properties;
  private GeneratorContext context;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    properties = new Properties();
    targetDir = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    final JavaProject javaProject = JavaProject.builder()
        .baseDirectory(temporaryFolder.toFile())
        .buildDirectory(targetDir.getAbsoluteFile())
        .buildPackageDirectory(targetDir.getAbsoluteFile())
        .outputDirectory(targetDir)
        .properties(properties)
        .version("1.0.0")
        .dependency(Dependency.builder()
            .groupId("org.springframework.boot")
            .artifactId("spring-boot-web")
            .version(SPRING_VERSION)
            .build())
        .plugin(Plugin.builder()
            .groupId("org.springframework.boot")
            .artifactId("spring-boot-maven-plugin")
            .version(SPRING_VERSION)
            .build())
        .artifactId("sample")
        .buildFinalName("sample")
        .build();
    context = GeneratorContext.builder()
        .logger(new KitLogger.SilentLogger())
        .project(javaProject)
        .build();
  }

  @Nested
  @DisplayName("With fat Jar packaging")
  class StandardPackaging {

    @BeforeEach
    void standardFatJar() {
      Manifest manifest = new Manifest();
      manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
      manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "org.example.Foo");
      try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(
        targetDir.toPath().resolve("fat.jar")), manifest)) {
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
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Test
    @DisplayName("has image name (group/artifact:latest)")
    void imageNameContainsGroupArtifactAndLatestTag() {
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .hasFieldOrPropertyWithValue("name", "%g/%a:%l");
    }

    @Test
    @DisplayName("has 'spring-boot' image alias")
    void imageAliasSpringBoot() {
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .hasFieldOrPropertyWithValue("alias", "spring-boot");
    }

    @Test
    @DisplayName("has image from based on standard Java Exec generator image")
    void hasFrom() {
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getFrom)
        .asString()
        .startsWith("quay.io/jkube/jkube-java");
    }

    @Test
    @DisplayName("has '8080' web port")
    void imageHasDefaultWebPort() {
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
        .singleElement()
        .extracting("buildConfiguration.ports").asList()
        .contains("8080");
    }
    @Test
    @DisplayName("has Jolokia port")
    void hasJolokiaPort() {
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images).singleElement()
        .extracting("buildConfiguration.ports").asList()
        .contains("8778");
    }

    @Test
    @DisplayName("has Prometheus port")
    void hasPrometheusPort() {
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images).singleElement()
        .extracting("buildConfiguration.ports").asList()
        .contains("9779");
    }

    @Test
    @DisplayName("has java environment variable for app dir")
    void hasJavaJavaAppDirEnvVar() {
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getEnv)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("JAVA_APP_DIR", "/deployments");
    }

    @Test
    @DisplayName("creates assembly with Fat Jar")
    void createAssembly() {
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
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
          .containsExactly("fat.jar"));
    }

    @Test
    @DisplayName("with custom main class, has java environment for main class")
    void withCustomMainClass_hasJavaMainClassEnvVar() {
      // Given
      properties.put("jkube.generator.spring-boot.mainClass", "org.example.Foo");
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
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
    @DisplayName("with custom port in application.properties, has overridden web port in image")
    void withApplicationPortOverridden_shouldUseOverriddenWebPort() {
      // Given
      context = context.toBuilder()
        .project(context.getProject().toBuilder()
          .compileClassPathElement(Objects.requireNonNull(getClass().getResource("/port-override-application-properties")).getPath())
          .build())
        .build();
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
        .singleElement()
        .extracting("buildConfiguration.ports").asList()
        .contains("8081");
    }

    @Test
    @DisplayName("with color configuration provided, enables ANSI color output")
    void withColorConfiguration_shouldAddAnsiEnabledPropertyToJavaOptions() {
      // Given
      properties.put("jkube.generator.spring-boot.color", "always");
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getEnv)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("JAVA_OPTIONS", "-Dspring.output.ansi.enabled=always");
    }

    @Test
    @DisplayName("with generator mode WATCH, then add Spring Boot Devtools environment variable to image")
    void withGeneratorModeWatch_shouldAddSpringBootDevtoolsSecretEnvVar() throws IOException {
      // Given
      final Path applicationProperties = Files.createFile(
        Files.createDirectory(targetDir.toPath().resolve("classes"))
          .resolve("application.properties"));
      context = context.toBuilder()
        .generatorMode(GeneratorMode.WATCH)
        .project(context.getProject().toBuilder()
          .compileClassPathElement(Objects.requireNonNull(getClass().getResource("/devtools-application-properties")).getPath())
          .dependency(Dependency.builder()
            .groupId("org.springframework.boot")
            .artifactId("spring-boot-devtools")
            .version(SPRING_VERSION)
            .type("jar")
            .file(Files.createFile(targetDir.toPath().resolve("spring-boot-devtools.zip")).toFile())
            .build())
          .build())
        .build();
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getEnv)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("SPRING_DEVTOOLS_REMOTE_SECRET", "some-secret");
    }
  }

  @Nested
  @DisplayName("With layered jar")
  class LayeredJar {

    @Test
    @DisplayName("should create assembly layers matching layered jar layers")
    void shouldCreateAssemblyLayers() throws IOException {
      // Given
      Files.copy(
        Objects.requireNonNull(SpringBootGeneratorIntegrationTest.class.getResourceAsStream("/generator-integration-test/layered-jar.jar")),
        targetDir.toPath().resolve("layered.jar")
      );
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context).customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
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

  @Nested
  @DisplayName("With Native artifact")
  class Native {

    @BeforeEach
    void nativeArtifact() throws IOException {
      context = context.toBuilder()
        .project(context.getProject().toBuilder()
          .plugin(Plugin.builder().groupId("org.graalvm.buildtools").artifactId("native-maven-plugin").build())
          .build()
        )
        .build();
      File nativeArtifactFile = Files.createFile(targetDir.toPath().resolve("native-binary-artifact")).toFile();
      assertThat(nativeArtifactFile.setExecutable(true)).isTrue();
    }

    @Test
    @DisplayName("creates assembly with native artifact")
    void createAssembly() {
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
          .isNotNull()
          .singleElement()
          .extracting(ImageConfiguration::getBuild)
          .extracting(BuildConfiguration::getAssembly)
          .hasFieldOrPropertyWithValue("targetDir", "/")
          .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", true)
          .extracting(AssemblyConfiguration::getLayers)
          .asList().hasSize(1)
          .satisfies(layers -> assertThat(layers).element(0).asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
              .extracting(Assembly::getFileSets)
              .asList().element(2)
              .hasFieldOrPropertyWithValue("outputDirectory", new File("."))
              .hasFieldOrPropertyWithValue("fileMode", "0755")
              .extracting("includes").asList()
              .containsExactly("native-binary-artifact"));
    }

    @Test
    @DisplayName("has image from based on UBI image")
    void hasFrom() {
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getFrom)
        .asString()
        .startsWith("registry.access.redhat.com/ubi8/ubi-minimal:");
    }

    @Test
    @DisplayName("disables Jolokia port")
    void disablesJolokiaPort() {
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images).singleElement()
          .extracting("buildConfiguration.env")
          .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
          .containsEntry("AB_JOLOKIA_OFF", "true");
    }

    @Test
    @DisplayName("disables Prometheus port")
    void disablesPrometheusPort() {
      // When
      final List<ImageConfiguration> images = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images).singleElement()
          .extracting("buildConfiguration.env")
          .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
          .containsEntry("AB_PROMETHEUS_OFF", "true");
    }


    @Test
    @DisplayName("sets workDir to root directory")
    void setsWorkDir() {
      // When
      final List<ImageConfiguration> result = new SpringBootGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(result).singleElement()
          .extracting("buildConfiguration.workdir").asString()
          .startsWith("/");
    }
  }
}
