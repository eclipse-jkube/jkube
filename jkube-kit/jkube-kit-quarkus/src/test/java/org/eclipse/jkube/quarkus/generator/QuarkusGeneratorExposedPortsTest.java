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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import org.eclipse.jkube.quarkus.QuarkusUtilsTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.quarkus.TestUtils.getResourceTestFilePath;
import static org.eclipse.jkube.quarkus.generator.QuarkusGeneratorTest.withFastJarInTarget;
import static org.eclipse.jkube.quarkus.generator.QuarkusGeneratorTest.withNativeBinaryInTarget;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QuarkusGeneratorExposedPortsTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private GeneratorContext ctx;

  private File target;
  private List<String> compileClassPathElements;
  private Properties projectProperties;

  @Before
  public void setUp() throws IOException {
    target = temporaryFolder.newFolder("target");
    compileClassPathElements = new ArrayList<>();
    ctx = mock(GeneratorContext.class, RETURNS_DEEP_STUBS);
    projectProperties = new Properties();
    when(ctx.getProject().getProperties()).thenReturn(projectProperties);
    when(ctx.getProject().getVersion()).thenReturn("1.33.7-SNAPSHOT");
    when(ctx.getProject().getCompileClassPathElements()).thenReturn(compileClassPathElements);
    when(ctx.getProject().getBaseDirectory()).thenReturn(target);
    when(ctx.getProject().getBuildDirectory()).thenReturn(target);
    when(ctx.getProject().getOutputDirectory()).thenReturn(target);
  }

  @Test
  public void withDefaults_shouldAddDefaults() throws IOException {
    // Given
    withFastJarInTarget(target);
    // When
    final List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), false);
    // Then
    assertThat(result).singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getPorts)
        .asList()
        .containsExactly("8080", "8778", "9779");
  }

  @Test
  public void withDefaultsInNative_shouldAddDefaultsForNative() throws IOException {
    // Given
    withNativeBinaryInTarget(target);
    // When
    final List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), false);
    // Then
    assertThat(result).singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getPorts)
        .asList()
        .containsExactly("8080");
  }

  @Test
  public void withApplicationProperties_shouldAddConfigured() throws IOException, URISyntaxException {
    // Given
    withFastJarInTarget(target);
    compileClassPathElements.add(
            getResourceTestFilePath(QuarkusGeneratorExposedPortsTest.class, "/generator-extract-ports")
    );
    // When
    final List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), false);
    // Then
    assertThat(result).singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getPorts)
        .asList()
        .containsExactly("1337", "8778", "9779");
  }

  @Test
  public void withApplicationPropertiesAndProfile_shouldAddConfiguredProfile() throws IOException, URISyntaxException {
    // Given
    withFastJarInTarget(target);
    projectProperties.put("quarkus.profile", "dev");
    compileClassPathElements.add(
            getResourceTestFilePath(QuarkusGeneratorExposedPortsTest.class, "/generator-extract-ports")
    );
    // When
    final List<ImageConfiguration> result = new QuarkusGenerator(ctx).customize(new ArrayList<>(), false);
    // Then
    assertThat(result).singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getPorts)
        .asList()
        .containsExactly("31337", "8778", "9779");
  }
}
