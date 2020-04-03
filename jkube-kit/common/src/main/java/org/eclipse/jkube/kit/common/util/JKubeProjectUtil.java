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
import org.eclipse.jkube.kit.common.JKubeProject;
import org.eclipse.jkube.kit.common.JKubeProjectDependency;
import org.eclipse.jkube.kit.common.JKubeProjectPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;

public class JKubeProjectUtil {

    public static final int MAX_RESOURCE_NAME_LENGTH = 63;

    private JKubeProjectUtil() { }

    public static <T> Optional<T> iterateOverListWithCondition(List<T> dependencyList, Predicate<? super T> condition) {
        if (dependencyList == null) {
            return Optional.empty();
        }
        return dependencyList.stream().filter(Objects::nonNull).filter(condition).findFirst();
    }

    public static String getAnyDependencyVersionWithGroupId(JKubeProject jkubeProject, String groupId) {
        Optional<JKubeProjectDependency> value = iterateOverListWithCondition(jkubeProject.getDependencies(),
                dependency -> dependency.getGroupId().equals(groupId));

        return value.map(JKubeProjectDependency::getVersion).orElse(null);
    }

    public static JKubeProjectPlugin getPlugin(JKubeProject jkubeProject, String groupId, String artifactId) {
        Optional<JKubeProjectPlugin> value = iterateOverListWithCondition(jkubeProject.getPlugins(),
                plugin -> plugin.getGroupId().equals(groupId) && plugin.getArtifactId().equals(artifactId));
        return value.orElse(null);
    }

    public static JKubeProjectPlugin getPlugin(JKubeProject jkubeProject, String artifactId) {
        Optional<JKubeProjectPlugin> value = iterateOverListWithCondition(jkubeProject.getPlugins(),
                plugin -> plugin.getArtifactId().equals(artifactId));
        return value.orElse(null);
    }

    public static boolean hasPlugin(JKubeProject jkubeProject, String groupId, String artifactId) {
        return getPlugin(jkubeProject, groupId, artifactId) != null;
    }

    public static boolean hasPluginOfAnyArtifactId(JKubeProject jkubeProject, String artifactId) {
        return getPlugin(jkubeProject, artifactId) != null;
    }

    public static boolean hasDependency(JKubeProject jkubeProject, String groupId, String artifactId) {
        return getDependency(jkubeProject, groupId, artifactId) != null;
    }

    public static JKubeProjectDependency getDependency(JKubeProject jkubeProject, String groupId, String artifactId) {
        List<JKubeProjectDependency> dependencyList = jkubeProject.getDependencies();
        if (dependencyList != null) {
            return iterateOverListWithCondition(dependencyList, dependency ->
                Objects.equals(dependency.getGroupId(), groupId) && Objects.equals(dependency.getArtifactId(), artifactId))
                .orElse(null);
        }
        return null;
    }

  /**
   * Checks if the resources specified in the provided paths exist in the project.
   *
   * @param project where the resources may exist
   * @param paths within the project where the resources exist
   * @return true if at least one of the provided resource paths exists within the project, false otherwise.
   * @throws IOException if there's a problem reading the resource
   */
  public static boolean hasResource(JKubeProject project, String... paths) throws IOException {
    try (URLClassLoader compileClassLoader = getClassLoader(project)) {
      for (String path : paths) {
        if (compileClassLoader.getResource(path) != null) {
          return true;
        }
      }
    } catch (NullPointerException e) {
      throw new IOException("Path to resource was null", e);
    }
    return false;
  }

    public static Properties getPropertiesWithSystemOverrides(JKubeProject project) {
        Properties properties = new Properties(project.getProperties());
        properties.putAll(System.getProperties());
        return properties;
    }

  public static File getFinalOutputArtifact(JKubeProject jkubeProject) {
    final String nameOfFinalArtifact;
    if (jkubeProject.getBuildFinalName() == null) {
      nameOfFinalArtifact = String.format("%s-%s.%s",
          jkubeProject.getArtifactId(), jkubeProject.getVersion(), jkubeProject.getPackaging());
    } else {
      nameOfFinalArtifact = String.format("%s.%s",
          jkubeProject.getBuildFinalName(), jkubeProject.getPackaging());
    }
    final File finalArtifact = new File(jkubeProject.getBuildDirectory(), nameOfFinalArtifact);
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

  public static URLClassLoader getClassLoader(JKubeProject jKubeProject) {
    return ClassUtil.createClassLoader(
        jKubeProject.getCompileClassPathElements(),
        jKubeProject.getOutputDirectory().getAbsolutePath());
  }
}
