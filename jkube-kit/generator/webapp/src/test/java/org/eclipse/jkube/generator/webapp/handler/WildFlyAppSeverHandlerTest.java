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


import java.util.Collections;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WildFlyAppSeverHandlerTest {

  @Mocked
  private GeneratorContext generatorContext;

  @Test
  public void kubernetes() {
    // Given
    // @formatter:off
    new Expectations() {{
      generatorContext.getRuntimeMode(); result = RuntimeMode.kubernetes;
    }};
    // @formatter:on
    // When
    final WildFlyAppSeverHandler handler = new WildFlyAppSeverHandler(generatorContext);
    // Then
    assertCommonValues(handler);
    assertEquals("jboss/wildfly:19.0.0.Final", handler.getFrom());
    assertTrue(handler.runCmds().isEmpty());
  }

  @Test
  public void openShiftDockerStrategy() {
    // Given
    // @formatter:off
    new Expectations() {{
      generatorContext.getRuntimeMode(); result = RuntimeMode.openshift;
      generatorContext.getStrategy(); result = OpenShiftBuildStrategy.docker;
    }};
    // @formatter:on
    // When
    final WildFlyAppSeverHandler handler = new WildFlyAppSeverHandler(generatorContext);
    // Then
    assertCommonValues(handler);
    assertEquals("jboss/wildfly:19.0.0.Final", handler.getFrom());
    assertEquals(Collections.singletonList("chmod -R a+rw /opt/jboss/wildfly/standalone/"), handler.runCmds());
  }

  @Test
  public void openShiftSourceStrategy() {
    // Given
    // @formatter:off
    new Expectations() {{
      generatorContext.getRuntimeMode(); result = RuntimeMode.openshift;
      generatorContext.getStrategy(); result = OpenShiftBuildStrategy.s2i;
    }};
    // @formatter:on
    // When
    final WildFlyAppSeverHandler handler = new WildFlyAppSeverHandler(generatorContext);
    // Then
    assertCommonValues(handler);
    assertEquals("quay.io/wildfly/wildfly-centos7:19.0", handler.getFrom());
    assertTrue(handler.runCmds().isEmpty());
  }

  private static void assertCommonValues(WildFlyAppSeverHandler handler) {
    assertTrue(handler.supportsS2iBuild());
    assertEquals("deployments", handler.getAssemblyName());
    assertEquals("/opt/jboss/wildfly/standalone/deployments", handler.getDeploymentDir());
    assertEquals("/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0", handler.getCommand());
    assertEquals("jboss:jboss:jboss", handler.getUser());
    assertEquals(Collections.singletonMap("GALLEON_PROVISION_LAYERS", "cloud-server,web-clustering"), handler.getEnv());
    assertEquals(Collections.singletonList("8080"), handler.exposedPorts());
  }
}