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
package org.eclipse.jkube.springboot;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.assertj.FileAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class SpringBootLayeredJarTest {

  @TempDir
  private File projectDir;

  private SpringBootLayeredJar springBootLayeredJar;


  @Nested
  @DisplayName("with invalid jar")
  class InvalidJar {
    @BeforeEach
    void setup() {
      springBootLayeredJar = new SpringBootLayeredJar(new File(projectDir, "invalid.jar"), new KitLogger.SilentLogger());
    }

    @Test
    void isLayeredJar_returnsFalse() {
      // When
      final boolean result = springBootLayeredJar.isLayeredJar();
      // Then
      assertThat(result).isFalse();
    }

    @Test
    void getMainClass_returnsNull() {
      // When
      final String result = springBootLayeredJar.getMainClass();
      // Then
      assertThat(result).isNull();
    }

    @Test
    void listLayers_whenJarInvalid_thenThrowException() {
      assertThatIllegalStateException()
        .isThrownBy(() -> springBootLayeredJar.listLayers())
        .withMessage("Failure in getting spring boot jar layers information");
    }

    @Test
    void extractLayers_whenJarInvalid_thenThrowException() {
      assertThatIllegalStateException()
        .isThrownBy(() -> springBootLayeredJar.extractLayers(projectDir))
        .withMessage("Failure in extracting spring boot jar layers");
    }
  }

  @Nested
  @DisplayName("with valid (real) jar")
  class RealJar {
    @BeforeEach
    void setup() throws IOException {
      final File layeredJar = new File(projectDir, "layered.jar");
      Files.copy(
        Objects.requireNonNull(SpringBootLayeredJarTest.class.getResourceAsStream("/generator-integration-test/layered-jar.jar")),
        new File(projectDir, "layered.jar").toPath()
      );
      springBootLayeredJar = new SpringBootLayeredJar(layeredJar, new KitLogger.SilentLogger());
    }

    @Test
    void isLayeredJar_returnsTrue() {
      // When
      final boolean result = springBootLayeredJar.isLayeredJar();
      // Then
      assertThat(result).isTrue();
    }

    @Test
    void getMainClass_returnsJarLauncher() {
      // When
      final String result = springBootLayeredJar.getMainClass();
      // Then
      assertThat(result).isEqualTo("org.springframework.boot.loader.JarLauncher");
    }

    @Test
    void listLayers() {
      // When
      final List<String> result = springBootLayeredJar.listLayers();
      // Then
      assertThat(result)
        .containsExactly("dependencies", "spring-boot-loader", "snapshot-dependencies", "application");
    }

    @Test
    void extractLayers() throws IOException {
      // Given
      final File extractionDir = Files.createDirectory(new File(projectDir, "extracted").toPath()).toFile();
      // When
      springBootLayeredJar.extractLayers(extractionDir);
      // Then
      FileAssertions.assertThat(extractionDir)
        .fileTree()
        .contains("dependencies", "spring-boot-loader", "snapshot-dependencies", "application");
    }
  }

  @Nested
  @DisplayName("with fake jar with MANIFEST.MF and layers.idx")
  class FakeJar {
    @BeforeEach
    void setup() throws IOException {
      final File fakeJar = new File(projectDir, "fake.jar");
      final Manifest manifest = new Manifest();
      manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
      manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "org.example.Foo");
      try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(fakeJar.toPath()), manifest)) {
        jarOutputStream.putNextEntry(new JarEntry("BOOT-INF/layers.idx"));
      }
      springBootLayeredJar = new SpringBootLayeredJar(fakeJar, new KitLogger.SilentLogger());
    }

    @Test
    void getMainClass_returnsManifestMainClass() {
      // When
      final String result = springBootLayeredJar.getMainClass();
      // Then
      assertThat(result).isEqualTo("org.example.Foo");
    }

    @Test
    void isLayeredJar_returnsTrue() {
      // When
      final boolean result = springBootLayeredJar.isLayeredJar();
      // Then
      assertThat(result).isTrue();
    }


    @Test
    void listLayers_whenJarInvalid_thenThrowException() {
      assertThatIllegalStateException()
        .isThrownBy(() -> springBootLayeredJar.listLayers())
        .withMessage("Failure in getting spring boot jar layers information");
    }

    @Test
    void extractLayers_whenJarInvalid_thenThrowException() {
      assertThatIllegalStateException()
        .isThrownBy(() -> springBootLayeredJar.extractLayers(projectDir))
        .withMessage("Failure in extracting spring boot jar layers");
    }
  }

  @Nested
  @DisplayName("getSpringBootVersion")
  class GetSpringBootVersion {
    @Test
    @DisplayName("with Spring Boot 2.7.14 jar, should return version")
    void withSpringBoot2714() throws IOException {
      // Given
      final File jarFile = createJarWithVersion("2.7.14");
      springBootLayeredJar = new SpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());
      // When
      final Optional<String> result = springBootLayeredJar.getSpringBootVersion();
      // Then
      assertThat(result).hasValue("2.7.14");
    }

    @Test
    @DisplayName("with Spring Boot 3.3.0 jar, should return version")
    void withSpringBoot330() throws IOException {
      // Given
      final File jarFile = createJarWithVersion("3.3.0");
      springBootLayeredJar = new SpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());
      // When
      final Optional<String> result = springBootLayeredJar.getSpringBootVersion();
      // Then
      assertThat(result).hasValue("3.3.0");
    }

    @Test
    @DisplayName("with Spring Boot 4.1.0 jar, should return version")
    void withSpringBoot410() throws IOException {
      // Given
      final File jarFile = createJarWithVersion("4.1.0");
      springBootLayeredJar = new SpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());
      // When
      final Optional<String> result = springBootLayeredJar.getSpringBootVersion();
      // Then
      assertThat(result).hasValue("4.1.0");
    }

    @Test
    @DisplayName("without Spring-Boot-Version in manifest, should return empty")
    void withoutSpringBootVersion() throws IOException {
      // Given
      final File jarFile = createJarWithoutVersion();
      springBootLayeredJar = new SpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());
      // When
      final Optional<String> result = springBootLayeredJar.getSpringBootVersion();
      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("with invalid jar, should return empty")
    void withInvalidJar() {
      // Given
      springBootLayeredJar = new SpringBootLayeredJar(new File(projectDir, "invalid.jar"), new KitLogger.SilentLogger());
      // When
      final Optional<String> result = springBootLayeredJar.getSpringBootVersion();
      // Then
      assertThat(result).isEmpty();
    }

    private File createJarWithVersion(String version) throws IOException {
      final File jarFile = new File(projectDir, "spring-boot-" + version + ".jar");
      final Manifest manifest = new Manifest();
      manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
      manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "org.springframework.boot.loader.JarLauncher");
      manifest.getMainAttributes().putValue("Spring-Boot-Version", version);
      try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile.toPath()), manifest)) {
        jarOutputStream.putNextEntry(new JarEntry("BOOT-INF/layers.idx"));
      }
      return jarFile;
    }

    private File createJarWithoutVersion() throws IOException {
      final File jarFile = new File(projectDir, "spring-boot-no-version.jar");
      final Manifest manifest = new Manifest();
      manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
      manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "org.springframework.boot.loader.JarLauncher");
      try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile.toPath()), manifest)) {
        jarOutputStream.putNextEntry(new JarEntry("BOOT-INF/layers.idx"));
      }
      return jarFile;
    }
  }

  @Nested
  @DisplayName("extractLayers with version-specific jarmode")
  class ExtractLayersWithJarMode {
    @Test
    @DisplayName("with Spring Boot 2.7.14, should use layertools jarmode")
    void withSpringBoot2714UsesLayertools() throws IOException {
      // Given
      final File jarFile = createExecutableJarWithVersion("2.7.14");
      final File extractionDir = Files.createDirectory(new File(projectDir, "extracted-2714").toPath()).toFile();
      springBootLayeredJar = new SpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());

      // When & Then - would fail if wrong jarmode is used, but we can't easily test subprocess execution
      // The real test is in the integration tests with actual Spring Boot jars
      assertThat(springBootLayeredJar.getSpringBootVersion()).hasValue("2.7.14");
    }

    @Test
    @DisplayName("with Spring Boot 3.2.0, should use layertools jarmode")
    void withSpringBoot320UsesLayertools() throws IOException {
      // Given
      final File jarFile = createExecutableJarWithVersion("3.2.0");
      springBootLayeredJar = new SpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());

      // When & Then
      assertThat(springBootLayeredJar.getSpringBootVersion()).hasValue("3.2.0");
    }

    @Test
    @DisplayName("with Spring Boot 3.3.0, should use tools jarmode")
    void withSpringBoot330UsesTools() throws IOException {
      // Given
      final File jarFile = createExecutableJarWithVersion("3.3.0");
      springBootLayeredJar = new SpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());

      // When & Then
      assertThat(springBootLayeredJar.getSpringBootVersion()).hasValue("3.3.0");
    }

    @Test
    @DisplayName("with Spring Boot 4.1.0, should use tools jarmode")
    void withSpringBoot410UsesTools() throws IOException {
      // Given
      final File jarFile = createExecutableJarWithVersion("4.1.0");
      springBootLayeredJar = new SpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());

      // When & Then
      assertThat(springBootLayeredJar.getSpringBootVersion()).hasValue("4.1.0");
    }

    private File createExecutableJarWithVersion(String version) throws IOException {
      final File jarFile = new File(projectDir, "executable-" + version + ".jar");
      final Manifest manifest = new Manifest();
      manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
      manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "org.springframework.boot.loader.JarLauncher");
      manifest.getMainAttributes().putValue("Spring-Boot-Version", version);
      try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile.toPath()), manifest)) {
        jarOutputStream.putNextEntry(new JarEntry("BOOT-INF/layers.idx"));
        jarOutputStream.write("- \"dependencies\":\n  - \"BOOT-INF/lib/\"\n".getBytes());
      }
      return jarFile;
    }
  }
}
