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

import java.util.Map;

import org.eclipse.jkube.kit.common.PrefixedLogger;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;

import static org.eclipse.jkube.kit.common.util.FileUtil.getAbsolutePath;
import static org.junit.Assert.assertEquals;

public class VertxPortsExtractorTest {

    @Mocked
    MavenProject project;

    @Mocked
    PrefixedLogger log;

    @Mocked
    Plugin plugin;

    @Mocked
    Xpp3Dom configuration;

    @Mocked
    Xpp3Dom vertxConfig;

    @Test
    public void testVertxConfigPathFromProject() throws Exception {
        new Expectations() {{
            project.getPlugin(Constants.VERTX_MAVEN_PLUGIN_GROUP + ":" + Constants.VERTX_MAVEN_PLUGIN_ARTIFACT);
            result = plugin;
            plugin.getConfiguration();
            result = configuration;
            configuration.getChild("config");
            result = vertxConfig;
            vertxConfig.getValue();
            result = getAbsolutePath(VertxPortsExtractorTest.class.getResource("/config.json"));
        }};

        Map<String, Integer> result = new VertxPortsExtractor(log).extract(project);
        assertEquals((Integer) 80, result.get("http.port"));
    }

    @Test
    public void testNoVertxConfiguration() throws Exception {
        new Expectations() {{
            project.getPlugin(Constants.VERTX_MAVEN_PLUGIN_GROUP + ":" + Constants.VERTX_MAVEN_PLUGIN_ARTIFACT);
            plugin.getConfiguration(); result = null;
        }};
        Map<String, Integer> result = new VertxPortsExtractor(log).extract(project);
        assertEquals(0,result.size());
    }
}
