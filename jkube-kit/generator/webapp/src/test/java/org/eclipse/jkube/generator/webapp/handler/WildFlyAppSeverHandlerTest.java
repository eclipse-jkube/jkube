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
import static org.assertj.core.api.Assertions.assertThat;
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
    assertThat(new File(temporaryFolder.newFolder("META-INF"), "some-file-with-jms.xml").createNewFile()).isTrue();
    assertThat(new File(temporaryFolder.newFolder("META-INF-1337"), "context.xml").createNewFile()).isTrue();
    // When
    final boolean result = new WildFlyAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isTrue();
  }

  @Test
  public void isApplicableHasJmsXmlAndThorntailPluginShouldReturnFalse() throws IOException {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder.getRoot());
    when(plugin.getGroupId()).thenReturn("io.thorntail");
    when(plugin.getArtifactId()).thenReturn("thorntail-maven-plugin");
    when(generatorContext.getProject().getPlugins()).thenReturn(Collections.singletonList(plugin));
    assertThat(new File(temporaryFolder.newFolder("META-INF"), "some-file-with-jms.xml").createNewFile()).isTrue();
    assertThat(new File(temporaryFolder.newFolder("META-INF-1337"), "context.xml").createNewFile()).isTrue();
    // When
    final boolean result = new WildFlyAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  public void isApplicableHasNoApplicableFilesShouldReturnFalse() throws IOException {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder.getRoot());
    assertThat(new File(temporaryFolder.newFolder("META-INF-1337"), "context.xml").createNewFile()).isTrue();
    // When
    final boolean result = new WildFlyAppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isFalse();
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
    assertThat(result).isTrue();
  }

  @Test
  public void kubernetes() {
    // Given
    when(generatorContext.getRuntimeMode()).thenReturn(RuntimeMode.KUBERNETES);
    // When
    final WildFlyAppSeverHandler handler = new WildFlyAppSeverHandler(generatorContext);
    // Then
    assertCommonValues(handler);
    assertThat((handler.getFrom())).isEqualTo("jboss/wildfly:25.0.0.Final");
    assertThat(handler.getDeploymentDir()).isEqualTo("/opt/jboss/wildfly/standalone/deployments");
    assertThat(handler.runCmds()).isEmpty();
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
    assertThat(handler.getFrom()).isEqualTo("jboss/wildfly:25.0.0.Final");
    assertThat(handler.getDeploymentDir()).isEqualTo("/opt/jboss/wildfly/standalone/deployments");
    assertThat(handler.runCmds()).isEqualTo(Collections.singletonList("chmod -R a+rw /opt/jboss/wildfly/standalone/"));
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
    assertThat(handler.getFrom()).isEqualTo("quay.io/wildfly/wildfly-centos7:26.0");
    assertThat(handler.getDeploymentDir()).isEqualTo("/deployments");
    assertThat(handler.runCmds()).isEmpty();
  }

  private static void assertCommonValues(WildFlyAppSeverHandler handler) {

    assertThat(handler.supportsS2iBuild()).isTrue();
    assertThat(handler.getAssemblyName()).isEqualTo("deployments");
    assertThat(handler.getCommand()).isEqualTo("/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0");
    assertThat(handler.getUser()).isEqualTo("jboss:jboss:jboss");
    assertThat(handler.getEnv()).isEqualTo(Collections.singletonMap("GALLEON_PROVISION_LAYERS", "cloud-server,web-clustering"));
    assertThat(handler.exposedPorts()).isEqualTo(Collections.singletonList("8080")).isEqualTo( handler.exposedPorts());
  }
}
