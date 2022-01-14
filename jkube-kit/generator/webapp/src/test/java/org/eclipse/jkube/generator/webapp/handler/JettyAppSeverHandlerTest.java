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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertTrue;

public class JettyAppSeverHandlerTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mocked
  private GeneratorContext generatorContext;

  @Test
  public void isApplicableHasJettyLoggingShouldReturnTrue() throws IOException {
    // Given
    // @formatter:off
    new Expectations() {{
      generatorContext.getProject().getBuildDirectory(); result = temporaryFolder.getRoot();
    }};
    // @formatter:on
    assertTrue(new File(temporaryFolder.newFolder("META-INF"), "jetty-logging.properties").createNewFile());
    assertTrue(new File(temporaryFolder.newFolder("META-INF-1337"), "context.xml").createNewFile());
    // When
    final boolean result = new JettyAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result, equalTo(true));
  }

  @Test
  public void isApplicableHasNotJettyLoggingShouldReturnFalse() throws IOException {
    // Given
    // @formatter:off
    new Expectations() {{
      generatorContext.getProject().getBuildDirectory(); result = temporaryFolder.getRoot();
    }};
    // @formatter:on
    assertTrue(new File(temporaryFolder.newFolder("META-INF-1337"), "context.xml").createNewFile());
    // When
    final boolean result = new JettyAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result, equalTo(false));
  }

  @Test
  public void isApplicableHasJettyMavenPluginShouldReturnTrue(@Mocked Plugin plugin) {
    // Given
    // @formatter:off
    new Expectations() {{
      generatorContext.getProject().getBuildDirectory(); result = temporaryFolder.getRoot();
      plugin.getGroupId(); result = "org.eclipse.jetty";
      plugin.getArtifactId(); result = "jetty-maven-plugin";
      generatorContext.getProject().getPlugins(); result = Collections.singletonList(plugin);
    }};
    // @formatter:on
    // When
    final boolean result = new JettyAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result, equalTo(true));
  }

  @Test
  public void isApplicableHasJetty1337MavenPluginShouldReturnFalse(@Mocked Plugin plugin) {
    // Given
    // @formatter:off
    new Expectations() {{
      generatorContext.getProject().getBuildDirectory(); result = temporaryFolder.getRoot();
      plugin.getGroupId(); result = "org.eclipse.jetty";
      plugin.getArtifactId(); result = "jetty1337-maven-plugin";
      generatorContext.getProject().getPlugins(); result = Collections.singletonList(plugin);
    }};
    // @formatter:on
    // When
    final boolean result = new JettyAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result, equalTo(false));
  }

  @Test
  public void handlerSettings() {
    // When
    final JettyAppSeverHandler handler = new JettyAppSeverHandler(generatorContext);
    // Then
    assertThat(handler.getFrom(), startsWith("quay.io/jkube/jkube-jetty9:"));
    assertThat(handler.exposedPorts(), contains("8080"));
    assertThat(handler.getDeploymentDir(), equalTo("/deployments"));
    assertThat(handler.getAssemblyName(), equalTo("deployments"));
    assertThat(handler.getCommand(), equalTo("/usr/local/s2i/run"));
    assertThat(handler.getUser(), nullValue());
    assertThat(handler.supportsS2iBuild(), equalTo(true));
  }
}
