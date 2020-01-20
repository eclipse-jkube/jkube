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

import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.jkube.kit.common.JkubeProject;
import org.eclipse.jkube.kit.common.JkubeProjectPlugin;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.eclipse.jkube.generator.api.support.AbstractPortsExtractor;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.jkube.kit.common.util.JkubeProjectUtil;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class VertxPortsExtractor extends AbstractPortsExtractor {


    public VertxPortsExtractor(PrefixedLogger log) {
        super(log);
    }

    @Override
    public String getConfigPathPropertyName() {
        return "vertx.config";
    }

    @Override
    public String getConfigPathFromProject(JkubeProject project) {
        JkubeProjectPlugin plugin = JkubeProjectUtil.getPlugin(project, Constants.VERTX_MAVEN_PLUGIN_GROUP, Constants.VERTX_MAVEN_PLUGIN_ARTIFACT);

        if (plugin != null) {
            try {
                Object pluginConfiguration = plugin.getConfiguration();
                Xpp3Dom configuration = Xpp3DomBuilder.build(new StringReader(pluginConfiguration.toString()));
                if (configuration == null) {
                    return null;
                }
                Xpp3Dom config = configuration.getChild("config");
                return config != null ? config.getValue() : null;
            } catch (IOException | XmlPullParserException exception) {
                log.warn("Error in parsing plugin configuration: ", exception);
            }
        }
        return null;
    }
}
