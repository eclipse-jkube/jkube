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
package org.eclipse.jkube.micronaut.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;

import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class MicronautGeneratorTest {

  private GeneratorContext ctx;
  private MicronautGenerator micronautGenerator;

  @BeforeEach
  void setUp() {
    final Properties projectProperties = new Properties();
    projectProperties.put("jkube.generator.micronaut.mainClass", "com.example.Main");
    ctx = GeneratorContext.builder()
      .logger(new KitLogger.SilentLogger())
      .project(JavaProject.builder()
        .version("1.33.7-SNAPSHOT")
        .properties(projectProperties)
        .build())
      .build();
    micronautGenerator = new MicronautGenerator(ctx);
  }

  @Test
  void isApplicableWithNoPlugin() {
    // When
    final boolean result = micronautGenerator.isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isApplicableWithMavenPlugin() {
    // Given
    ctx.getProject().setPlugins(Collections.singletonList(Plugin.builder()
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
  void isApplicableWithGradlePlugin() {
    // Given
    ctx.getProject().setPlugins(Collections.singletonList(Plugin.builder()
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
  void customize_webPortIsFirst(@TempDir File outputDirectory) {
    // Given
    ctx.getProject().setCompileClassPathElements(Collections.emptyList());
    ctx.getProject().setOutputDirectory(outputDirectory);

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
