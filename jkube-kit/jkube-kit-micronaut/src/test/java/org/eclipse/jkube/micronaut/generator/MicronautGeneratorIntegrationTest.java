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
package org.eclipse.jkube.micronaut.generator;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Arguments;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;

class MicronautGeneratorIntegrationTest {

  private static final String MICRONAUT_VERSION = "4.2.0";
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
        .groupId("io.micronaut")
        .artifactId("micronaut-http-server-netty")
        .version(MICRONAUT_VERSION)
        .build())
      .plugin(Plugin.builder()
        .groupId("io.micronaut.maven")
        .artifactId("micronaut-maven-plugin")
        .version(MICRONAUT_VERSION)
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
        jarOutputStream.putNextEntry(new JarEntry("org/example/"));
        jarOutputStream.putNextEntry(new JarEntry("org/example/Foo.class"));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Test
    @DisplayName("has image name (group/artifact:latest)")
    void imageNameContainsGroupArtifactAndLatestTag() {
      // When
      final List<ImageConfiguration> images = new MicronautGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .hasFieldOrPropertyWithValue("name", "%g/%a:%l");
    }

    @Test
    @DisplayName("has 'micronaut' image alias")
    void imageAliasMicronaut() {
      // When
      final List<ImageConfiguration> images = new MicronautGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .hasFieldOrPropertyWithValue("alias", "micronaut");
    }

    @Test
    @DisplayName("has image from based on standard Java Exec generator image")
    void hasFrom() {
      // When
      final List<ImageConfiguration> images = new MicronautGenerator(context)
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
      final List<ImageConfiguration> images = new MicronautGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
        .singleElement()
        .extracting("buildConfiguration.ports")
        .asInstanceOf(InstanceOfAssertFactories.list(String.class))
        .contains("8080");
    }

    @Test
    @DisplayName("has Jolokia port")
    void hasJolokiaPort() {
      // When
      final List<ImageConfiguration> images = new MicronautGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images).singleElement()
        .extracting("buildConfiguration.ports")
        .asInstanceOf(InstanceOfAssertFactories.list(String.class))
        .contains("8778");
    }

    @Test
    @DisplayName("has Prometheus port")
    void hasPrometheusPort() {
      // When
      final List<ImageConfiguration> images = new MicronautGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images).singleElement()
        .extracting("buildConfiguration.ports")
        .asInstanceOf(InstanceOfAssertFactories.list(String.class))
        .contains("9779");
    }

    @Test
    @DisplayName("has java environment variable for app dir")
    void hasJavaJavaAppDirEnvVar() {
      // When
      final List<ImageConfiguration> images = new MicronautGenerator(context)
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
      final List<ImageConfiguration> images = new MicronautGenerator(context)
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
        .asInstanceOf(InstanceOfAssertFactories.list(Assembly.class))
        .hasSize(1)
        .satisfies(layers -> assertThat(layers).element(0).asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
          .extracting(Assembly::getFileSets)
          .asInstanceOf(InstanceOfAssertFactories.list(AssemblyFileSet.class))
          .element(2)
          .hasFieldOrPropertyWithValue("outputDirectory", new File("."))
          .extracting("includes")
          .asInstanceOf(InstanceOfAssertFactories.list(String.class))
          .containsExactly("fat.jar"));
    }

    @Test
    @DisplayName("with custom main class, has java environment for main class")
    void withCustomMainClass_hasJavaMainClassEnvVar() {
      // Given
      properties.put("jkube.generator.micronaut.mainClass", "org.example.Foo");
      // When
      final List<ImageConfiguration> images = new MicronautGenerator(context)
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
          .compileClassPathElement(Objects.requireNonNull(getClass().getResource("/utils-test/port-config/properties/")).getPath())
          .build())
        .build();
      // When
      final List<ImageConfiguration> images = new MicronautGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
        .singleElement()
        .extracting("buildConfiguration.ports")
        .asInstanceOf(InstanceOfAssertFactories.list(String.class))
        .contains("1337");
    }
  }

  @Nested
  @DisplayName("With Native artifact")
  class Native {

    @BeforeEach
    void nativeArtifact() throws IOException {
      properties.put("packaging", "native-image");
      context = context.toBuilder()
        .project(context.getProject().toBuilder()
          .properties(properties)
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
      final List<ImageConfiguration> images = new MicronautGenerator(context)
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
        .asInstanceOf(InstanceOfAssertFactories.list(Assembly.class))
        .hasSize(1)
        .satisfies(layers -> assertThat(layers).element(0).asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
          .extracting(Assembly::getFileSets)
          .asInstanceOf(InstanceOfAssertFactories.list(AssemblyFileSet.class))
          .element(2)
          .hasFieldOrPropertyWithValue("outputDirectory", new File("."))
          .hasFieldOrPropertyWithValue("fileMode", "0755")
          .extracting("includes")
          .asInstanceOf(InstanceOfAssertFactories.list(String.class))
          .containsExactly("native-binary-artifact"));
    }

    @Test
    @DisplayName("has image from based on UBI image")
    void hasFrom() {
      // When
      final List<ImageConfiguration> images = new MicronautGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getFrom)
        .asString()
        .startsWith("registry.access.redhat.com/ubi9/ubi-minimal:");
    }

    @Test
    @DisplayName("disables Jolokia port")
    void disablesJolokiaPort() {
      // When
      final List<ImageConfiguration> images = new MicronautGenerator(context)
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
      final List<ImageConfiguration> images = new MicronautGenerator(context)
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
      final List<ImageConfiguration> result = new MicronautGenerator(context)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(result).singleElement()
        .extracting("buildConfiguration.workdir").asString()
        .startsWith("/");
    }

    @Test
    @DisplayName("has entrypoint pointing to native binary")
    void hasCustomEntrypoint() {
      // When
      final List<ImageConfiguration> images = new MicronautGenerator(context).customize(new ArrayList<>(), false);
      // Then
      assertThat(images)
        .isNotNull()
        .singleElement()
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getEntryPoint)
        .returns(Collections.singletonList("./native-binary-artifact"), Arguments::asStrings);
    }
  }
}
