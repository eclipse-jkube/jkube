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
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.Plugin;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;

@SuppressWarnings({"ResultOfMethodCallIgnored", "unchecked", "unused"})
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
    assertThat(result, equalTo(false));
  }

  @Test
  public void isApplicableWithConfiguredMainClassShouldReturnTrue() {
    // Given
    properties.put("jkube.generator.java-exec.mainClass", "com.example.main");
    // When
    final boolean result = new JavaExecGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result, equalTo(true));
  }

  @Test
  public void isApplicableWithExecPluginShouldReturnTrue() {
    // Given
    plugins.add(Plugin.builder().groupId("org.apache.maven.plugins").artifactId("maven-shade-plugin").build());
    // When
    final boolean result = new JavaExecGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result, equalTo(true));
  }

  @Test
  public void addAssemblyWithNoFatJarShouldAddDefaultFileSets() {
    // Given
    final AssemblyConfiguration.AssemblyConfigurationBuilder builder = AssemblyConfiguration.builder();
    // When
    new JavaExecGenerator(generatorContext).addAssembly(builder);
    // Then
    final AssemblyConfiguration ac = builder.build();
    assertThat(ac.getInline().getFileSets(), hasSize(2));
    assertThat(ac.isExcludeFinalOutputArtifact(), equalTo(false));
  }

  @Test
  public void addAssemblyWithFatJarShouldAddDefaultFileSetsAndFatJar(
      @Mocked FatJarDetector fatJarDetector, @Mocked FatJarDetector.Result fjResult) {
    // Given
    // @formatter:off
    new Expectations() {{
      generatorContext.getProject().getBuildDirectory(); result = new File("");
      generatorContext.getProject().getBaseDirectory(); result = new File("");
      fjResult.getArchiveFile(); result = new File("fat.jar");
      fatJarDetector.scan(); result = fjResult;
    }};
    // @formatter:on
    final AssemblyConfiguration.AssemblyConfigurationBuilder builder = AssemblyConfiguration.builder();
    // When
    new JavaExecGenerator(generatorContext).addAssembly(builder);
    // Then
    final AssemblyConfiguration ac = builder.build();
    assertThat(ac.getInline().getFileSets(), hasSize(3));
    assertThat(ac.getInline().getFileSets(), containsInAnyOrder(
        hasProperty("directory", equalTo(new File("src/main/jkube-includes"))),
        hasProperty("directory", equalTo(new File("src/main/jkube-includes/bin"))),
        hasProperty("includes", containsInAnyOrder("fat.jar"))
    ));
    assertThat(ac.isExcludeFinalOutputArtifact(), equalTo(true));
  }
}
