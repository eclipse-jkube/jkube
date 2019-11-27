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

import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.eclipse.jkube.generator.api.support.AbstractPortsExtractor;
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
