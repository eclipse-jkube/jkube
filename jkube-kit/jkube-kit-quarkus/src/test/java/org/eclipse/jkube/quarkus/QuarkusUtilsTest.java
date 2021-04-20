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
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.eclipse.jkube.kit.common.JavaProject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.jkube.quarkus.QuarkusUtils.extractPort;
import static org.eclipse.jkube.quarkus.QuarkusUtils.getQuarkusConfiguration;

public class QuarkusUtilsTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private JavaProject javaProject;

  @Before
  public void setUp() throws IOException {
    javaProject = JavaProject.builder()
        .properties(new Properties())
        .outputDirectory(temporaryFolder.newFolder())
        .build();
  }

  @Test
  public void extractPort_noProfileAndNoPort_shouldReturnDefault() {
    // When
    final String result = extractPort(javaProject, new Properties(), "80");
    // Then
    assertThat(result).isEqualTo("80");
  }

  @Test
  public void extractPort_noProfileAndPort_shouldReturnPort() {
    // Given
    final Properties properties = new Properties();
    properties.put("quarkus.http.port", "1337");
    // When
    final String result = extractPort(javaProject, properties, "80");
    // Then
    assertThat(result).isEqualTo("1337");
  }

  @Test
  public void extractPort_inactiveProfileAndPort_shouldReturnPort() {
    // Given
    final Properties properties = new Properties();
    properties.put("quarkus.http.port", "1337");
    properties.put("%dev.quarkus.http.port", "31337");
    // When
    final String result = extractPort(javaProject, properties, "80");
    // Then
    assertThat(result).isEqualTo("1337");
  }

  @Test
  public void extractPort_activeProfileAndPort_shouldReturnProfilePort() {
    // Given
    final Properties properties = new Properties();
    properties.put("quarkus.http.port", "1337");
    properties.put("%dev.quarkus.http.port", "31337");
    javaProject.getProperties().put("quarkus.profile", "dev");
    // When
    final String result = extractPort(javaProject, properties, "80");
    // Then
    assertThat(result).isEqualTo("31337");
  }

  @Test
  public void getQuarkusConfiguration_propertiesAndYamlProjectProperties_shouldUseProjectProperties() {
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
  public void getQuarkusConfiguration_propertiesAndYaml_shouldUseProperties() {
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
  public void getQuarkusConfiguration_yamlOnly_shouldUseYaml() {
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
  public void getQuarkusConfiguration_noConfigFiles_shouldReturnEmpty() {
    // Given
    javaProject.setCompileClassPathElements(Collections.singletonList(
        QuarkusUtilsTest.class.getResource("/").getPath()
    ));
    // When
    final Properties props = getQuarkusConfiguration(javaProject);
    // Then
    assertThat(props).isEmpty();
  }
}
