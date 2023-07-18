/**
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
package org.eclipse.jkube.quarkus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.jkube.quarkus.QuarkusUtils.concatPath;
import static org.eclipse.jkube.quarkus.QuarkusUtils.extractPort;
import static org.eclipse.jkube.quarkus.QuarkusUtils.findQuarkusVersion;
import static org.eclipse.jkube.quarkus.QuarkusUtils.getQuarkusConfiguration;
import static org.eclipse.jkube.quarkus.QuarkusUtils.isStartupEndpointSupported;
import static org.eclipse.jkube.quarkus.QuarkusUtils.resolveCompleteQuarkusHealthRootPath;
import static org.eclipse.jkube.quarkus.QuarkusUtils.resolveQuarkusLivenessPath;
import static org.eclipse.jkube.quarkus.QuarkusUtils.resolveQuarkusStartupPath;

class QuarkusUtilsTest {

  @TempDir
  Path temporaryFolder;

  private JavaProject javaProject;

  @BeforeEach
  void setUp() throws IOException {
    javaProject = JavaProject.builder()
        .properties(new Properties())
        .outputDirectory(Files.createDirectory(temporaryFolder.resolve("target")).toFile())
        .build();
  }

  @Test
  void extractPort_whenServerPortPropertyPresent_thenReturnPort() {
    // Given
    Properties properties = new Properties();
    properties.put("quarkus.http.port", "8888");
    // When
    String result = QuarkusUtils.extractPort(properties, "8080");
    // Then
    assertThat(result).isEqualTo("8888");
  }

  @Test
  void extractPort_whenServerPortPropertyAbsent_thenReturnDefaultPort() {
    // Given
    Properties properties = new Properties();
    // When
    String result = QuarkusUtils.extractPort(properties, "8080");
    // Then
    assertThat(result).isEqualTo("8080");
  }

  @Test
  void getQuarkusConfiguration_propertiesAndYamlProjectProperties_shouldUseProjectProperties() {
    // Given
    javaProject.getProperties().put("quarkus.http.port", "42");
    javaProject.setCompileClassPathElements(Arrays.asList(
        QuarkusUtilsTest.class.getResource("/utils-test/config/yaml/").getPath(),
        QuarkusUtilsTest.class.getResource("/utils-test/config/properties/").getPath()
    ));
    // When
    final Properties props = getQuarkusConfiguration(javaProject);
    // Then
    assertThat(props).containsOnly(
        entry("quarkus.http.port", "42"),
        entry("%dev.quarkus.http.port", "8082"));
  }

  @Test
  void getQuarkusConfiguration_propertiesAndYaml_shouldUseProperties() {
    // Given
    javaProject.setCompileClassPathElements(Arrays.asList(
        QuarkusUtilsTest.class.getResource("/utils-test/config/yaml/").getPath(),
        QuarkusUtilsTest.class.getResource("/utils-test/config/properties/").getPath()
    ));
    // When
    final Properties props = getQuarkusConfiguration(javaProject);
    // Then
    assertThat(props).containsOnly(
        entry("quarkus.http.port", "1337"),
        entry("%dev.quarkus.http.port", "8082"));
  }

  @Test
  void getQuarkusConfiguration_yamlOnly_shouldUseYaml() {
    // Given
    javaProject.setCompileClassPathElements(Collections.singletonList(
        QuarkusUtilsTest.class.getResource("/utils-test/config/yaml/").getPath()
    ));
    // When
    final Properties props = getQuarkusConfiguration(javaProject);
    // Then
    assertThat(props).containsOnly(
        entry("quarkus.http.port", "31337"),
        entry("%dev.quarkus.http.port", "13373"));
  }

  @Test
  void getQuarkusConfiguration_noConfigFiles_shouldReturnEmpty() {
    // Given
    javaProject.setCompileClassPathElements(Collections.singletonList(
        QuarkusUtilsTest.class.getResource("/").getPath()
    ));
    // When
    final Properties props = getQuarkusConfiguration(javaProject);
    // Then
    assertThat(props).isEmpty();
  }

  @Test
  void findQuarkusVersion_noDependency_shouldReturnEmpty() {
    // Given
    javaProject.setDependencies(Collections.emptyList());
    // When
    final String result = findQuarkusVersion(javaProject);
    // Then
    assertThat(result).isNull();
  }

  @Test
  void findQuarkusVersion_withQuarkusUniverseDependency_shouldReturnValidVersion() {
    // Given
    javaProject.setDependencies(quarkusDependencyWithVersion("2.0.1.Final"));
    // When
    final String result = findQuarkusVersion(javaProject);
    // Then
    assertThat(result).isEqualTo("2.0.1.Final");
  }

  @Test
  void resolveCompleteQuarkusHealthRootPath_withHealthRootPathSet_shouldReturnValidPath() {
    // Given
    Properties properties = new Properties();
    properties.setProperty("quarkus.http.non-application-root-path", "q");
    properties.setProperty("quarkus.smallrye-health.root-path", "health");
    properties.setProperty("quarkus.http.root-path", "/");
    javaProject.setProperties(properties);

    // When
    String resolvedHealthPath = resolveCompleteQuarkusHealthRootPath(javaProject, "");

    // Then
    assertThat(resolvedHealthPath).isNotEmpty().isEqualTo("/q/health");
  }

  @Test
  void resolveCompleteQuarkusHealthRootPath_withHealthRootPathSetAbsolute_shouldReturnValidPath() {
    // Given
    Properties properties = new Properties();
    properties.setProperty("quarkus.smallrye-health.root-path", "/health");
    javaProject.setProperties(properties);
    javaProject.setDependencies(quarkusDependencyWithVersion("1.13.7.Final"));

    // When
    String resolvedHealthPath = resolveCompleteQuarkusHealthRootPath(javaProject, "");

    // Then
    assertThat(resolvedHealthPath).isNotEmpty().isEqualTo("/health");
  }

  @Test
  void resolveCompleteQuarkusHealthRootPath_withOldQuarkusVersion_shouldReturnValidPath() {
    // Given
    Properties properties = new Properties();
    properties.setProperty("quarkus.smallrye-health.root-path", "/health");
    properties.setProperty("quarkus.http.root-path", "/root");
    javaProject.setProperties(properties);
    javaProject.setDependencies(quarkusDependencyWithVersion("1.10.5.Final"));

    // When
    String resolvedHealthPath = resolveCompleteQuarkusHealthRootPath(javaProject, "");

    // Then
    assertThat(resolvedHealthPath).isNotEmpty().isEqualTo("/root/health");
  }

  @Test
  void resolveCompleteQuarkusHealthRootPath_withPostPathResolutionChangesQuarkusVersion_shouldReturnAbsolutePath() {
    // Given
    Properties properties = new Properties();
    properties.setProperty("quarkus.smallrye-health.root-path", "/health");
    properties.setProperty("quarkus.http.root-path", "/root");
    javaProject.setProperties(properties);
    javaProject.setDependencies(quarkusDependencyWithVersion("1.13.7.Final"));

    // When
    String resolvedHealthPath = resolveCompleteQuarkusHealthRootPath(javaProject, "");

    // Then
    assertThat(resolvedHealthPath).isNotEmpty().isEqualTo("/health");
  }

  @Test
  void resolveCompleteQuarkusHealthRootPath_withQuarkus2_shouldReturnAbsoluteNonApplicationRootPath() {
    // Given
    Properties properties = new Properties();
    properties.setProperty("quarkus.http.non-application-root-path", "/q");
    properties.setProperty("quarkus.smallrye-health.root-path", "health");
    javaProject.setProperties(properties);
    javaProject.setDependencies(quarkusDependencyWithVersion("1.13.7.Final"));

    // When
    String resolvedHealthPath = resolveCompleteQuarkusHealthRootPath(javaProject, "");

    // Then
    assertThat(resolvedHealthPath).isNotEmpty().isEqualTo("/q/health");
  }

  @Test
  void resolveCompleteQuarkusHealthRootPath_withQuarkus2_shouldReturnCompleteHealthPath() {
    // Given
    Properties properties = new Properties();
    properties.setProperty("quarkus.http.root-path", "/");
    properties.setProperty("quarkus.http.non-application-root-path", "q");
    properties.setProperty("quarkus.smallrye-health.root-path", "health");
    javaProject.setProperties(properties);
    javaProject.setDependencies(quarkusDependencyWithVersion("1.13.7.Final"));

    // When
    String resolvedHealthPath = resolveCompleteQuarkusHealthRootPath(javaProject, "");

    // Then
    assertThat(resolvedHealthPath).isNotEmpty().isEqualTo("/q/health");
  }

  @Test
  void resolveQuarkusLivenessPath_withLivenessPathSet_shouldReturnValidPath() {
    // Given
    Properties properties = new Properties();
    properties.setProperty("quarkus.smallrye-health.liveness-path", "liveness");
    javaProject.setProperties(properties);

    // When
    String resolvedHealthPath = resolveQuarkusLivenessPath(javaProject);

    // Then
    assertThat(resolvedHealthPath).isNotEmpty().isEqualTo("liveness");
  }

  @Test
  void resolveQuarkusStartupPath_withStartupPathSet_shouldReturnValidPath() {
    // Given
    Properties properties = new Properties();
    properties.setProperty("quarkus.smallrye-health.startup-path", "startup");
    javaProject.setProperties(properties);
    // When
    String resolvedStartupPath = resolveQuarkusStartupPath(javaProject);
    // Then
    assertThat(resolvedStartupPath).isNotEmpty()
            .isEqualTo("startup");
  }

  @Test
  void isStartupEndpointSupported_withQuarkusVersionBefore2_1_shouldReturnFalse() {
    // Given
    javaProject.setDependencies(quarkusDependencyWithVersion("2.0.3.Final"));

    // When
    boolean result = isStartupEndpointSupported(javaProject);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isStartupEndpointSupported_withQuarkusVersionAfter2_1_shouldReturnTrue() {
    // Given
    javaProject.setDependencies(quarkusDependencyWithVersion("2.9.2.Final"));

    // When
    boolean result = isStartupEndpointSupported(javaProject);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void concatPath_withEmptyRootAndPrefixed() {
    assertThat(concatPath("/", "/liveness")).isEqualTo("/liveness");
  }

  @Test
  void concatPath_withRootAndFiltered() {
    assertThat(concatPath("/root", null, "/", "q", "liveness")).isEqualTo("/root/q/liveness");
  }

  private List<Dependency> quarkusDependencyWithVersion(String version) {
    return Collections.singletonList(Dependency.builder()
            .groupId("io.quarkus")
            .artifactId("quarkus-universe-bom")
            .version(version)
            .build());
  }
}
