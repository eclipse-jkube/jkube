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
package org.eclipse.jkube.kit.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JkubeProjectPlugin extends JkubeProjectDependency {
    public Object getConfiguration() {
        return configuration;
    }

    private Object configuration;
    private List<String> executions;

    public JkubeProjectPlugin(String groupId, String artifactId, String version, Object configuration, List<String> executions) {
        super(groupId, artifactId, version);
        this.configuration = configuration;
        this.executions = executions;
    }

    public static JkubeProjectPlugin fromString(String jkubePluginAsString) {
        String parts[] = jkubePluginAsString.split(",");
        if (parts.length == 4) {
            return new JkubeProjectPlugin(parts[0], parts[1], parts[2], parts[3], null);
        } else if (parts.length == 5) {
            return new JkubeProjectPlugin(parts[0], parts[1], parts[2], parts[3], Arrays.asList(parts[4].split("\\|")));
        }
        return null;
    }

    public static List<JkubeProjectPlugin> listFromStringPlugins(List<String> jkubePluginsAsStr) {
        List<JkubeProjectPlugin> plugins = new ArrayList<>();
        for (String commaSeparatedPlugins : jkubePluginsAsStr) {
            JkubeProjectPlugin jkubeProjectPlugin = JkubeProjectPlugin.fromString(commaSeparatedPlugins);
            if (jkubeProjectPlugin != null) {
                plugins.add(jkubeProjectPlugin);
            }
        }
        return plugins;
    }

    public List<String> getExecutions() {
        return executions;
    }
}
