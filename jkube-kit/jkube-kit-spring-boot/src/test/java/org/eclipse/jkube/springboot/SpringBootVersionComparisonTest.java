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
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Spring Boot Version Comparison")
class SpringBootVersionComparisonTest {

  @TempDir
  private File projectDir;

  private SpringBootLayeredJar springBootLayeredJar;

  @ParameterizedTest(name = "version {0} should return {1} for isVersion330OrNewer")
  @CsvSource({
    "2.7.14, false",
    "3.0.0, false",
    "3.2.0, false",
    "3.2.9, false",
    "3.3.0, true",
    "3.3.1, true",
    "3.4.0, true",
    "3.10.5, true",
    "4.0.0, true",
    "4.1.0, true",
    "4.1.0-M1, true",
    "4.1.0-SNAPSHOT, true",
    "3.3.0-M1, true",
    "3.2.9-SNAPSHOT, false",
    "4.1.1, true",
    "5.0.0, true",
    "10.0.0, true"
  })
  @DisplayName("version comparison")
  void versionComparison(String version, boolean expected) throws Exception {
    // Given
    final File jarFile = createJarWithVersion(version);
    springBootLayeredJar = new SpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());

    // When - use reflection to call private method
    Method method = SpringBootLayeredJar.class.getDeclaredMethod("isVersion330OrNewer", String.class);
    method.setAccessible(true);
    boolean result = (boolean) method.invoke(springBootLayeredJar, version);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  @DisplayName("with malformed version, should return false")
  void withMalformedVersion() throws Exception {
    // Given
    final File jarFile = createJarWithVersion("invalid");
    springBootLayeredJar = new SpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());

    // When
    Method method = SpringBootLayeredJar.class.getDeclaredMethod("isVersion330OrNewer", String.class);
    method.setAccessible(true);
    boolean result = (boolean) method.invoke(springBootLayeredJar, "invalid");

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("with version containing only major, should return false")
  void withMajorOnlyVersion() throws Exception {
    // Given
    springBootLayeredJar = new SpringBootLayeredJar(new File(projectDir, "test.jar"), new KitLogger.SilentLogger());

    // When
    Method method = SpringBootLayeredJar.class.getDeclaredMethod("isVersion330OrNewer", String.class);
    method.setAccessible(true);
    boolean result = (boolean) method.invoke(springBootLayeredJar, "3");

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("determineJarMode with Spring Boot 2.7.14 should return layertools")
  void determineJarModeWithSpringBoot2714() throws Exception {
    // Given
    final File jarFile = createJarWithVersion("2.7.14");
    springBootLayeredJar = new SpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());

    // When
    Method method = SpringBootLayeredJar.class.getDeclaredMethod("determineJarMode");
    method.setAccessible(true);
    String result = (String) method.invoke(springBootLayeredJar);

    // Then
    assertThat(result).isEqualTo("layertools");
  }

  @Test
  @DisplayName("determineJarMode with Spring Boot 3.3.0 should return tools")
  void determineJarModeWithSpringBoot330() throws Exception {
    // Given
    final File jarFile = createJarWithVersion("3.3.0");
    springBootLayeredJar = new SpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());

    // When
    Method method = SpringBootLayeredJar.class.getDeclaredMethod("determineJarMode");
    method.setAccessible(true);
    String result = (String) method.invoke(springBootLayeredJar);

    // Then
    assertThat(result).isEqualTo("tools");
  }

  @Test
  @DisplayName("determineJarMode with Spring Boot 4.1.0 should return tools")
  void determineJarModeWithSpringBoot410() throws Exception {
    // Given
    final File jarFile = createJarWithVersion("4.1.0");
    springBootLayeredJar = new SpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());

    // When
    Method method = SpringBootLayeredJar.class.getDeclaredMethod("determineJarMode");
    method.setAccessible(true);
    String result = (String) method.invoke(springBootLayeredJar);

    // Then
    assertThat(result).isEqualTo("tools");
  }

  @Test
  @DisplayName("determineJarMode with no version should return null for fallback")
  void determineJarModeWithNoVersion() throws Exception {
    // Given
    final File jarFile = createJarWithoutVersion();
    springBootLayeredJar = new SpringBootLayeredJar(jarFile, new KitLogger.SilentLogger());

    // When
    Method method = SpringBootLayeredJar.class.getDeclaredMethod("determineJarMode");
    method.setAccessible(true);
    String result = (String) method.invoke(springBootLayeredJar);

    // Then
    assertThat(result).isNull();
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
