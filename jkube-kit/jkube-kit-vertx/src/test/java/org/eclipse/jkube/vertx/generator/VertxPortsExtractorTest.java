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

import static org.eclipse.jkube.kit.common.util.FileUtil.getAbsolutePath;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.PrefixedLogger;

import mockit.Mocked;
import org.junit.Test;

public class VertxPortsExtractorTest {
  @Mocked
  PrefixedLogger log;

  @Test
  public void testVertxConfigPathFromProject() {
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
    assertEquals((Integer) 80, result.get("http.port"));
  }

  @Test
  public void testNoVertxConfiguration() {
    JavaProject project = JavaProject.builder()
        .plugins(Collections.singletonList(Plugin.builder().groupId(Constants.VERTX_MAVEN_PLUGIN_GROUP)
            .artifactId(Constants.VERTX_MAVEN_PLUGIN_ARTIFACT).version("testversion").configuration(Collections.emptyMap())
            .build()))
        .build();
    Map<String, Integer> result = new VertxPortsExtractor(log).extract(project);
    assertEquals(0, result.size());
  }
}
