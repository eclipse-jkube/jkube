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
package io.jkube.vertx.generator;

import io.jkube.kit.common.PrefixedLogger;
import io.jkube.generator.api.support.AbstractPortsExtractor;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class VertxPortsExtractor extends AbstractPortsExtractor {


    public VertxPortsExtractor(PrefixedLogger log) {
        super(log);
    }

    @Override
    public String getConfigPathPropertyName() {
        return "vertx.config";
    }

    @Override
    public String getConfigPathFromProject(MavenProject project) {
        Plugin plugin = project.getPlugin(Constants.VERTX_MAVEN_PLUGIN_GROUP + ":" + Constants.VERTX_MAVEN_PLUGIN_ARTIFACT);
        if (plugin == null) {
            return null;
        }

        Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
        if (configuration == null) {
            return null;
        }
        Xpp3Dom config = configuration.getChild("config");
        return config != null ? config.getValue() : null;
    }
}
