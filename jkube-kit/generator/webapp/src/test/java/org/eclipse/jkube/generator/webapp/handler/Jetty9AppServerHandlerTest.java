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
import java.util.Collections;

import org.eclipse.jkube.generator.api.GeneratorContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Jetty9AppServerHandlerTest {

  @TempDir
  File temporaryFolder;

  private GeneratorContext generatorContext;

  @BeforeEach
  void setUp() {
    generatorContext = mock(GeneratorContext.class, RETURNS_DEEP_STUBS);
  }

  @Test
  void isApplicable_shouldReturnFalse() {
    // Given
    when(generatorContext.getProject().getBuildDirectory()).thenReturn(temporaryFolder);
    // When
    final boolean result = new Jetty9AppSeverHandler(generatorContext).isApplicable();
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void handlerSettings() {
    // When
    final Jetty9AppSeverHandler handler = new Jetty9AppSeverHandler(generatorContext);
    // Then
    assertThat(handler)
        .returns("jetty9", Jetty9AppSeverHandler::getName)
        .satisfies(h -> assertThat(h.getFrom()).startsWith("quay.io/jkube/jkube-jetty9:"))
        .returns(Collections.singletonList("8080"), Jetty9AppSeverHandler::exposedPorts)
        .returns("/deployments", Jetty9AppSeverHandler::getDeploymentDir)
        .returns("deployments", Jetty9AppSeverHandler::getAssemblyName)
        .returns("/usr/local/s2i/run", Jetty9AppSeverHandler::getCommand)
        .returns(null, Jetty9AppSeverHandler::getUser)
        .returns(true, Jetty9AppSeverHandler::supportsS2iBuild);
  }
}
