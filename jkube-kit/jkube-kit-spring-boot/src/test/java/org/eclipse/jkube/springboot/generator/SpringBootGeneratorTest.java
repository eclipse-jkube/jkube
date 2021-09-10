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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class SpringBootGeneratorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mocked
  private GeneratorContext context;

  @Mocked
  private JavaProject project;

  private SpringBootGenerator springBootGenerator;

  @Before
  public void setUp() throws Exception {
    // @formatter:off
    new Expectations() {{
      context.getProject(); result = project;
      project.getOutputDirectory(); result = temporaryFolder.newFolder("springboot-test-project").getAbsolutePath();
      project.getPlugins(); result = Collections.emptyList(); minTimes = 0;
      project.getVersion(); result = "1.0.0"; minTimes = 0;
    }};
    // @formatter:on
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
    assertEquals(1, configs.size());
    Map<String, String> env = configs.get(0).getBuildConfiguration().getEnv();
    assertNull(env.get("JAVA_OPTIONS"));
  }

  private void withPlugins(List<Plugin> plugins) {
    // @formatter:off
    new Expectations() {{
      project.getPlugins(); result = plugins;
    }};
    // @formatter:on
  }
}
