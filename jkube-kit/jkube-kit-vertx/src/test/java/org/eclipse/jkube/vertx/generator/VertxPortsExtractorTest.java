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

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jkube.kit.common.JkubeProject;
import org.eclipse.jkube.kit.common.JkubeProjectPlugin;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import mockit.Mocked;
import org.junit.Test;

import static org.eclipse.jkube.kit.common.util.FileUtil.getAbsolutePath;
import static org.junit.Assert.assertEquals;

public class VertxPortsExtractorTest {
    @Mocked
    PrefixedLogger log;

    @Test
    public void testVertxConfigPathFromProject() throws Exception {
        Map<String, Object> vertxConfig = new HashMap<>();
        vertxConfig.put("vertxConfig", getAbsolutePath(VertxPortsExtractorTest.class.getResource("/config.json")));

        Map<String, Object> configuration = new HashMap<>();
        configuration.put("config", vertxConfig);


        JkubeProject project = new JkubeProject.Builder()
                .plugins(Collections.singletonList(new JkubeProjectPlugin.Builder().groupId(Constants.VERTX_MAVEN_PLUGIN_GROUP).artifactId(Constants.VERTX_MAVEN_PLUGIN_ARTIFACT).version("testversion").executions(Collections.singletonList("testexec")).configuration(configuration).build()))
                .build();

        Map<String, Integer> result = new VertxPortsExtractor(log).extract(project);
        assertEquals((Integer) 80, result.get("http.port"));
    }

    @Test
    public void testNoVertxConfiguration() throws Exception {
        JkubeProject project = new JkubeProject.Builder()
                .plugins(Collections.singletonList(new JkubeProjectPlugin.Builder().groupId(Constants.VERTX_MAVEN_PLUGIN_GROUP).artifactId(Constants.VERTX_MAVEN_PLUGIN_ARTIFACT).version("testversion").configuration(Collections.emptyMap()).build()))
                .build();
        Map<String, Integer> result = new VertxPortsExtractor(log).extract(project);
        assertEquals(0,result.size());
    }
}
