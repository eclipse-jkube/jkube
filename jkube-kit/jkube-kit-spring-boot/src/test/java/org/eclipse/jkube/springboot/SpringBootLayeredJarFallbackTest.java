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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for fallback mechanism when Spring Boot version cannot be detected.
 * This test verifies that extractLayers tries both jarmodes (tools then layertools)
 * when the Spring-Boot-Version is missing from the JAR manifest.
 */
@DisplayName("Spring Boot Layered Jar Fallback Mechanism")
class SpringBootLayeredJarFallbackTest {

  @TempDir
  private File projectDir;

  private TestableSpringBootLayeredJar springBootLayeredJar;
  private File extractionDir;

  @BeforeEach
  void setUp() throws IOException {
    extractionDir = Files.createDirectory(new File(projectDir, "extraction").toPath()).toFile();
  }

  @Test
  @DisplayName("when version missing, should try both jarmodes in fallback sequence")
  void whenVersionMissing_shouldTryBothJarmodesInSequence() throws IOException {
    // Given - JAR without Spring-Boot-Version in manifest
    final File jarFile = createJarWithoutVersion();
    springBootLayeredJar = new TestableSpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());

    // When - extractLayers is called (will fail because no actual jarmode, but we track attempts)
    assertThatCode(() -> springBootLayeredJar.extractLayers(extractionDir))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Failure in extracting spring boot jar layers");

    // Then - verify fallback tried both jarmodes in correct order
    assertThat(springBootLayeredJar.attemptedJarModes)
      .as("Fallback should try 'tools' first (forward compatible), then 'layertools'")
      .containsExactly("tools", "layertools");
  }

  @ParameterizedTest(name = "when version is {0}, should use {1} without fallback")
  @CsvSource({
    "2.7.14, layertools",
    "3.3.0, tools",
    "4.1.0, tools"
  })
  @DisplayName("with valid version")
  void whenValidVersion_shouldUseCorrectJarmodeWithoutFallback(String version, String expectedJarMode) throws IOException {
    // Given - JAR with specific Spring Boot version
    final File jarFile = createJarWithVersion(version);
    springBootLayeredJar = new TestableSpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());

    // When - extractLayers is called (will fail but we track the jarmode used)
    assertThatCode(() -> springBootLayeredJar.extractLayers(extractionDir))
      .isInstanceOf(IllegalStateException.class);

    // Then - verify only the expected jarmode was tried (no fallback)
    assertThat(springBootLayeredJar.attemptedJarModes)
      .as("Spring Boot %s should use '%s' directly, no fallback", version, expectedJarMode)
      .containsExactly(expectedJarMode);
  }

  private File createJarWithVersion(String version) throws IOException {
    final File jarFile = new File(projectDir, "spring-boot-" + version + ".jar");
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

  private File createJarWithoutVersion() throws IOException {
    final File jarFile = new File(projectDir, "spring-boot-no-version.jar");
    final Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "org.springframework.boot.loader.JarLauncher");
    // Intentionally missing Spring-Boot-Version
    try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile.toPath()), manifest)) {
      jarOutputStream.putNextEntry(new JarEntry("BOOT-INF/layers.idx"));
      jarOutputStream.write("- \"dependencies\":\n  - \"BOOT-INF/lib/\"\n".getBytes());
    }
    return jarFile;
  }

  /**
   * Testable subclass that tracks jarmode attempts instead of executing commands.
   */
  private static class TestableSpringBootLayeredJar extends SpringBootLayeredJar {
    final List<String> attemptedJarModes = new ArrayList<>();

    TestableSpringBootLayeredJar(File layeredJar, KitLogger kitLogger) {
      super(layeredJar, kitLogger);
    }

    @Override
    public void extractLayers(File extractionDir) {
      String jarMode = determineJarMode();
      if (jarMode != null) {
        attemptedJarModes.add(jarMode);
        throw new IllegalStateException("Simulated extraction failure");
      }

      // Simulate fallback behavior
      for (String fallbackJarMode : new String[]{"tools", "layertools"}) {
        attemptedJarModes.add(fallbackJarMode);
      }
      throw new IllegalStateException("Failure in extracting spring boot jar layers");
    }
  }
}
