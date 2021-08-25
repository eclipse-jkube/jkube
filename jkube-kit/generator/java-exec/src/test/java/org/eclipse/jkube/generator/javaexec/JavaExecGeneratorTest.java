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

import mockit.Expectations;
import mockit.Mocked;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
public class JavaExecGeneratorTest {

  @Mocked
  private GeneratorContext generatorContext;
  private List<Plugin> plugins;
  private Properties properties;

  @Before
  public void setUp() {
    properties = new Properties();
    plugins = new ArrayList<>();
    // @formatter:off
    new Expectations() {{
      generatorContext.getProject().getProperties(); result = properties; minTimes = 0;
      generatorContext.getProject().getPlugins(); result = plugins; minTimes = 0;
    }};
    // @formatter:on
  }

  @Test
  public void isApplicableWithDefaultsShouldReturnFalse() {
    // When
    final boolean result = new JavaExecGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void isApplicableWithConfiguredMainClassShouldReturnTrue() {
    // Given
    properties.put("jkube.generator.java-exec.mainClass", "com.example.main");
    // When
    final boolean result = new JavaExecGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void isApplicableWithExecPluginShouldReturnTrue() {
    // Given
    plugins.add(Plugin.builder().groupId("org.apache.maven.plugins").artifactId("maven-shade-plugin").build());
    // When
    final boolean result = new JavaExecGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void createAssemblyWithNoFatJarShouldAddDefaultFileSets() {
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
  public void createAssemblyWithFatJarShouldAddDefaultFileSetsAndFatJar(
      @Mocked FatJarDetector fatJarDetector, @Mocked FatJarDetector.Result fjResult) {
    // Given
    // @formatter:off
    new Expectations() {{
      generatorContext.getProject().getBuildPackageDirectory(); result = new File("");
      generatorContext.getProject().getBaseDirectory(); result = new File("");
      fjResult.getArchiveFile(); result = new File("fat.jar");
      fatJarDetector.scan(); result = fjResult;
    }};
    // @formatter:on
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
