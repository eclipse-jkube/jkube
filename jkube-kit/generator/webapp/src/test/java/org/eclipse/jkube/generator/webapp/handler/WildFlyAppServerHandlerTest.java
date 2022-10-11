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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WildFlyAppServerHandlerTest {

  @TempDir
  Path temporaryFolder;

  private GeneratorContext generatorContext;
  private Plugin plugin;

  @BeforeEach
  public void setUp() {
    generatorContext = mock(GeneratorContext.class,RETURNS_DEEP_STUBS);
    plugin = mock(Plugin.class);
  }

  @Nested
  @DisplayName("isApplicable")
  class IsApplicable {

    @Test
    @DisplayName("with JMS XML, should return true")
    void withJmsXml_shouldReturnTrue() throws IOException {
      // Given
      when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder.toFile());

      Path metaInf = Files.createDirectories(temporaryFolder.resolve("META-INF"));
      Files.createFile(metaInf.resolve("some-file-with-jms.xml"));

      Path invalidMetaInf = Files.createDirectories(temporaryFolder.resolve("META-INF-1337"));
      Files.createFile(invalidMetaInf.resolve("context.xml"));
      // When
      final boolean result = new WildFlyAppSeverHandler(generatorContext).isApplicable();
      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("with JMS XML and thorntail plugin, should return false")
    void withJmsXmlAndThorntailPlugin_shouldReturnFalse() throws IOException {
      // Given
      when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder.toFile());
      when(plugin.getGroupId()).thenReturn("io.thorntail");
      when(plugin.getArtifactId()).thenReturn("thorntail-maven-plugin");
      when(generatorContext.getProject().getPlugins()).thenReturn(Collections.singletonList(plugin));

      Path metaInf = Files.createDirectories(temporaryFolder.resolve("META-INF"));
      Files.createFile(metaInf.resolve("some-file-with-jms.xml"));

      Path invalidMetaInf = Files.createDirectories(temporaryFolder.resolve("META-INF-1337"));
      Files.createFile(invalidMetaInf.resolve("context.xml"));
      // When
      final boolean result = new WildFlyAppSeverHandler(generatorContext).isApplicable();
      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("with no files, should return false")
    void withNoApplicableFiles_shouldReturnFalse() throws IOException {
      // Given
      when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder.toFile());
      Path invalidMetaInf = Files.createDirectories(temporaryFolder.resolve("META-INF-1337"));
      Files.createFile(invalidMetaInf.resolve("context.xml"));
      // When
      final boolean result = new WildFlyAppSeverHandler(generatorContext).isApplicable();
      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("with wildfly plugin, should return true")
    void withWildflyPlugin_shouldReturnTrue() {
      // Given
      when(plugin.getGroupId()).thenReturn("org.wildfly.plugins");
      when(plugin.getArtifactId()).thenReturn("wildfly-maven-plugin");
      when(generatorContext.getProject().getPlugins()).thenReturn(Collections.singletonList(plugin));
      // When
      final boolean result = new WildFlyAppSeverHandler(generatorContext).isApplicable();
      // Then
      assertThat(result).isTrue();
    }
  }

  @Test
  void kubernetes() {
    // Given
    when(generatorContext.getRuntimeMode()).thenReturn(RuntimeMode.KUBERNETES);
    // When
    final WildFlyAppSeverHandler handler = new WildFlyAppSeverHandler(generatorContext);
    // Then
    assertCommonValues(handler);
    assertThat(handler)
        .returns("jboss/wildfly:25.0.0.Final", WildFlyAppSeverHandler::getFrom)
        .returns("/opt/jboss/wildfly/standalone/deployments", WildFlyAppSeverHandler::getDeploymentDir)
        .returns(Collections.emptyList(), WildFlyAppSeverHandler::runCmds);
  }

  @Test
  void openShiftDockerStrategy() {
    // Given
    when(generatorContext.getRuntimeMode()).thenReturn(RuntimeMode.OPENSHIFT);
    when(generatorContext.getStrategy()).thenReturn(JKubeBuildStrategy.docker);
    // When
    final WildFlyAppSeverHandler handler = new WildFlyAppSeverHandler(generatorContext);
    // Then
    assertCommonValues(handler);
    assertThat(handler)
        .returns("jboss/wildfly:25.0.0.Final", WildFlyAppSeverHandler::getFrom)
        .returns("/opt/jboss/wildfly/standalone/deployments", WildFlyAppSeverHandler::getDeploymentDir)
        .returns(Collections.singletonList("chmod -R a+rw /opt/jboss/wildfly/standalone/"), WildFlyAppSeverHandler::runCmds);
  }

  @Test
  void openShiftSourceStrategy() {
    // Given
    when(generatorContext.getRuntimeMode()).thenReturn(RuntimeMode.OPENSHIFT);
    when(generatorContext.getStrategy()).thenReturn(JKubeBuildStrategy.s2i);
    // When
    final WildFlyAppSeverHandler handler = new WildFlyAppSeverHandler(generatorContext);
    // Then
    assertCommonValues(handler);
    assertThat(handler)
        .returns("quay.io/wildfly/wildfly-centos7:26.0", WildFlyAppSeverHandler::getFrom)
        .returns("/deployments", WildFlyAppSeverHandler::getDeploymentDir)
        .returns(Collections.emptyList(), WildFlyAppSeverHandler::runCmds);
  }

  private static void assertCommonValues(WildFlyAppSeverHandler handler) {
    assertThat(handler)
        .returns(true, WildFlyAppSeverHandler::supportsS2iBuild)
        .returns("deployments", WildFlyAppSeverHandler::getAssemblyName)
        .returns("/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0", WildFlyAppSeverHandler::getCommand)
        .returns("jboss:jboss:jboss", WildFlyAppSeverHandler::getUser)
        .returns(Collections.singletonMap("GALLEON_PROVISION_LAYERS", "cloud-server,web-clustering"), WildFlyAppSeverHandler::getEnv)
        .returns(Collections.singletonList("8080"), WildFlyAppSeverHandler::exposedPorts);
  }
}
