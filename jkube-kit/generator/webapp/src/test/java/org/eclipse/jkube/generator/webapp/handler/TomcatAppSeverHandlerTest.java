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

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class TomcatAppSeverHandlerTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mocked
  private GeneratorContext generatorContext;

  @Test
  public void isApplicableHasContextXmlShouldReturnTrue() throws IOException {
    // Given
    // @formatter:off
    new Expectations() {{
      generatorContext.getProject().getBuildDirectory(); result = temporaryFolder.getRoot();
    }};
    // @formatter:on
    assertTrue(new File(temporaryFolder.newFolder("META-INF"), "context.xml").createNewFile());
    assertTrue(new File(temporaryFolder.newFolder("META-INF-1337"), "context.xml").createNewFile());
    // When
    final boolean result = new TomcatAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void isApplicableHasNotContextXmlShouldReturnFalse() throws IOException {
    // Given
    // @formatter:off
    new Expectations() {{
      generatorContext.getProject().getBuildDirectory(); result = temporaryFolder.getRoot();
    }};
    // @formatter:on
    assertTrue(new File(temporaryFolder.newFolder("META-INF-1337"), "context.xml").createNewFile());
    // When
    final boolean result = new TomcatAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void isApplicableHasTomcat8PluginShouldReturnTrue(@Mocked Plugin plugin) {
    // Given
    // @formatter:off
    new Expectations() {{
      generatorContext.getProject().getBuildDirectory(); result = temporaryFolder.getRoot();
      plugin.getGroupId(); result = "org.apache.tomcat.maven";
      plugin.getArtifactId(); result = "tomcat8-maven-plugin";
      generatorContext.getProject().getPlugins(); result = Collections.singletonList(plugin);
    }};
    // @formatter:on
    // When
    final boolean result = new TomcatAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void isApplicableHasTomcat1337PluginShouldReturnFalse(@Mocked Plugin plugin) {
    // Given
    // @formatter:off
    new Expectations() {{
      generatorContext.getProject().getBuildDirectory(); result = temporaryFolder.getRoot();
      plugin.getGroupId(); result = "org.apache.tomcat.maven";
      plugin.getArtifactId(); result = "tomcat1337-maven-plugin";
      generatorContext.getProject().getPlugins(); result = Collections.singletonList(plugin);
    }};
    // @formatter:on
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
    assertThat(handler.getFrom()).startsWith("quay.io/jkube/jkube-tomcat9:");
    assertThat(handler.exposedPorts()).contains("8080");
    assertThat(handler.getDeploymentDir()).isEqualTo("/deployments");
    assertThat(handler.getAssemblyName()).isEqualTo("deployments");
    assertThat(handler.getCommand()).isEqualTo("/usr/local/s2i/run");
    assertThat(handler.getUser()).isNull();
    assertThat(handler.supportsS2iBuild()).isTrue();
  }
}
