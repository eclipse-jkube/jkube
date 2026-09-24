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

import static org.eclipse.jkube.springboot.SpringBootLayeredJar.JARMODE_LAYERTOOLS;
import static org.eclipse.jkube.springboot.SpringBootLayeredJar.JARMODE_TOOLS;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for fallback mechanism when Spring Boot version cannot be detected.
 * This test verifies that extractLayers tries both jarmodes (tools then layertools)
 * when the Spring-Boot-Version is missing from the JAR manifest.
 *
 * Uses a package-private seam (executeLayerToolsCommand) to observe actual command
 * invocations from production code without mocking the extraction logic.
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

  /**
   * Represents a single command invocation captured during testing.
   */
  static class CommandInvocation {
    final String jarMode;
    final String[] extractArgs;

    CommandInvocation(String jarMode, String[] extractArgs) {
      this.jarMode = jarMode;
      this.extractArgs = extractArgs;
    }
  }

  @Test
  @DisplayName("when version missing, should try both jarmodes in fallback sequence with correct args")
  void whenVersionMissing_shouldTryBothJarmodesInSequence() throws IOException {
    // Given - JAR without Spring-Boot-Version in manifest
    final File jarFile = createJarWithoutVersion();
    springBootLayeredJar = new TestableSpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());

    // When - extractLayers is called (will fail because commands are stubbed)
    assertThatThrownBy(() -> springBootLayeredJar.extractLayers(extractionDir))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Failure in extracting spring boot jar layers");

    // Then - verify fallback tried both jarmodes in correct order with correct args
    assertThat(springBootLayeredJar.commandInvocations)
      .as("Fallback should try 'tools' first (forward compatible), then 'layertools'")
      .hasSize(2);

    // First attempt: tools jarmode with --force flag
    CommandInvocation firstAttempt = springBootLayeredJar.commandInvocations.get(0);
    assertThat(firstAttempt.jarMode).isEqualTo(JARMODE_TOOLS);
    assertThat(firstAttempt.extractArgs)
      .containsExactly("extract", "--launcher", "--layers", "--destination", ".", "--force");

    // Second attempt: layertools jarmode without --force flag
    CommandInvocation secondAttempt = springBootLayeredJar.commandInvocations.get(1);
    assertThat(secondAttempt.jarMode).isEqualTo(JARMODE_LAYERTOOLS);
    assertThat(secondAttempt.extractArgs)
      .containsExactly("extract", "--destination", ".");
  }

  @ParameterizedTest(name = "when version is {0}, should use {1} with correct args, no fallback")
  @CsvSource({
    "2.7.14, layertools",
    "3.3.0, layertools",
    "4.0.8, layertools",
    "4.1.0, tools",
    "4.1.1, tools"
  })
  @DisplayName("with valid version")
  void whenValidVersion_shouldUseCorrectJarmodeWithoutFallback(String version, String expectedJarMode) throws IOException {
    // Given - JAR with specific Spring Boot version
    final File jarFile = createJarWithVersion(version);
    springBootLayeredJar = new TestableSpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());

    // When - extractLayers is called (will fail and wrap IOException in IllegalStateException)
    assertThatThrownBy(() -> springBootLayeredJar.extractLayers(extractionDir))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Failure in extracting spring boot jar layers")
      .hasCauseInstanceOf(IOException.class);

    // Then - verify only the expected jarmode was tried (no fallback) with correct args
    assertThat(springBootLayeredJar.commandInvocations)
      .as("Spring Boot %s should use '%s' directly, no fallback", version, expectedJarMode)
      .hasSize(1);

    CommandInvocation invocation = springBootLayeredJar.commandInvocations.get(0);
    assertThat(invocation.jarMode).isEqualTo(expectedJarMode);

    // Verify correct args based on jarmode
    if (JARMODE_TOOLS.equals(expectedJarMode)) {
      assertThat(invocation.extractArgs)
        .as("tools jarmode should include --force flag")
        .containsExactly("extract", "--launcher", "--layers", "--destination", ".", "--force");
    } else {
      assertThat(invocation.extractArgs)
        .as("layertools jarmode should not include --force flag")
        .containsExactly("extract", "--destination", ".");
    }
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
   * Testable subclass that observes actual command invocations from production code.
   * Overrides the package-private seam to capture jarMode and extractArgs without
   * executing the actual external command.
   */
  private static class TestableSpringBootLayeredJar extends SpringBootLayeredJar {
    final List<CommandInvocation> commandInvocations = new ArrayList<>();

    TestableSpringBootLayeredJar(File layeredJar, KitLogger kitLogger) {
      super(layeredJar, kitLogger);
    }

    @Override
    void executeLayerToolsCommand(File extractionDir, String jarMode, String[] extractArgs) throws IOException {
      // Capture the actual invocation from production code
      commandInvocations.add(new CommandInvocation(jarMode, extractArgs));
      // Simulate command failure to trigger fallback or error handling
      throw new IOException("Simulated extraction failure");
    }
  }
}
