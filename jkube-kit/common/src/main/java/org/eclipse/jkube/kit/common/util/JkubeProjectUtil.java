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
package org.eclipse.jkube.kit.common.util;

import org.eclipse.jkube.kit.common.JkubeProject;
import org.eclipse.jkube.kit.common.JkubeProjectDependency;
import org.eclipse.jkube.kit.common.JkubeProjectPlugin;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Properties;

public class JkubeProjectUtil {
    public static String getDependencyVersion(JkubeProject jkubeProject, String groupId, String artifactId) {
        List<JkubeProjectDependency> dependencyList = jkubeProject.getDependencies();
        for (JkubeProjectDependency dependency : dependencyList) {
            if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                return dependency.getVersion();
            }
        }
        return null;
    }

    public static JkubeProjectPlugin getPlugin(JkubeProject jkubeProject, String groupId, String artifactId) {
        List<JkubeProjectPlugin> pluginList = jkubeProject.getPlugins();
        for (JkubeProjectPlugin plugin : pluginList) {
            if (plugin.getGroupId().equals(groupId) && plugin.getArtifactId().equals(artifactId)) {
                return plugin;
            }
        }
        return null;
    }

    public static boolean hasPlugin(JkubeProject jkubeProject, String groupId, String artifactId) {
        return getPlugin(jkubeProject, groupId, artifactId) != null;
    }

    public static JkubeProjectPlugin getPluginOfAnyArtifactId(JkubeProject jkubeProject, String groupId) {
        List<JkubeProjectPlugin> pluginList = jkubeProject.getPlugins();
        for (JkubeProjectPlugin plugin : pluginList) {
            if (plugin.getArtifactId().equals(groupId)) {
                return plugin;
            }
        }
        return null;
    }

    public static boolean hasPluginOfAnyArtifactId(JkubeProject jkubeProject, String artifactId) {
        return getPluginOfAnyArtifactId(jkubeProject, artifactId) != null;
    }

    public static boolean hasDependency(JkubeProject jkubeProject, String groupId, String artifactId) {
        return getDependency(jkubeProject, groupId, artifactId) != null;
    }

    public static JkubeProjectDependency getDependency(JkubeProject jkubeProject, String groupId, String artifactId) {
        List<JkubeProjectDependency> dependencyList = jkubeProject.getDependencies();
        if (dependencyList != null) {
            for (JkubeProjectDependency dependency : dependencyList) {
                if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                    return dependency;
                }
            }
        }
        return null;
    }

    public static boolean hasResource(JkubeProject project, String... paths) throws IOException {
        try (URLClassLoader compileClassLoader = ClassUtil.createClassLoader(project.getCompileClassPathElements(), project.getOutputDirectory())) {
            for (String path : paths) {
                try {
                    if (compileClassLoader.getResource(path) != null) {
                        return true;
                    }
                } catch (Throwable e) {
                    // ignore
                }
            }
        }
        return false;
    }

    public static Properties getPropertiesWithSystemOverrides(JkubeProject project) {
        Properties properties = new Properties(project.getProperties());
        properties.putAll(System.getProperties());
        return properties;
    }
}
