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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JKubeProjectUtilTest {

  @TempDir
  File temporaryFolder;

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
    final JavaProject project = mock(JavaProject.class);
    when(project.getDependencies()).thenReturn(Arrays.asList(
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
    final JavaProject project = mock(JavaProject.class);
    when(project.getDependencies()).thenReturn(Arrays.asList(
            Dependency.builder().groupId("io.dep").build(),
            Dependency.builder().groupId("io.dep").artifactId("artifact").version("1.3.37").build(),
            Dependency.builder().groupId("io.other").artifactId("artifact").version("1.3.37").build()
    ));
    // When
    final boolean result = JKubeProjectUtil.hasDependencyWithGroupId(project, "io.nothere");
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void hasTransitiveDependency_whenGroupArtifactMatchesInProvidedDeps_shouldReturnTrue() {
    // Given
    final JavaProject project = mock(JavaProject.class);
    when(project.getDependenciesWithTransitive()).thenReturn(Collections.singletonList(
        Dependency.builder().groupId("org.example").artifactId("artifact").version("1.3.37").build()
    ));

    // When
    final boolean result = JKubeProjectUtil.hasTransitiveDependency(project, "org.example", "artifact");

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void hasTransitiveDependency_whenGroupArtifactNoMatchInProvidedDeps_shouldReturnTrue() {
    // Given
    final JavaProject project = mock(JavaProject.class);

    // When
    final boolean result = JKubeProjectUtil.hasTransitiveDependency(project, "org.example", "artifact");

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void getFinalOutputArtifact_withNothingProvided_returnsNull() {
    // Given
    JavaProject javaProject = JavaProject.builder().build();

    // When
    File finalOutputArtifact = JKubeProjectUtil.getFinalOutputArtifact(javaProject);

    // Then
    assertThat(finalOutputArtifact).isNull();
  }

  @Test
  void getFinalOutputArtifact_withProjectArtifactVersionPackagingBuildDir_returnsInferredArtifact() throws IOException {
    // Given
    File artifactFile = new File(temporaryFolder, "foo-test-1.0.0.jar");
    boolean artifactFileCreated = artifactFile.createNewFile();
    JavaProject javaProject = JavaProject.builder()
        .artifactId("foo-test")
        .version("1.0.0")
        .packaging("jar")
        .buildDirectory(temporaryFolder)
        .build();

    // When
    File finalOutputArtifact = JKubeProjectUtil.getFinalOutputArtifact(javaProject);

    // Then
    assertThat(artifactFileCreated).isTrue();
    assertThat(finalOutputArtifact).hasName("foo-test-1.0.0.jar");
  }

  @Test
  void getFinalOutputArtifact_withBuildFinalNamePackagingBuildDir_returnsInferredArtifact() throws IOException {
    // Given
    File artifactFile = new File(temporaryFolder, "foo-test-final.jar");
    boolean artifactFileCreated = artifactFile.createNewFile();
    JavaProject javaProject = JavaProject.builder()
        .buildFinalName("foo-test-final")
        .packaging("jar")
        .buildDirectory(temporaryFolder)
        .build();

    // When
    File finalOutputArtifact = JKubeProjectUtil.getFinalOutputArtifact(javaProject);

    // Then
    assertThat(artifactFileCreated).isTrue();
    assertThat(finalOutputArtifact).hasName("foo-test-final.jar");
  }

  @Test
  void getFinalOutputArtifact_withArtifactAndBuildFinalNameAndPackaging_returnsInferredArtifact() throws IOException {
    // Given
    File artifactFile = new File(temporaryFolder, "foo-test-1.0.0.jar");
    File buildFinalArtifactFile = new File(temporaryFolder, "foo-test-final.jar");
    boolean buildFinalArtifactFileCreated = buildFinalArtifactFile.createNewFile();
    boolean artifactFileCreated = artifactFile.createNewFile();
    JavaProject javaProject = JavaProject.builder()
        .artifact(artifactFile)
        .buildFinalName("foo-test-final")
        .packaging("jar")
        .buildDirectory(temporaryFolder)
        .build();

    // When
    File finalOutputArtifact = JKubeProjectUtil.getFinalOutputArtifact(javaProject);

    // Then
    assertThat(artifactFileCreated).isTrue();
    assertThat(buildFinalArtifactFileCreated).isTrue();
    assertThat(finalOutputArtifact).hasName("foo-test-final.jar");
  }

  @Test
  void getFinalOutputArtifact_withArtifactAndBuildFinalNameAndPackaging_returnsArtifactFromJavaProject() throws IOException {
    // Given
    File artifactFile = new File(temporaryFolder, "foo-test-1.0.0.jar");
    boolean artifactFileCreated = artifactFile.createNewFile();
    JavaProject javaProject = JavaProject.builder()
        .artifact(artifactFile)
        .buildFinalName("foo-test-final")
        .packaging("jar")
        .build();

    // When
    File finalOutputArtifact = JKubeProjectUtil.getFinalOutputArtifact(javaProject);

    // Then
    assertThat(artifactFileCreated).isTrue();
    assertThat(finalOutputArtifact).hasName("foo-test-1.0.0.jar");
  }

  @Test
  void getProperty_whenSystemPropertyPresent_returnsSystemProperty() {
    // Given
    try {
      System.setProperty("jkube.testProperty", "true");
      // TODO : Replace this when https://github.com/eclipse/jkube/issues/958 gets fixed
      JavaProject javaProject = JavaProject.builder().build();

      // When
      String result = JKubeProjectUtil.getProperty("jkube.testProperty", javaProject);

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
    JavaProject javaProject = JavaProject.builder().properties(properties).build();

    // When
    String result = JKubeProjectUtil.getProperty("jkube.test.project.property", javaProject);

    // Then
    assertThat(result).isEqualTo("true");
  }

  @Test
  void resolveArtifact_whenArtifactPresent_shouldReturnArtifact() {
    // Given
    File actualArtifact = new File(temporaryFolder, "test-artifact-0.0.1.jar");
    JavaProject javaProject = JavaProject.builder()
        .dependency(Dependency.builder()
            .groupId("org.example")
            .artifactId("test-artifact")
            .version("0.0.1")
            .type("jar")
            .file(actualArtifact)
            .build())
        .build();

    // When
    File resolvedArtifact = JKubeProjectUtil.resolveArtifact(javaProject, "org.example", "test-artifact", "0.0.1", "jar");

    // Then
    assertThat(resolvedArtifact).isNotNull().isEqualTo(actualArtifact);
  }

  @Test
  void resolveArtifact_whenNoArtifactPresent_shouldThrowException() {
    // Given
    JavaProject javaProject = JavaProject.builder().build();

    // When
    IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
        () -> JKubeProjectUtil.resolveArtifact(javaProject, "org.example", "test-artifact", "0.0.1", "jar"));

    // Then
    assertThat(illegalStateException).hasMessage("Cannot find artifact test-artifact-0.0.1.jar within the resolved resources");
  }
}
