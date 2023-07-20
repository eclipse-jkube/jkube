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
package org.eclipse.jkube.kit.common;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class JKubeConfigurationTest {

  @Test
  void getBaseDir_withJavaProject_shouldReturnJavaProjectBaseDirectory() {
    // Given
    final JKubeConfiguration configuration = JKubeConfiguration.builder().project(JavaProject.builder()
        .baseDirectory(new File("base-directory")).build()).build();
    // When
    final File result = configuration.getBasedir();
    // Then
    assertThat(result).isEqualTo(new File("base-directory"));
  }

  @Test
  void getProperties_withJavaProject_shouldReturnJavaProjectProperties() {
    // Given
    final Properties props = new Properties();
    props.put("property", "value");
    final JKubeConfiguration configuration = JKubeConfiguration.builder().project(JavaProject.builder()
        .properties(props).build()).build();
    // When
    final Properties result = configuration.getProperties();
    // Then
    assertThat(result).containsOnly(entry("property", "value"));
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  void inOutputDir_withAbsolutePath_shouldReturnPath() {
    // Given
    final JKubeConfiguration configuration = JKubeConfiguration.builder()
        .project(JavaProject.builder().baseDirectory(new File("/")).build())
        .outputDirectory("target").build();
    // When
    final File result = configuration.inOutputDir("/other");
    // Then
    assertThat(result).isEqualTo(new File("/other"));
  }

  @Test
  void inOutputDir_withRelativePath_shouldReturnResolvedPath() {
    // Given
    final JKubeConfiguration configuration = JKubeConfiguration.builder()
        .project(JavaProject.builder().baseDirectory(new File("/base")).build())
        .outputDirectory("target").build();
    // When
    final File result = configuration.inOutputDir("other");
    // Then
    assertThat(result).isEqualTo(new File("/base/target/other"));
  }

  @Test
  void inSourceDir_withRelativePath_shouldReturnResolvedPath() {
    // Given
    final JKubeConfiguration configuration = JKubeConfiguration.builder()
        .project(JavaProject.builder().baseDirectory(new File("/")).build())
        .sourceDirectory("src").build();
    // When
    final File result = configuration.inSourceDir("other");
    // Then
    assertThat(result).isEqualTo(new File("/src/other"));
  }

  @Test
  void builder() {
    // Given
    JKubeConfiguration.JKubeConfigurationBuilder builder = JKubeConfiguration.builder()
        .project(JavaProject.builder().artifactId("test-project").build())
        .sourceDirectory("src/main/jkube")
        .outputDirectory("target")
        .buildArgs(Collections.singletonMap("foo", "bar"))
        .registryConfig(RegistryConfig.builder()
            .registry("r.example.com")
            .build());

    // When
    JKubeConfiguration jKubeConfiguration = builder.build();

    // Then
    assertThat(jKubeConfiguration)
        .hasFieldOrPropertyWithValue("project.artifactId", "test-project")
        .hasFieldOrPropertyWithValue("sourceDirectory", "src/main/jkube")
        .hasFieldOrPropertyWithValue("outputDirectory", "target")
        .hasFieldOrPropertyWithValue("buildArgs", Collections.singletonMap("foo", "bar"))
        .hasFieldOrPropertyWithValue("registryConfig.registry", "r.example.com");
  }

  /**
   * Verifies that deserialization works for raw deserialization disregarding annotations.
   */
  @Test
  void rawDeserialization() throws IOException {
    // Given
    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(MapperFeature.USE_ANNOTATIONS, false);
    // When
    final JKubeConfiguration result = mapper.readValue(
        JKubeConfigurationTest.class.getResourceAsStream("/jkube-configuration.json"),
        JKubeConfiguration.class
    );
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("project", null)
        .hasFieldOrPropertyWithValue("sourceDirectory", "src")
        .hasFieldOrPropertyWithValue("outputDirectory", "target")
        .hasFieldOrPropertyWithValue("buildArgs.http_proxy", "127.0.0.1:8001")
        .hasFieldOrPropertyWithValue("registryConfig.registry", "the-registry")
        .extracting(JKubeConfiguration::getReactorProjects).asList().isEmpty();
  }
}
