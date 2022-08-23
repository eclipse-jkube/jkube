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
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WildFlyAppSeverHandlerTest {

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
  public void isApplicableHasJmsXmlShouldReturnTrue() throws IOException {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder.getRoot());
    assertTrue(new File(temporaryFolder.newFolder("META-INF"), "some-file-with-jms.xml").createNewFile());
    assertTrue(new File(temporaryFolder.newFolder("META-INF-1337"), "context.xml").createNewFile());
    // When
    final boolean result = new WildFlyAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertTrue(result);
  }

  @Test
  public void isApplicableHasJmsXmlAndThorntailPluginShouldReturnFalse() throws IOException {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder.getRoot());
    when(plugin.getGroupId()).thenReturn("io.thorntail");
    when(plugin.getArtifactId()).thenReturn("thorntail-maven-plugin");
    when(generatorContext.getProject().getPlugins()).thenReturn(Collections.singletonList(plugin));
    assertTrue(new File(temporaryFolder.newFolder("META-INF"), "some-file-with-jms.xml").createNewFile());
    assertTrue(new File(temporaryFolder.newFolder("META-INF-1337"), "context.xml").createNewFile());
    // When
    final boolean result = new WildFlyAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertFalse(result);
  }

  @Test
  public void isApplicableHasNoApplicableFilesShouldReturnFalse() throws IOException {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder.getRoot());
    assertTrue(new File(temporaryFolder.newFolder("META-INF-1337"), "context.xml").createNewFile());
    // When
    final boolean result = new WildFlyAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertFalse(result);
  }

  @Test
  public void isApplicableHasWildflyPluginShouldReturnTrue() {
    // Given
    when(plugin.getGroupId()).thenReturn("org.wildfly.plugins");
    when(plugin.getArtifactId()).thenReturn("wildfly-maven-plugin");
    when(generatorContext.getProject().getPlugins()).thenReturn(Collections.singletonList(plugin));
    // When
    final boolean result = new WildFlyAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertTrue(result);
  }

  @Test
  public void kubernetes() {
    // Given
    when(generatorContext.getRuntimeMode()).thenReturn(RuntimeMode.KUBERNETES);
    // When
    final WildFlyAppSeverHandler handler = new WildFlyAppSeverHandler(generatorContext);
    // Then
    assertCommonValues(handler);
    assertEquals("jboss/wildfly:25.0.0.Final", handler.getFrom());
    assertEquals("/opt/jboss/wildfly/standalone/deployments", handler.getDeploymentDir());
    assertTrue(handler.runCmds().isEmpty());
  }

  @Test
  public void openShiftDockerStrategy() {
    // Given
    when(generatorContext.getRuntimeMode()).thenReturn(RuntimeMode.OPENSHIFT);
    when(generatorContext.getStrategy()).thenReturn(JKubeBuildStrategy.docker);
    // When
    final WildFlyAppSeverHandler handler = new WildFlyAppSeverHandler(generatorContext);
    // Then
    assertCommonValues(handler);
    assertEquals("jboss/wildfly:25.0.0.Final", handler.getFrom());
    assertEquals("/opt/jboss/wildfly/standalone/deployments", handler.getDeploymentDir());
    assertEquals(Collections.singletonList("chmod -R a+rw /opt/jboss/wildfly/standalone/"), handler.runCmds());
  }

  @Test
  public void openShiftSourceStrategy() {
    // Given
    when(generatorContext.getRuntimeMode()).thenReturn(RuntimeMode.OPENSHIFT);
    when(generatorContext.getStrategy()).thenReturn(JKubeBuildStrategy.s2i);
    // When
    final WildFlyAppSeverHandler handler = new WildFlyAppSeverHandler(generatorContext);
    // Then
    assertCommonValues(handler);
    assertEquals("quay.io/wildfly/wildfly-centos7:26.0", handler.getFrom());
    assertEquals("/deployments", handler.getDeploymentDir());
    assertTrue(handler.runCmds().isEmpty());
  }

  private static void assertCommonValues(WildFlyAppSeverHandler handler) {
    assertTrue(handler.supportsS2iBuild());
    assertEquals("deployments", handler.getAssemblyName());
    assertEquals("/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0", handler.getCommand());
    assertEquals("jboss:jboss:jboss", handler.getUser());
    assertEquals(Collections.singletonMap("GALLEON_PROVISION_LAYERS", "cloud-server,web-clustering"), handler.getEnv());
    assertEquals(Collections.singletonList("8080"), handler.exposedPorts());
  }
}
