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
package org.eclipse.jkube.generator.javaexec;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.Plugin;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JavaExecGeneratorTest {
  private GeneratorContext generatorContext;
  private List<Plugin> plugins;
  private Properties properties;


  @BeforeEach
  void setUp() {
    generatorContext = mock(GeneratorContext.class,RETURNS_DEEP_STUBS);
    properties = new Properties();
    plugins = new ArrayList<>();
    when(generatorContext.getProject().getProperties()).thenReturn(properties);
    when(generatorContext.getProject().getPlugins()).thenReturn(plugins);
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
    plugins.add(Plugin.builder().groupId("org.apache.maven.plugins").artifactId("maven-shade-plugin").build());
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
  void createAssemblyWithFatJarShouldAddDefaultFileSetsAndFatJar() {
    // Given
    FatJarDetector fatJarDetector = mock(FatJarDetector.class);
    FatJarDetector.Result fjResult = mock(FatJarDetector.Result.class);
    when(generatorContext.getProject().getBuildPackageDirectory()).thenReturn(new File(""));
    when(generatorContext.getProject().getBaseDirectory()).thenReturn(new File(""));
    when(fjResult.getArchiveFile()).thenReturn(new File("fat.jar"));
    when(fatJarDetector.scan()).thenReturn(fjResult);
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
            tuple(new File(""), Collections.singletonList("fat.jar"))
        );
  }
}
