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
package org.eclipse.jkube.kit.common;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.common.util.EnvUtil.isWindows;
import static org.junit.Assume.assumeFalse;

public class JKubeConfigurationTest {

  @Test
  public void getBaseDir_withJavaProject_shouldReturnJavaProjectBaseDirectory() {
    // Given
    final JKubeConfiguration configuration = JKubeConfiguration.builder().project(JavaProject.builder()
        .baseDirectory(new File("base-directory")).build()).build();
    // When
    final File result = configuration.getBasedir();
    // Then
    assertThat(result).isEqualTo(new File("base-directory"));
  }

  @Test
  public void getProperties_withJavaProject_shouldReturnJavaProjectProperties() {
    // Given
    final Properties props = new Properties();
    props.put("property", "value");
    final JKubeConfiguration configuration = JKubeConfiguration.builder().project(JavaProject.builder()
        .properties(props).build()).build();
    // When
    final Properties result = configuration.getProperties();
    // Then
    assertThat(result).hasFieldOrPropertyWithValue("property", "value");
  }

  @Test
  public void inOutputDir_withAbsolutePath_shouldReturnPath() {
    // Given
    assumeFalse(isWindows());
    final JKubeConfiguration configuration = JKubeConfiguration.builder()
        .project(JavaProject.builder().baseDirectory(new File("/")).build())
        .outputDirectory("target").build();
    // When
    final File result = configuration.inOutputDir("/other");
    // Then
    assertThat(result).isEqualTo(new File("/other"));
  }

  @Test
  public void inOutputDir_withRelativePath_shouldReturnResolvedPath() {
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
  public void inSourceDir_withRelativePath_shouldReturnResolvedPath() {
    // Given
    final JKubeConfiguration configuration = JKubeConfiguration.builder()
        .project(JavaProject.builder().baseDirectory(new File("/")).build())
        .sourceDirectory("src").build();
    // When
    final File result = configuration.inSourceDir("other");
    // Then
    assertThat(result).isEqualTo(new File("/src/other"));
  }

  /**
   * Verifies that deserialization works for raw deserialization disregarding annotations.
   */
  @Test
  public void rawDeserialization() throws IOException {
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
