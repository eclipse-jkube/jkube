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
import org.eclipse.jkube.generator.api.GeneratorMode;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.WatchMode;

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
        .returns("jetty", JettyAppSeverHandler::getName)
        .satisfies(h -> assertThat(h.getFrom()).startsWith("quay.io/jkube/jkube-jetty12:"))
        .returns(Collections.singletonList("8080"), JettyAppSeverHandler::exposedPorts)
        .returns("/deployments", JettyAppSeverHandler::getDeploymentDir)
        .returns("deployments", JettyAppSeverHandler::getAssemblyName)
        .returns("/usr/local/s2i/run", JettyAppSeverHandler::getCommand)
        .returns(null, JettyAppSeverHandler::getUser)
        .returns(true, JettyAppSeverHandler::supportsS2iBuild);
  }

  @Test
  void getEnv_inWatchCopyMode_shouldReturnScanIntervalForHotDeploy() {
    // Given
    when(generatorContext.getGeneratorMode()).thenReturn(GeneratorMode.WATCH);
    when(generatorContext.getWatchMode()).thenReturn(WatchMode.copy);
    // When
    final JettyAppSeverHandler handler = new JettyAppSeverHandler(generatorContext);
    // Then
    assertThat(handler.getEnv())
        .containsEntry("JAVA_TOOL_OPTIONS", "-Djetty.deploy.scanInterval=1");
  }

  @Test
  void getEnv_inWatchBuildMode_shouldNotReturnScanInterval() {
    // Given
    when(generatorContext.getGeneratorMode()).thenReturn(GeneratorMode.WATCH);
    when(generatorContext.getWatchMode()).thenReturn(WatchMode.build);
    // When
    final JettyAppSeverHandler handler = new JettyAppSeverHandler(generatorContext);
    // Then
    assertThat(handler.getEnv()).isEmpty();
  }

  @Test
  void getEnv_inBuildMode_shouldNotReturnScanInterval() {
    // Given
    when(generatorContext.getGeneratorMode()).thenReturn(GeneratorMode.BUILD);
    // When
    final JettyAppSeverHandler handler = new JettyAppSeverHandler(generatorContext);
    // Then
    assertThat(handler.getEnv()).isEmpty();
  }
}
