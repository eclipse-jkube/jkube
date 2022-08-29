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
import java.util.Collections;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Plugin;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TomcatAppSeverHandlerTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private GeneratorContext generatorContext;

  private Plugin plugin;

  @Before
  public void setUp() {
    generatorContext = mock(GeneratorContext.class,RETURNS_DEEP_STUBS);
    plugin = mock(Plugin.class);
  }

  @Test
  public void isApplicableHasContextXmlShouldReturnTrue() throws IOException {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder.getRoot());
    assertThat(new File(temporaryFolder.newFolder("META-INF"), "context.xml").createNewFile()).isTrue();
    assertThat(new File(temporaryFolder.newFolder("META-INF-1337"), "context.xml").createNewFile()).isTrue();
    // When
    final boolean result = new TomcatAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void isApplicableHasNotContextXmlShouldReturnFalse() throws IOException {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder.getRoot());
    assertThat(new File(temporaryFolder.newFolder("META-INF-1337"), "context.xml").createNewFile()).isTrue();
    // When
    final boolean result = new TomcatAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void isApplicableHasTomcat8PluginShouldReturnTrue() {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder.getRoot());
    when(plugin.getGroupId()).thenReturn("org.apache.tomcat.maven");
    when(plugin.getArtifactId()).thenReturn("tomcat8-maven-plugin");
    when(generatorContext.getProject().getPlugins()).thenReturn(Collections.singletonList(plugin));
    // When
    final boolean result = new TomcatAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void isApplicableHasTomcat1337PluginShouldReturnFalse() {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder.getRoot());
    when(plugin.getGroupId()).thenReturn("org.apache.tomcat.maven");
    when(plugin.getArtifactId()).thenReturn("tomcat1337-maven-plugin");
    when(generatorContext.getProject().getPlugins()).thenReturn(Collections.singletonList(plugin));
    // When
    final boolean result = new TomcatAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void isApplicableHasNoPluginShouldReturnFalse() {
    // When
    final boolean result = new TomcatAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void handlerSettings() {
    // When
    final TomcatAppSeverHandler handler = new TomcatAppSeverHandler(generatorContext);
    // Then
    assertThat(handler.getFrom()).startsWith("quay.io/jkube/jkube-tomcat:");
    assertThat(handler.exposedPorts()).contains("8080");
    assertThat(handler.getDeploymentDir()).isEqualTo("/deployments");
    assertThat(handler.getAssemblyName()).isEqualTo("deployments");
    assertThat(handler.getCommand()).isEqualTo("/usr/local/s2i/run");
    assertThat(handler.getUser()).isNull();
    assertThat(handler.supportsS2iBuild()).isTrue();
  }
}
