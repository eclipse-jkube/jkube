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

class JettyAppServerHandlerTest {

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
  void isApplicable_withJettyLogging_shouldReturnTrue() throws IOException {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder);

    Path metaInf = Files.createDirectories(temporaryFolder.toPath().resolve("META-INF"));
    Files.createFile(metaInf.resolve("jetty-logging.properties"));

    Path invalidMetaInf = Files.createDirectories(temporaryFolder.toPath().resolve("META-INF-1337"));
    Files.createFile(invalidMetaInf.resolve("context.xml"));
    // When
    final boolean result = new JettyAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isApplicable_withNotJettyLogging_shouldReturnFalse() throws IOException {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder);
    Path invalidMetaInf = Files.createDirectories(temporaryFolder.toPath().resolve("META-INF-1337"));
    Files.createFile(invalidMetaInf.resolve("context.xml"));
    // When
    final boolean result = new JettyAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isApplicable_withJettyMavenPlugin_shouldReturnTrue() {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder);
    when(plugin.getGroupId()).thenReturn("org.eclipse.jetty");
    when(plugin.getArtifactId()).thenReturn("jetty-maven-plugin");
    when(generatorContext.getProject().getPlugins()).thenReturn(Collections.singletonList(plugin));
    // When
    final boolean result = new JettyAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isApplicable_withInvalidJettyMavenPlugin_shouldReturnFalse() {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder);
    when(plugin.getGroupId()).thenReturn("org.eclipse.jetty");
    when(plugin.getArtifactId()).thenReturn("jetty1337-maven-plugin");
    when(generatorContext.getProject().getPlugins()).thenReturn(Collections.singletonList(plugin));
    // When
    final boolean result = new JettyAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void handlerSettings() {
    // When
    final JettyAppSeverHandler handler = new JettyAppSeverHandler(generatorContext);
    // Then
    assertThat(handler)
        .satisfies(h -> assertThat(h.getFrom()).startsWith("quay.io/jkube/jkube-jetty9:"))
        .returns(Collections.singletonList("8080"), JettyAppSeverHandler::exposedPorts)
        .returns("/deployments", JettyAppSeverHandler::getDeploymentDir)
        .returns("deployments", JettyAppSeverHandler::getAssemblyName)
        .returns("/usr/local/s2i/run", JettyAppSeverHandler::getCommand)
        .returns(null, JettyAppSeverHandler::getUser)
        .returns(true, JettyAppSeverHandler::supportsS2iBuild);
  }
}
