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
package org.eclipse.jkube.kit.common.util;

import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JKubeProjectUtilTest {

  @TempDir
  File temporaryFolder;

  private JavaProject project;

  private File artifactFile;

  private File finalOutputArtifact;

  @BeforeEach
  void setUp() throws IOException {
    project = JavaProject.builder().build();
    artifactFile = Files.createFile(temporaryFolder.toPath().resolve("foo-test-1.0.0.jar")).toFile();
  }

  @Test
  void hasDependencyWithGroupIdWithNulls() {
    // When
    final boolean result = JKubeProjectUtil.hasDependencyWithGroupId(null, null);
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void hasDependencyWithGroupIdWithDependency() {
    // Given
    project.setDependencies(Arrays.asList(
            Dependency.builder().groupId("io.dep").build(),
            Dependency.builder().groupId("io.dep").artifactId("artifact").version("1.3.37").build(),
            Dependency.builder().groupId("io.other").artifactId("artifact").version("1.3.37").build()
    ));
    // When
    final boolean result = JKubeProjectUtil.hasDependencyWithGroupId(project, "io.dep");
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void hasDependencyWithGroupIdWithNoDependency() {
    // Given
    project.toBuilder()
            .dependencies(Arrays.asList(
            Dependency.builder().groupId("io.dep").build(),
            Dependency.builder().groupId("io.dep").artifactId("artifact").version("1.3.37").build(),
            Dependency.builder().groupId("io.other").artifactId("artifact").version("1.3.37").build()))
            .build();
    // When
    final boolean result = JKubeProjectUtil.hasDependencyWithGroupId(project, "io.nothere");
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void hasTransitiveDependency_whenGroupArtifactMatchesInProvidedDeps_shouldReturnTrue() {
    // Given
    project = project.toBuilder()
            .dependenciesWithTransitive(Collections.singletonList(
            Dependency.builder().groupId("org.example").artifactId("artifact").version("1.3.37").build()))
            .build();

    // When
    final boolean result = JKubeProjectUtil.hasTransitiveDependency(project, "org.example", "artifact");

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void hasTransitiveDependency_whenGroupArtifactNoMatchInProvidedDeps_shouldReturnTrue() {
    // Given
    // When
    final boolean result = JKubeProjectUtil.hasTransitiveDependency(project, "org.example", "artifact");

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void getFinalOutputArtifact_withNothingProvided_returnsNull() {
    // Given
    // When
    finalOutputArtifact = JKubeProjectUtil.getFinalOutputArtifact(project);

    // Then
    assertThat(finalOutputArtifact).isNull();
  }

  @Test
  void getFinalOutputArtifact_withProjectArtifactVersionPackagingBuildDir_returnsInferredArtifact() throws IOException {
    // Given
    project = project.toBuilder()
            .artifactId("foo-test")
            .version("1.0.0")
            .packaging("jar")
            .buildDirectory(temporaryFolder)
            .build();

    // When
    finalOutputArtifact = JKubeProjectUtil.getFinalOutputArtifact(project);

    // Then
    assertThat(finalOutputArtifact).hasName("foo-test-1.0.0.jar");
  }

  @Test
  void getFinalOutputArtifact_withBuildFinalNamePackagingBuildDir_returnsInferredArtifact() throws IOException {
    // Given
    Files.createFile(temporaryFolder.toPath().resolve("foo-test-final.jar"));
    project = project.toBuilder()
            .buildFinalName("foo-test-final")
            .packaging("jar")
            .buildDirectory(temporaryFolder)
            .build();

    // When
    finalOutputArtifact = JKubeProjectUtil.getFinalOutputArtifact(project);

    // Then
    assertThat(finalOutputArtifact).hasName("foo-test-final.jar");
  }

  @Test
  void getFinalOutputArtifact_withArtifactAndBuildFinalNameAndPackaging_returnsInferredArtifact() throws IOException {
    // Given
    Files.createFile(temporaryFolder.toPath().resolve("foo-test-final.jar"));
    project = project.toBuilder()
        .artifact(artifactFile)
        .buildFinalName("foo-test-final")
        .packaging("jar")
        .buildDirectory(temporaryFolder)
        .build();

    // When
    finalOutputArtifact = JKubeProjectUtil.getFinalOutputArtifact(project);

    // Then
    assertThat(finalOutputArtifact).hasName("foo-test-final.jar");
  }

  @Test
  void getFinalOutputArtifact_withArtifactAndBuildFinalNameAndPackaging_returnsArtifactFromJavaProject() throws IOException {
    // Given
    project = project.toBuilder()
        .artifact(artifactFile)
        .buildFinalName("foo-test-final")
        .packaging("jar")
        .build();

    // When
    finalOutputArtifact = JKubeProjectUtil.getFinalOutputArtifact(project);

    // Then
    assertThat(finalOutputArtifact).hasName("foo-test-1.0.0.jar");
  }

  @Test
  void getProperty_whenSystemPropertyPresent_returnsSystemProperty() {
    // Given
    try {
      System.setProperty("jkube.testProperty", "true");
      // TODO : Replace this when https://github.com/eclipse/jkube/issues/958 gets fixed

      // When
      String result = JKubeProjectUtil.getProperty("jkube.testProperty", project);

      // Then
      assertThat(result).isEqualTo("true");
    } finally {
      System.clearProperty("jkube.testProperty");
    }
  }

  @Test
  void getProperty_whenProjectPropertyPresent_returnsProjectProperty() {
    // Given
    Properties properties = new Properties();
    properties.put("jkube.test.project.property", "true");
    project = project.toBuilder().properties(properties).build();

    // When
    String result = JKubeProjectUtil.getProperty("jkube.test.project.property", project);

    // Then
    assertThat(result).isEqualTo("true");
  }

  @Test
  void resolveArtifact_whenArtifactPresent_shouldReturnArtifact() {
    // Given
    project = project.toBuilder()
        .dependency(Dependency.builder()
            .groupId("org.example")
            .artifactId("test-artifact")
            .version("0.0.1")
            .type("jar")
            .file(artifactFile)
            .build())
        .build();

    // When
    File resolvedArtifact = JKubeProjectUtil.resolveArtifact(project, "org.example", "test-artifact", "0.0.1", "jar");

    // Then
    assertThat(resolvedArtifact).isNotNull().isEqualTo(artifactFile);
  }

  @Test
  void resolveArtifact_whenNoArtifactPresent_shouldThrowException() {
    // Given
    // When
    IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
        () -> JKubeProjectUtil.resolveArtifact(project, "org.example", "test-artifact", "0.0.1", "jar"));

    // Then
    assertThat(illegalStateException).hasMessage("Cannot find artifact test-artifact-0.0.1.jar within the resolved resources");
  }
}
