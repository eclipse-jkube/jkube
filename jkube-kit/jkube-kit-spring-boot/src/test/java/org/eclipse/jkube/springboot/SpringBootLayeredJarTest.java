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
}
