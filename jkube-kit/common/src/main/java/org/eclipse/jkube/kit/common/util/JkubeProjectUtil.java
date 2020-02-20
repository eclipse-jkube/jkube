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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.JkubeProject;
import org.eclipse.jkube.kit.common.JkubeProjectDependency;
import org.eclipse.jkube.kit.common.JkubeProjectPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;

public class JkubeProjectUtil {

    public static final int MAX_RESOURCE_NAME_LENGTH = 63;

    private JkubeProjectUtil() { }

    public static <T> Optional<T> iterateOverListWithCondition(List<T> dependencyList, Predicate<? super T> condition) {
        return dependencyList.stream().filter(condition).findFirst();
    }

    public static String getAnyDependencyVersionWithGroupId(JkubeProject jkubeProject, String groupId) {
        Optional<JkubeProjectDependency> value = iterateOverListWithCondition(jkubeProject.getDependencies(),
                dependency -> dependency.getGroupId().equals(groupId));

        return value.map(JkubeProjectDependency::getVersion).orElse(null);
    }

    public static JkubeProjectPlugin getPlugin(JkubeProject jkubeProject, String groupId, String artifactId) {
        Optional<JkubeProjectPlugin> value = iterateOverListWithCondition(jkubeProject.getPlugins(),
                plugin -> plugin.getGroupId().equals(groupId) && plugin.getArtifactId().equals(artifactId));
        return value.orElse(null);
    }

    public static JkubeProjectPlugin getPlugin(JkubeProject jkubeProject, String artifactId) {
        Optional<JkubeProjectPlugin> value = iterateOverListWithCondition(jkubeProject.getPlugins(),
                plugin -> plugin.getArtifactId().equals(artifactId));
        return value.orElse(null);
    }

    public static boolean hasPlugin(JkubeProject jkubeProject, String groupId, String artifactId) {
        return getPlugin(jkubeProject, groupId, artifactId) != null;
    }

    public static boolean hasPluginOfAnyArtifactId(JkubeProject jkubeProject, String artifactId) {
        return getPlugin(jkubeProject, artifactId) != null;
    }

    public static boolean hasDependency(JkubeProject jkubeProject, String groupId, String artifactId) {
        return getDependency(jkubeProject, groupId, artifactId) != null;
    }

    public static JkubeProjectDependency getDependency(JkubeProject jkubeProject, String groupId, String artifactId) {
        List<JkubeProjectDependency> dependencyList = jkubeProject.getDependencies();
        if (dependencyList != null) {
            Optional<JkubeProjectDependency> value = iterateOverListWithCondition(dependencyList,
                    dependency -> dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId));

            return value.orElse(null);
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
                } catch (Exception e) {
                    throw new IOException(e);
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

    public static File getFinalOutputArtifact(JkubeProject jkubeProject) {
        String nameOfFinalArtifact;
        if (jkubeProject.getBuildFinalName() == null) {
            nameOfFinalArtifact = jkubeProject.getArtifactId() + "-"
                    + jkubeProject.getVersion() + "." + jkubeProject.getPackaging();
        } else {
            nameOfFinalArtifact = jkubeProject.getBuildFinalName() + "." + jkubeProject.getPackaging();
        }
        File outputDirectory = new File(jkubeProject.getBuildDirectory());
        File finalArtifact = new File(outputDirectory, nameOfFinalArtifact);
        return finalArtifact.exists() ? finalArtifact : null;
    }

    public static String createDefaultResourceName(String artifactId, String ... suffixes) {
        String suffix = StringUtils.join(suffixes, "-");
        String ret = artifactId + (suffix.length() > 0 ? "-" + suffix : "");
        if (ret.length() > MAX_RESOURCE_NAME_LENGTH) {
            ret = ret.substring(0, MAX_RESOURCE_NAME_LENGTH);
        }
        return ret.toLowerCase();
    }
}
