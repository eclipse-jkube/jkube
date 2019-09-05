/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jshift.vertx.generator;

import java.util.Map;

import io.jshift.kit.common.PrefixedLogger;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;

import static io.jshift.kit.common.util.FileUtil.getAbsolutePath;
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
