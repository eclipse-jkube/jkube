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
package org.eclipse.jkube.quarkus.generator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class QuarkusGeneratorExtractPortsTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mocked
  private GeneratorContext ctx;

  @Mocked
  private JavaProject project;

  private File target;
  private List<String> compileClassPathElements;
  private Properties projectProperties;

  @Before
  public void setUp() throws IOException {
    target = temporaryFolder.newFolder("target");
    compileClassPathElements = new ArrayList<>();
    projectProperties = new Properties();
    // @formatter:off
    new Expectations() {{
      ctx.getProject(); result = project;
      project.getCompileClassPathElements(); result = compileClassPathElements;
      project.getOutputDirectory(); result = target;
      project.getProperties(); result = projectProperties;
    }};
    // @formatter:on
  }

  @Test
  public void extractPorts_withDefaults_shouldAddDefaults() {
    // When
    final List<String> result = new QuarkusGenerator(ctx).extractPorts();
    // Then
    assertThat(result).containsExactly("8080", "8778", "9779");
  }

  @Test
  public void extractPorts_withDefaultsInNative_shouldAddDefaultsForNative() {
    // Given
    projectProperties.put("jkube.generator.quarkus.nativeImage", "true");
    // When
    final List<String> result = new QuarkusGenerator(ctx).extractPorts();
    // Then
    assertThat(result).containsExactly("8080");
  }

  @Test
  public void extractPorts_withApplicationProperties_shouldAddConfigured() {
    // Given
    compileClassPathElements.add(
        QuarkusGeneratorExtractPortsTest.class.getResource("/generator-extract-ports").getPath());
    // When
    final List<String> result = new QuarkusGenerator(ctx).extractPorts();
    // Then
    assertThat(result).containsExactly("1337", "8778", "9779");
  }

  @Test
  public void extractPorts_withApplicationPropertiesAndProfile_shouldAddConfiguredProfile() {
    // Given
    projectProperties.put("quarkus.profile", "dev");
    compileClassPathElements.add(
        QuarkusGeneratorExtractPortsTest.class.getResource("/generator-extract-ports").getPath());
    // When
    final List<String> result = new QuarkusGenerator(ctx).extractPorts();
    // Then
    assertThat(result).containsExactly("31337", "8778", "9779");
  }
}
