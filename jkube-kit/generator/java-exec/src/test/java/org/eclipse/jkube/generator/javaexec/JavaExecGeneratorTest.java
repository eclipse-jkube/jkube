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
package org.eclipse.jkube.generator.javaexec;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class JavaExecGeneratorTest {
  private JavaProject project;
  private GeneratorContext generatorContext;
  private Properties properties;


  @BeforeEach
  void setUp() {
    properties = new Properties();
    project = JavaProject.builder()
      .properties(properties)
      .version("0.0.1")
      .build();
    generatorContext = GeneratorContext.builder()
      .logger(new KitLogger.SilentLogger())
      .project(project)
      .build();
  }

  @Test
  void isApplicableWithDefaultsShouldReturnFalse() {
    // When
    final boolean result = new JavaExecGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isApplicableWithConfiguredMainClassShouldReturnTrue() {
    // Given
    properties.put("jkube.generator.java-exec.mainClass", "com.example.main");
    // When
    final boolean result = new JavaExecGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isApplicableWithExecPluginShouldReturnTrue() {
    // Given
    project.setPlugins(Collections.singletonList(
      Plugin.builder().groupId("org.apache.maven.plugins").artifactId("maven-shade-plugin").build()));
    // When
    final boolean result = new JavaExecGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void createAssemblyWithNoFatJarShouldAddDefaultFileSets() {
    // When
    final AssemblyConfiguration result = new JavaExecGenerator(generatorContext).createAssembly();
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", false)
        .extracting(AssemblyConfiguration::getLayers).asList().hasSize(1)
        .first().asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
        .extracting(Assembly::getFileSets).asList()
        .hasSize(2);
  }

  @Test
  void createAssemblyWithFatJarShouldAddDefaultFileSetsAndFatJar() throws Exception {
    // Given
    final Path fatJarDirectory = Paths.get(JavaExecGeneratorTest.class.getResource("/fatjar-simple").toURI());
    generatorContext = generatorContext.toBuilder()
        .project(project.toBuilder()
          .buildPackageDirectory(fatJarDirectory.toFile())
          .baseDirectory(fatJarDirectory.getParent().toFile())
          .build())
        .build();
    // When
    final AssemblyConfiguration result = new JavaExecGenerator(generatorContext).createAssembly();
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", true)
        .extracting(AssemblyConfiguration::getLayers).asList().hasSize(1)
        .first().asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
        .extracting(Assembly::getFileSets).asList()
        .hasSize(3)
        .extracting("directory", "includes")
        .containsExactlyInAnyOrder(
            tuple(new File("src/main/jkube-includes"), Collections.emptyList()),
            tuple(new File("src/main/jkube-includes/bin"), Collections.emptyList()),
            tuple(new File("fatjar-simple"), Collections.singletonList("fat.jar"))
        );
  }

  @Test
  void customize_whenInvoked_shouldAddLabelsToBuildConfiguration() {
    // Given
    properties.put("jkube.generator.java-exec.mainClass", "org.example.Foo");

    // When
    final List<ImageConfiguration> result = new JavaExecGenerator(generatorContext).customize(new ArrayList<>(), false);

    // Then
    assertThat(result)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getLabels)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsKeys("org.label-schema.build-date", "org.label-schema.description", "org.label-schema.version",
            "org.label-schema.schema-version", "org.label-schema.build-date", "org.label-schema.name");
  }

  @Test
  void customize_whenInvoked_shouldNotAddBuildTimestampToBuildDateLabel() {
    // Given
    properties.put("jkube.generator.java-exec.mainClass", "org.example.Foo");
    generatorContext = generatorContext.toBuilder()
      .project(project.toBuilder()
        .buildDate(LocalDate.of(2015, 10, 21))
        .build())
      .build();
    // When
    final List<ImageConfiguration> result = new JavaExecGenerator(generatorContext).customize(new ArrayList<>(), false);
    // Then
    assertThat(result)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getLabels)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("org.label-schema.build-date", "2015-10-21");
  }
}
