/*
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
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.Plugin;

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

    public static String getAnyDependencyVersionWithGroupId(JavaProject jkubeProject, String groupId) {
        Optional<Dependency> value = iterateOverListWithCondition(jkubeProject.getDependencies(),
                dependency -> dependency.getGroupId().equals(groupId));

        return value.map(Dependency::getVersion).orElse(null);
    }

    public static Plugin getPlugin(JavaProject javaProject, String groupId, String artifactId) {
        Optional<Plugin> value = iterateOverListWithCondition(javaProject.getPlugins(),
                plugin -> plugin.getGroupId().equals(groupId) && plugin.getArtifactId().equals(artifactId));
        return value.orElse(null);
    }

    public static Plugin getPlugin(JavaProject jkubeProject, String artifactId) {
        Optional<Plugin> value = iterateOverListWithCondition(jkubeProject.getPlugins(),
                plugin -> plugin.getArtifactId().equals(artifactId));
        return value.orElse(null);
    }

    public static boolean hasPlugin(JavaProject jkubeProject, String groupId, String artifactId) {
        return getPlugin(jkubeProject, groupId, artifactId) != null;
    }

    public static boolean hasPluginOfAnyArtifactId(JavaProject jkubeProject, String artifactId) {
        return getPlugin(jkubeProject, artifactId) != null;
    }

    public static boolean hasGradlePlugin(JavaProject javaProject, String pluginClassName) {
      return Optional.ofNullable(javaProject.getGradlePlugins())
          .map(classes -> classes.contains(pluginClassName))
          .orElse(false);
    }

    public static boolean hasDependency(JavaProject jkubeProject, String groupId, String artifactId) {
        return getDependency(jkubeProject, groupId, artifactId) != null;
    }

    public static boolean hasTransitiveDependency(JavaProject javaProject, String groupId, String artifactId) {
      return getTransitiveDependency(javaProject, groupId, artifactId) != null;
    }

    public static boolean hasDependencyWithGroupId(JavaProject project, String groupId) {
      return Optional.ofNullable(project).map(JavaProject::getDependencies)
          .map(deps -> deps.stream().anyMatch(dep -> Objects.equals(dep.getGroupId(), groupId)))
          .orElse(false);
    }

    public static Dependency getTransitiveDependency(JavaProject javaProject, String groupId, String artifactId) {
      return getDependencyByGroupArtifact(javaProject.getDependenciesWithTransitive(), groupId, artifactId);
    }

    public static Dependency getDependency(JavaProject jkubeProject, String groupId, String artifactId) {
        return getDependencyByGroupArtifact(jkubeProject.getDependencies(), groupId, artifactId);
    }

  /**
   * Checks if the resources specified in the provided paths exist in the project.
   *
   * @param project where the resources may exist
   * @param paths within the project where the resources exist
   * @return true if at least one of the provided resource paths exists within the project, false otherwise.
   * @throws IOException if there's a problem reading the resource
   */
  public static boolean hasResource(JavaProject project, String... paths) throws IOException {
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

    public static Properties getPropertiesWithSystemOverrides(JavaProject project) {
        final Properties properties = new Properties();
        properties.putAll(project.getProperties());
        properties.putAll(System.getProperties());
        return properties;
    }

  /**
   * Returns the output artifact for the Project.
   *
   * <p> This method infers the Project's artifact from the provided project build directory, GAV coordinates,
   *  packaging, and configured build name.
   *
   * <p> If the inferred artifact file exists it is returned.
   *
   * <p> If the inferred artifact file doesn't exist, but an existent artifact file is provided in the configuration, it is
   * returned instead.

   * <p> In case no artifacts are found or configured, this method returns null.
   *
   * <p> TODO: https://github.com/eclipse/jkube/issues/817
   * <br> This method prioritizes artifact inference instead of configuration. This is done to handle case of maven
   * where value of artifact varies depending upon the maven phase in which goal was executed.
   *
   * @see <a href="https://github.com/eclipse/jkube/issues/817">#817</a>
   *
   * @param jkubeProject project for which to retrieve the output artifact.
   * @return the final output artifact file or null if it doesn't exist.
   */
  public static File getFinalOutputArtifact(JavaProject jkubeProject) {
    final String inferredArtifactName;
    if (jkubeProject.getBuildFinalName() == null) {
      inferredArtifactName = String.format("%s-%s.%s",
          jkubeProject.getArtifactId(), jkubeProject.getVersion(), jkubeProject.getPackaging());
    } else {
      inferredArtifactName = String.format("%s.%s",
          jkubeProject.getBuildFinalName(), jkubeProject.getPackaging());
    }
    final File inferredArtifact = new File(jkubeProject.getBuildDirectory(), inferredArtifactName);
    if (inferredArtifact.exists()) {
      return inferredArtifact;
    } else if (jkubeProject.getArtifact() != null && jkubeProject.getArtifact().exists()) {
      return jkubeProject.getArtifact();
    }
    return null;
  }

    public static String createDefaultResourceName(String artifactId, String ... suffixes) {
        String suffix = StringUtils.join(suffixes, "-");
        String ret = artifactId + (suffix.length() > 0 ? "-" + suffix : "");
        if (ret.length() > MAX_RESOURCE_NAME_LENGTH) {
            ret = ret.substring(0, MAX_RESOURCE_NAME_LENGTH);
        }
        return ret.toLowerCase();
    }

  public static URLClassLoader getClassLoader(JavaProject jKubeProject) {
    return ClassUtil.createClassLoader(
        jKubeProject.getCompileClassPathElements(),
        jKubeProject.getOutputDirectory().getAbsolutePath());
  }

  public static String getProperty(String key, JavaProject project) {
    String value = System.getProperty(key);
    if (value == null) {
      value = project.getProperties().getProperty(key);
    }
    return value;
  }

  public static File resolveArtifact(JavaProject project, String groupId, String artifactId, String version, String type) {
    File artifact = project.getDependencies().stream()
        .filter(d -> d.getGroupId().equals(groupId)
            && d.getArtifactId().equals(artifactId)
            && d.getVersion().equals(version)
            && d.getType().equals(type))
        .findFirst()
        .map(Dependency::getFile)
        .orElse(null);
    if (artifact == null) {
      throw new IllegalStateException("Cannot find artifact " + String.format("%s-%s.%s", artifactId, version, type) + " within the resolved resources");
    }
    return artifact;
  }

  private static Dependency getDependencyByGroupArtifact(List<Dependency> dependencyList, String groupId, String artifactId) {
    if (dependencyList != null) {
      return iterateOverListWithCondition(dependencyList, dependency ->
          Objects.equals(dependency.getGroupId(), groupId) && Objects.equals(dependency.getArtifactId(), artifactId))
          .orElse(null);
    }
    return null;
  }
}
