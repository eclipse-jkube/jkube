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

import org.eclipse.jkube.kit.common.JKubeProject;
import org.eclipse.jkube.kit.common.JKubeProjectPlugin;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.eclipse.jkube.generator.api.support.AbstractPortsExtractor;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

import java.util.Map;

public class VertxPortsExtractor extends AbstractPortsExtractor {


    public VertxPortsExtractor(PrefixedLogger log) {
        super(log);
    }

    @Override
    public String getConfigPathPropertyName() {
        return "vertx.config";
    }

    @Override
    public String getConfigPathFromProject(JKubeProject project) {
        JKubeProjectPlugin plugin = JKubeProjectUtil.getPlugin(project, Constants.VERTX_MAVEN_PLUGIN_GROUP, Constants.VERTX_MAVEN_PLUGIN_ARTIFACT);

        if (plugin != null) {
            Map<String, Object> pluginConfiguration = plugin.getConfiguration();
            /*
             * During deserialization into JKubeProjectPlugin null configuration gets converted to null string hence
             * this check.
             */
            if (pluginConfiguration == null) {
                return null;
            }
            Map<String, Object> config = (Map<String, Object>)pluginConfiguration.get("config");
            return config != null ? config.get("vertxConfig").toString() : null;
        }
        return null;
    }
}
