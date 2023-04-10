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
package org.eclipse.jkube.helidon;

import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class HelidonUtilsTest {
  @Test
  void hasHelidonDependencies_whenHelidonDependenciesPresent_thenReturnTrue() {
    // Given
    JavaProject javaProject = createNewJKubeProjectWithDeps("io.helidon.webserver", "helidon-webserver");

    // When
    boolean result = HelidonUtils.hasHelidonDependencies(javaProject);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void hasHelidonDependencies_whenHelidonDependenciesAbsent_thenReturnFalse() {
    // Given
    JavaProject javaProject = createNewJKubeProjectWithDeps("com.example", "test-webserver");

    // When
    boolean result = HelidonUtils.hasHelidonDependencies(javaProject);

    // Then
    assertThat(result).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"helidon-graal-native-image-extension", "helidon-mp-graal-native-image-extension"})
  void hasHelidonGraalNativeImageExtension_whenGraalDependencyPresent_thenReturnTrue(String artifactId) {
    // Given
    JavaProject javaProject = createNewJKubeProjectWithDeps("io.helidon.integrations.graal", artifactId);

    // When
    boolean result = HelidonUtils.hasHelidonGraalNativeImageExtension(javaProject);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void hasHelidonGraalNativeImageExtension_whenGraalDependencyAbsent_thenReturnFalse() {
    // Given
    JavaProject javaProject = createNewJKubeProjectWithDeps("io.helidon.integrations.test", "helidon-test-image-extension");

    // When
    boolean result = HelidonUtils.hasHelidonGraalNativeImageExtension(javaProject);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void getHelidonConfiguration_whenApplicationYamlProvided_thenShouldExtractConfigurationAsProperties(@TempDir Path temporaryFolder) throws IOException {
    // Given
    JavaProject javaProject = JavaProject.builder()
        .compileClassPathElements(Collections.singletonList(
            getClass().getResource("/utils-test/config/yaml/").getPath()))
        .outputDirectory(Files.createDirectory(temporaryFolder.resolve("target")).toFile())
        .build();
    // When
    final Properties props = HelidonUtils.getHelidonConfiguration(javaProject);
    // Then
    assertThat(props).containsOnly(
        entry("app.greeting", "Hello"),
        entry("server.port", "8080"),
        entry("server.host", "0.0.0.0"));
  }

  @Test
  void getHelidonConfiguration_whenMicroprofilePropertiesProvided_thenShouldExtractConfigurationAsProperties(@TempDir Path temporaryFolder) throws IOException {
    // Given
    JavaProject javaProject = JavaProject.builder()
        .compileClassPathElements(Collections.singletonList(
            getClass().getResource("/utils-test/config/properties/").getPath()))
        .outputDirectory(Files.createDirectory(temporaryFolder.resolve("target")).toFile())
        .build();
    // When
    final Properties props = HelidonUtils.getHelidonConfiguration(javaProject);
    // Then
    assertThat(props).containsOnly(
        entry("app.greeting", "Hello"),
        entry("server.port", "8080"),
        entry("server.host", "0.0.0.0"));
  }

  @Test
  void getHelidonConfiguration_whenNothingFoundOnClassPath_thenShouldReturnProjectProperties(@TempDir Path temporaryFolder) throws IOException {
    // Given
    Properties properties = new Properties();
    JavaProject javaProject = JavaProject.builder()
        .properties(properties)
        .compileClassPathElements(Collections.emptyList())
        .outputDirectory(Files.createDirectory(temporaryFolder.resolve("target")).toFile())
        .build();
    // When
    final Properties props = HelidonUtils.getHelidonConfiguration(javaProject);
    // Then
    assertThat(props).isEqualTo(properties);
  }

  @Test
  void extractPort_whenServerPortPropertyPresent_thenReturnPort() {
    // Given
    Properties properties = new Properties();
    properties.put("server.port", "8888");
    // When
    String result = HelidonUtils.extractPort(properties, "8080");
    // Then
    assertThat(result).isEqualTo("8888");
  }

  @Test
  void extractPort_whenServerPortPropertyAbsent_thenReturnDefaultPort() {
    // Given
    Properties properties = new Properties();
    // When
    String result = HelidonUtils.extractPort(properties, "8080");
    // Then
    assertThat(result).isEqualTo("8080");
  }

  @Test
  void hasHelidonHealthDependency_whenNoDependency_thenReturnFalse() {
    // Given
    JavaProject javaProject = createNewJKubeProjectWithDeps("io.helidon.webserver", "helidon-webserver");

    // When
    boolean result = HelidonUtils.hasHelidonHealthDependency(javaProject);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void hasHelidonHealthDependency_whenHelidonHealthDependency_thenReturnTrue() {
    // Given
    JavaProject javaProject = createNewJKubeProjectWithDeps("io.helidon.health", "helidon-health");

    // When
    boolean result = HelidonUtils.hasHelidonHealthDependency(javaProject);

    // Then
    assertThat(result).isTrue();
  }

  private JavaProject createNewJKubeProjectWithDeps(String groupId, String artifactId) {
    List<Dependency> dependencyList = Collections.singletonList(Dependency.builder()
        .groupId(groupId)
        .artifactId(artifactId)
        .build());
    return JavaProject.builder()
        .dependencies(dependencyList)
        .dependenciesWithTransitive(dependencyList)
        .build();
  }
}
