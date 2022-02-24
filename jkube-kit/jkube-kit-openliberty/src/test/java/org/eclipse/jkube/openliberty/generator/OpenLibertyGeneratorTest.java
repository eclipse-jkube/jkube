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
package org.eclipse.jkube.openliberty.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.FatJarDetector;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class OpenLibertyGeneratorTest {

  private GeneratorContext context;
  private Properties projectProperties;

  @Before
  public void setUp() {
    context = mock(GeneratorContext.class, RETURNS_DEEP_STUBS);
    projectProperties = new Properties();
    when(context.getProject().getProperties()).thenReturn(projectProperties);
    when(context.getProject().getVersion()).thenReturn("1.33.7-SNAPSHOT");
  }

  @Test
  public void getEnvWithFatJar() {
    try (MockedConstruction<FatJarDetector> ignore = mockConstruction(FatJarDetector.class,
        withSettings().defaultAnswer(RETURNS_DEEP_STUBS),
        (mock, ctx) -> {
          // Given
          when(mock.scan().getArchiveFile()).thenReturn(new File("/the/archive/file.jar"));
          when(mock.scan().getMainClass()).thenReturn("wlp.lib.extract.SelfExtractRun");
        })) {
      // When
      final Map<String, String> result = new OpenLibertyGenerator(context).getEnv(false);
      // Then
      assertThat(result)
          .hasSize(2)
          .containsEntry("LIBERTY_RUNNABLE_JAR", "file.jar")
          .containsEntry("JAVA_APP_JAR", "file.jar");
    }
  }

  @Test
  public void getEnvWithoutFatJar() {
    // Given
    projectProperties.put("jkube.generator.openliberty.mainClass", "com.example.MainClass");
    projectProperties.put("jkube.generator.java-exec.mainClass", "com.example.MainNotApplicable");
    // When
    final Map<String, String> result = new OpenLibertyGenerator(context).getEnv(false);
    // Then
    assertThat(result)
        .hasSize(1)
        .containsEntry("JAVA_MAIN_CLASS", "com.example.MainClass");
  }

  @Test
  public void getDefaultWebPort_overridesDefault() {
    // Given
    projectProperties.put("jkube.generator.openliberty.mainClass", "com.example.Main");
    // When
    final List<ImageConfiguration> result = new OpenLibertyGenerator(context)
        .customize(new ArrayList<>(), false);
    // Then
    assertThat(result).singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getPorts)
        .asList()
        .containsExactly("9080", "8778", "9779");
  }

  @Test
  public void addAdditionalFiles() {
    // When
    final List<AssemblyFileSet> result = new OpenLibertyGenerator(context).addAdditionalFiles();
    // Then
    assertThat(result)
        .extracting(AssemblyFileSet::getDirectory)
        .containsExactlyInAnyOrder(
            new File("src/main/jkube-includes"),
            new File("src/main/jkube-includes/bin"),
            new File("src/main/liberty/config")
        );
  }
}
