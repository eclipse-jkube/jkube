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

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JkubeProjectPlugin implements Serializable {
    private String groupId;
    private String artifactId;
    private String version;
    private Map<String, Object> configuration;
    private List<String> executions;

    private JkubeProjectPlugin() { }

    public JkubeProjectPlugin(String groupId, String artifactId, String version, Map<String, Object> configuration, List<String> executions) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.configuration = configuration;
        this.executions = executions;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public static JkubeProjectPlugin fromString(String pluginAsStr, Map<String, Object> pluginConfiguration) {
        String[] parts = pluginAsStr.split(",");
        if (parts.length == 3) {
            return new JkubeProjectPlugin(parts[0], parts[1], parts[2], pluginConfiguration, null);
        } else if (parts.length == 4) {
            return new JkubeProjectPlugin(parts[0], parts[1], parts[2], pluginConfiguration, Arrays.asList(parts[3].split("\\|")));
        }
        return null;
    }

    public static List<JkubeProjectPlugin> listFromStringPlugins(List<AbstractMap.SimpleEntry<String, Map<String, Object>>> jkubePluginsAsStr) {
        List<JkubeProjectPlugin> plugins = new ArrayList<>();
        for (AbstractMap.SimpleEntry<String, Map<String, Object>> commaSeparatedPlugins : jkubePluginsAsStr) {
            JkubeProjectPlugin jkubeProjectPlugin = JkubeProjectPlugin.fromString(commaSeparatedPlugins.getKey(), commaSeparatedPlugins.getValue());
            if (jkubeProjectPlugin != null) {
                plugins.add(jkubeProjectPlugin);
            }
        }
        return plugins;
    }

    public List<String> getExecutions() {
        return executions;
    }

    public static class Builder {
        private JkubeProjectPlugin projectPlugin;

        public Builder() {
            this.projectPlugin = new JkubeProjectPlugin();
        }

        public Builder(JkubeProjectPlugin plugin) {
            if (plugin != null) {
                this.projectPlugin = plugin;
            }
        }

        public Builder groupId(String groupId) {
            this.projectPlugin.groupId = groupId;
            return this;
        }

        public Builder artifactId(String artifactId) {
            this.projectPlugin.artifactId = artifactId;
            return this;
        }

        public Builder version(String version) {
            this.projectPlugin.version = version;
            return this;
        }

        public Builder executions(List<String> executions) {
            this.projectPlugin.executions = executions;
            return this;
        }

        public Builder configuration(Map<String, Object> configuration) {
            this.projectPlugin.configuration = configuration;
            return this;
        }

        public JkubeProjectPlugin build() {
            return this.projectPlugin;
        }
    }
}
