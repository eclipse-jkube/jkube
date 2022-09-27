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
package org.eclipse.jkube.generator.webapp.handler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TomcatAppServerHandlerTest {

  @TempDir
  File temporaryFolder;

  private GeneratorContext generatorContext;

  private Plugin plugin;

  @BeforeEach
  public void setUp() {
    generatorContext = mock(GeneratorContext.class,RETURNS_DEEP_STUBS);
    plugin = mock(Plugin.class);
  }

  @Test
  void isApplicable_withContextXml_shouldReturnTrue() throws IOException {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder);

    Path metaInf = Files.createDirectories(temporaryFolder.toPath().resolve("META-INF"));
    Files.createFile(metaInf.resolve("context.xml"));

    Path invalidMetaInf = Files.createDirectories(temporaryFolder.toPath().resolve("META-INF-1337"));
    Files.createFile(invalidMetaInf.resolve("context.xml"));
    // When
    final boolean result = new TomcatAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isApplicable_withNoContextXml_shouldReturnFalse() throws IOException {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder);
    Path invalidMetaInf = Files.createDirectories(temporaryFolder.toPath().resolve("META-INF-1337"));
    Files.createFile(invalidMetaInf.resolve("context.xml"));
    // When
    final boolean result = new TomcatAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isApplicable_withTomcat8Plugin_shouldReturnTrue() {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder);
    when(plugin.getGroupId()).thenReturn("org.apache.tomcat.maven");
    when(plugin.getArtifactId()).thenReturn("tomcat8-maven-plugin");
    when(generatorContext.getProject().getPlugins()).thenReturn(Collections.singletonList(plugin));
    // When
    final boolean result = new TomcatAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isApplicable_withInvalidTomcatPlugin_shouldReturnFalse() {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder);
    when(plugin.getGroupId()).thenReturn("org.apache.tomcat.maven");
    when(plugin.getArtifactId()).thenReturn("tomcat1337-maven-plugin");
    when(generatorContext.getProject().getPlugins()).thenReturn(Collections.singletonList(plugin));
    // When
    final boolean result = new TomcatAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isApplicable_withNoPlugin_shouldReturnFalse() {
    // When
    final boolean result = new TomcatAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void handlerSettings() {
    // When
    final TomcatAppSeverHandler handler = new TomcatAppSeverHandler(generatorContext);
    // Then
    assertThat(handler)
        .satisfies(h -> assertThat(h.getFrom()).startsWith("quay.io/jkube/jkube-tomcat:"))
        .returns(Collections.singletonList("8080"), TomcatAppSeverHandler::exposedPorts)
        .returns("/deployments", TomcatAppSeverHandler::getDeploymentDir)
        .returns("deployments", TomcatAppSeverHandler::getAssemblyName)
        .returns("/usr/local/s2i/run", TomcatAppSeverHandler::getCommand)
        .returns(null, TomcatAppSeverHandler::getUser)
        .returns(true, TomcatAppSeverHandler::supportsS2iBuild);
  }
}
