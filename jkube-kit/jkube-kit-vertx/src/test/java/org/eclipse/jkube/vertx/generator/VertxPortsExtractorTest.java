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
package org.eclipse.jkube.vertx.generator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.common.util.FileUtil.getAbsolutePath;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

class VertxPortsExtractorTest {
  private PrefixedLogger log;

  @BeforeEach
  public void setUp() {
    log = mock(PrefixedLogger.class,RETURNS_DEEP_STUBS);
  }

  @Test
  void extract_vertxConfigPathFromProject_shouldReturnConfiguredPort() {
    Map<String, Object> vertxConfig = new HashMap<>();
    vertxConfig.put("vertxConfig", getAbsolutePath(VertxPortsExtractorTest.class.getResource("/config.json")));

    Map<String, Object> configuration = new HashMap<>();
    configuration.put("config", vertxConfig);

    JavaProject project = JavaProject.builder()
        .plugins(Collections.singletonList(Plugin.builder().groupId(Constants.VERTX_MAVEN_PLUGIN_GROUP)
            .artifactId(Constants.VERTX_MAVEN_PLUGIN_ARTIFACT).version("testversion")
            .executions(Collections.singletonList("testexec")).configuration(configuration).build()))
        .build();

    Map<String, Integer> result = new VertxPortsExtractor(log).extract(project);
    assertThat(result).containsEntry("http.port", 80);
  }

  @Test
  void extract_withNoVertxConfiguration_shouldBeEmpty() {
    JavaProject project = JavaProject.builder()
        .plugins(Collections.singletonList(Plugin.builder().groupId(Constants.VERTX_MAVEN_PLUGIN_GROUP)
            .artifactId(Constants.VERTX_MAVEN_PLUGIN_ARTIFACT).version("testversion").configuration(Collections.emptyMap())
            .build()))
        .build();
    Map<String, Integer> result = new VertxPortsExtractor(log).extract(project);
    assertThat(result).isEmpty();
  }
}
