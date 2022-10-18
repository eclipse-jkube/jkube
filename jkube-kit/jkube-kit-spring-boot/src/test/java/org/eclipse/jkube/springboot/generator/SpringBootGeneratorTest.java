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
package org.eclipse.jkube.springboot.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class SpringBootGeneratorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private GeneratorContext context;

  private JavaProject project;

  private SpringBootGenerator springBootGenerator;

  @Before
  public void setUp() throws Exception {
    context = mock(GeneratorContext.class);
    project = mock(JavaProject.class);
    when(context.getProject()).thenReturn(project);
    when(project.getOutputDirectory()).thenReturn(new File(temporaryFolder.newFolder("springboot-test-project").getAbsolutePath()));
    when(project.getPlugins()).thenReturn(Collections.emptyList());
    when(project.getVersion()).thenReturn("1.0.0");
    when(context.getLogger()).thenReturn(new KitLogger.SilentLogger());
    springBootGenerator = new SpringBootGenerator(context);
  }

  @Test
  public void isApplicable_withNoImageConfigurations_shouldReturnFalse() {
    // When
    final boolean result = springBootGenerator.isApplicable(Collections.emptyList());
    // Then
    assertFalse(result);
  }

  @Test
  public void isApplicable_withNoImageConfigurationsAndMavenPlugin_shouldReturnTrue() {
    // Given
    withPlugins(Collections.singletonList(Plugin.builder()
        .groupId("org.springframework.boot")
        .artifactId("spring-boot-maven-plugin")
        .build()));
    // When
    final boolean result = springBootGenerator.isApplicable(Collections.emptyList());
    // Then
    assertTrue(result);
  }

  @Test
  public void isApplicable_withNoImageConfigurationsAndGradlePlugin_shouldReturnTrue() {
    // Given
    withPlugins(Collections.singletonList(Plugin.builder()
        .groupId("org.springframework.boot")
        .artifactId("org.springframework.boot.gradle.plugin")
        .build()));
    // When
    final boolean result = springBootGenerator.isApplicable(Collections.emptyList());
    // Then
    assertTrue(result);
  }

  @Test
  public void getExtraJavaOptions_withDefaults_shouldBeEmpty() {
    // When
    final List<String> result = springBootGenerator.getExtraJavaOptions();
    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void customize_withEmptyList_shouldReturnAddedImage() {
    // When
    final List<ImageConfiguration> configs = springBootGenerator.customize(new ArrayList<>(), true);
    // Then
    assertThat(configs)
        .singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .hasFieldOrPropertyWithValue("ports", Arrays.asList("8080", "8778", "9779"))
        .extracting(BuildConfiguration::getEnv)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .hasSize(1)
        .containsEntry("JAVA_APP_DIR", "/deployments");
  }

  private void withPlugins(List<Plugin> plugins) {
    when(project.getPlugins()).thenReturn(plugins);
  }
}
