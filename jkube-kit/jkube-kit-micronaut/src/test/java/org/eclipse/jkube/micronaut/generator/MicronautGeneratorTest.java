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
package org.eclipse.jkube.micronaut.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Plugin;

import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MicronautGeneratorTest {

  private GeneratorContext ctx;
  private MicronautGenerator micronautGenerator;

  @Before
  public void setUp() {
    ctx = mock(GeneratorContext.class, RETURNS_DEEP_STUBS);
    final Properties projectProperties = new Properties();
    projectProperties.put("jkube.generator.micronaut.mainClass", "com.example.Main");
    when(ctx.getProject().getProperties()).thenReturn(projectProperties);
    when(ctx.getProject().getVersion()).thenReturn("1.33.7-SNAPSHOT");
    micronautGenerator = new MicronautGenerator(ctx);
  }

  @Test
  public void isApplicableWithNoPlugin() {
    // When
    final boolean result = micronautGenerator.isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void isApplicableWithMavenPlugin() {
    // Given
    when(ctx.getProject().getPlugins()).thenReturn(Collections.singletonList(Plugin.builder()
        .groupId("io.micronaut.build")
        .artifactId("micronaut-maven-plugin")
        .build()
    ));
    // When
    final boolean result = micronautGenerator.isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void isApplicableWithGradlePlugin() {
    // Given
    when(ctx.getProject().getPlugins()).thenReturn(Collections.singletonList(Plugin.builder()
        .groupId("io.micronaut.application")
        .artifactId("io.micronaut.application.gradle.plugin")
        .build()
    ));
    // When
    final boolean result = micronautGenerator.isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void customize_webPortIsFirst() {
    // Given
    when(ctx.getProject().getCompileClassPathElements()).thenReturn(Collections.emptyList());
    when(ctx.getProject().getOutputDirectory()).thenReturn(new File("MOCK"));

    // When
    final List<ImageConfiguration> result = micronautGenerator.customize(new ArrayList<>(), false);
    // Then
    assertThat(result).singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getPorts)
        .asList()
        .containsExactly("8080", "8778", "9779");
  }

}
