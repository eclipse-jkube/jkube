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
package org.eclipse.jkube.gradle.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.common.JavaProject;

import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.plugins.JavaPluginConvention;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.eclipse.jkube.gradle.plugin.GradleUtil.convertGradleProject;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public class GradleUtilTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private Project project;
  private JavaPluginConvention javaPlugin;

  @Before
  public void setUp() throws IOException {
    project = mock(Project.class, RETURNS_DEEP_STUBS);
    javaPlugin = mock(JavaPluginConvention.class, RETURNS_DEEP_STUBS);
    when(project.getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getBuildscript().getConfigurations().stream()).thenAnswer(i -> Stream.empty());
    when(project.getProperties()).thenReturn(Collections.emptyMap());
    when(project.getBuildDir()).thenReturn(folder.newFolder("build"));
    when(project.getConvention().getPlugin(JavaPluginConvention.class)).thenReturn(javaPlugin);
  }

  @Test
  public void extractProperties_withComplexMap_shouldReturnValidProperties() {
    // Given
    final Map<String, Object> complexProperties = new HashMap<>();
    when(project.getProperties()).thenAnswer(i -> complexProperties);
    complexProperties.put("property.1", "test");
    complexProperties.put("property.ignored", null);
    complexProperties.put("object", Collections.singletonMap("field1", 1));
    // When
    final JavaProject result = convertGradleProject(project);
    // Then
    assertThat(result.getProperties())
        .isNotEmpty()
        .hasFieldOrPropertyWithValue("object.field1", 1)
        .hasFieldOrPropertyWithValue("property.1", null)
        .hasEntrySatisfying("property.1", e -> assertThat(e).isEqualTo("test"));
  }

  @Test
  public void extractProperties_shouldContainSystemProperties() {
    // Given
    System.setProperty("foo.property", "somevalue");
    when(project.getProperties()).thenReturn(Collections.emptyMap());

    // When
    final JavaProject result = convertGradleProject(project);

    // Then
    assertThat(result.getProperties())
      .hasFieldOrPropertyWithValue("foo.property", null)
      .hasEntrySatisfying("foo.property", e -> assertThat(e).isEqualTo("somevalue"));
    System.clearProperty("foo.property");
  }

  @Test
  public void extractProperties_whenBothSystemAndGradlePropertyProvided_thenSystemPropertyShouldHaveMorePrecedence() {
    // Given
    final Map<String, Object> gradleProperties = new HashMap<>();
    gradleProperties.put("foo.property", "gradlevalue");
    System.setProperty("foo.property", "systemvalue");
    when(project.getProperties()).thenAnswer(i -> gradleProperties);

    // When
    final JavaProject result = convertGradleProject(project);

    // Then
    assertThat(result.getProperties())
      .hasFieldOrPropertyWithValue("foo.property", null)
      .hasEntrySatisfying("foo.property", e -> assertThat(e).isEqualTo("systemvalue"));
    System.clearProperty("foo.property");
  }

  @Test
  public void extractDependencies_withMultipleAndDuplicateDependencies_shouldReturnValidDependencies() {
    // Given
    final ConfigurationContainer cc = mock(ConfigurationContainer.class);
    when(project.getConfigurations()).thenReturn(cc);
    final Function<String[], Configuration> mockConfiguration = configurationDependencyMock();
    when(cc.stream()).thenAnswer(i -> Stream.of(
        mockConfiguration.apply(new String[] {"com.example", "artifact", null}),
        mockConfiguration.apply(new String[] {"com.example.sub", "duplicate-artifact", "1.33.7"}),
        mockConfiguration.apply(new String[] {"com.example.sub", "duplicate-artifact", "1.33.7"}),
        mockConfiguration.apply(new String[] {"com.example", "other.artifact", "1.0.0"}),
        mockConfiguration.apply(new String[] {"com.example", "other.artifact", "1.1.0"})));
    // When
    final JavaProject result = convertGradleProject(project);
    // Then
    assertThat(result.getDependencies())
        .hasSize(4)
        .extracting("groupId", "artifactId", "version")
        .containsExactlyInAnyOrder(
            tuple("com.example", "artifact", null),
            tuple("com.example.sub", "duplicate-artifact", "1.33.7"),
            tuple("com.example", "other.artifact", "1.0.0"),
            tuple("com.example", "other.artifact", "1.1.0")
        );
  }

  @Test
  public void extractPlugins_withMultipleAndBuildScriptDuplicateDependencies_shouldReturnValidPlugins() {
    // Given
    final ConfigurationContainer cc = mock(ConfigurationContainer.class);
    when(project.getBuildscript().getConfigurations()).thenReturn(cc);
    final Function<String[], Configuration> mockConfiguration = configurationDependencyMock();
    when(cc.stream()).thenAnswer(i -> Stream.of(
        mockConfiguration.apply(new String[] {"org.springframework.boot", "org.springframework.boot.gradle.plugin", "1.33.7"}),
        mockConfiguration.apply(new String[] {"org.springframework.boot", "org.springframework.boot.gradle.plugin", "1.33.7"}),
        mockConfiguration.apply(new String[] {"com.example", "not-a-plugin", "1.33.7"}),
        mockConfiguration.apply(new String[] {"org.eclipse.jkube.kubernetes", "org.eclipse.jkube.kubernetes.gradle.plugin", "1.0.0"})));
    // When
    final JavaProject result = convertGradleProject(project);
    // Then
    assertThat(result.getPlugins())
        .hasSize(2)
        .extracting("groupId", "artifactId", "version")
        .containsExactlyInAnyOrder(
            tuple("org.springframework.boot", "org.springframework.boot.gradle.plugin", "1.33.7"),
            tuple("org.eclipse.jkube.kubernetes", "org.eclipse.jkube.kubernetes.gradle.plugin", "1.0.0")
        );
  }

  @Test
  public void findClassesOutputDirectory_withNotFoundSourceSet_shouldReturnDefault() {
    // Given
    when(javaPlugin.getSourceSets().getByName("main")).thenThrow(new UnknownDomainObjectException("Not found"));
    // When
    final JavaProject result = convertGradleProject(project);
    // Then
    assertThat(result.getOutputDirectory())
        .isEqualTo(folder.getRoot().toPath().resolve("build").resolve("classes").resolve("java").resolve("main").toFile());
  }

  @Test
  public void findClassesOutputDirectory_withValidSourceSet_shouldReturnFromSourceSet() {
    // Given
    when(javaPlugin.getSourceSets().getByName("main").getJava().getDestinationDirectory().getAsFile())
        .thenReturn(new DefaultProvider<>(() -> new File("classes")));
    // When
    final JavaProject result = convertGradleProject(project);
    // Then
    assertThat(result.getOutputDirectory()).isEqualTo(new File("classes"));
  }

  @Test
  public void findClassesOutputDirectory_withNoSourceSets_shouldReturnDefault() {
    // Given
    when(javaPlugin.getSourceSets()).thenReturn(null);
    // When
    final JavaProject result = convertGradleProject(project);
    // Then
    assertThat(result.getOutputDirectory())
        .isEqualTo(folder.getRoot().toPath().resolve("build").resolve("classes").resolve("java").resolve("main").toFile());
  }

  @Test
  public void findArtifact_withExistentFile_shouldReturnValidArtifact() throws IOException {
    // Given
    final Configuration c = mock(Configuration.class, RETURNS_DEEP_STUBS);
    when(c.getAllDependencies().stream()).thenAnswer(i -> Stream.empty());
    when(c.getOutgoing().getArtifacts().getFiles().getFiles()).thenReturn(Stream.of(
        folder.newFile("final-artifact.jar")
    ).collect(Collectors.toSet()));
    when(project.getConfigurations().stream()).thenAnswer(i -> Stream.of(c));
    // When
    final JavaProject result = convertGradleProject(project);
    // Then
    assertThat(result.getArtifact()).isNotNull().hasName("final-artifact.jar");
  }

  @Test
  public void findArtifact_withMultipleExistentFiles_shouldReturnArtifactWithLargestSize() throws IOException {
    // Given
    final Configuration c = mock(Configuration.class, RETURNS_DEEP_STUBS);
    File jar1 = folder.newFile("final-artifact.jar");
    Files.write(jar1.toPath(), "FatJar".getBytes());
    File jar2 = folder.newFile("final-artifact-plain.jar");
    when(c.getAllDependencies().stream()).thenAnswer(i -> Stream.empty());
    when(c.getOutgoing().getArtifacts().getFiles().getFiles()).thenReturn(Stream.of(
        jar2, jar1
    ).collect(Collectors.toSet()));
    when(project.getConfigurations().stream()).thenAnswer(i -> Stream.of(c));
    // When
    final JavaProject result = convertGradleProject(project);
    // Then
    assertThat(result.getArtifact()).isNotNull().hasName("final-artifact.jar");
  }

  private static Function<String[], Configuration> configurationDependencyMock() {
    return s -> {
      final Configuration c = mock(Configuration.class, RETURNS_DEEP_STUBS);
      final Dependency d = mock(Dependency.class);
      when(c.getAllDependencies().stream()).thenAnswer(i -> Stream.of(d));
      when(c.getOutgoing().getArtifacts().getFiles().getFiles()).thenReturn(Collections.emptySet());
      when(d.getGroup()).thenReturn(s[0]);
      when(d.getName()).thenReturn(s[1]);
      when(d.getVersion()).thenReturn(s[2]);
      return c;
    };
  }
}