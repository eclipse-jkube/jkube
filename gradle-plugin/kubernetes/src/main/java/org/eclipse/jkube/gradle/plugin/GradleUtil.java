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
package org.eclipse.jkube.gradle.plugin;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;

import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationPublications;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.PublishArtifactSet;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

public class GradleUtil {

  private static final Path DEFAULT_CLASSES_DIR = Paths.get("classes", "java", "main");

  private GradleUtil() {}

  public static JavaProject convertGradleProject(Project gradleProject) {
    File artifact = findArtifact(gradleProject);
    JavaProject.JavaProjectBuilder builder = JavaProject.builder()
        .properties(extractProperties(gradleProject))
        .name(gradleProject.getName())
        .description(gradleProject.getDescription())
        .groupId(Objects.toString(gradleProject.getGroup()))
        .artifactId(gradleProject.getName())
        .version(Objects.toString(gradleProject.getVersion()))
        .baseDirectory(gradleProject.getProjectDir())
//        .documentationUrl(gradleProject.)
//        .compileClassPathElements(gradleProject.)
//        .packaging(gradleProject)
        .dependencies(extractDependencies(gradleProject))
        .dependenciesWithTransitive(extractDependencies(gradleProject))
//        .localRepositoryBaseDirectory(gradleProject.)
        .plugins(extractPlugins(gradleProject))
//
//        .site(gradleProject.)
//        .organizationName(gradleProject.)
//
        .outputDirectory(findClassesOutputDirectory(gradleProject))
//        .buildFinalName(gradleProject.)
        .buildDirectory(gradleProject.getBuildDir())
//        .issueManagementSystem(gradleProject.)
//        .issueManagementUrl(gradleProject.)
//
//        .scmTag(gradleProject.)
//        .scmUrl(gradleProject.)
        .artifact(artifact);

    if (artifact != null) {
      builder.buildPackageDirectory(artifact.getParentFile());
    }
    return builder.build();
  }

  private static Properties extractProperties(Project gradleProject) {
    return gradleProject.getProperties().entrySet().stream().filter(e -> Objects.nonNull(e.getValue()))
        .reduce(new Properties(), (acc, e) -> {
          acc.put(e.getKey(), e.getValue());
          return acc;
        }, (acc, e) -> acc);
  }

  private static List<Dependency> extractDependencies(Project gradleProject) {
    return gradleProject.getConfigurations().stream()
        .map(Configuration::getAllDependencies).flatMap(DependencySet::stream)
        .map(d -> Dependency.builder().groupId(d.getGroup()).artifactId(d.getName()).version(d.getVersion()).build())
        .distinct()
        .collect(Collectors.toList());
  }

  private static List<Plugin> extractPlugins(Project gradleProject) {
    return gradleProject.getBuildscript().getConfigurations().stream()
        .map(Configuration::getAllDependencies).flatMap(DependencySet::stream)
        .filter(d -> d.getName().toLowerCase(Locale.ENGLISH).endsWith("gradle.plugin"))
        .map(d -> Plugin.builder().groupId(d.getGroup()).artifactId(d.getName()).version(d.getVersion()).build())
        .distinct()
        .collect(Collectors.toList());
  }

  private static File findClassesOutputDirectory(Project gradleProject) {
    try {
      final SourceSetContainer sourceSetContainer = gradleProject.getConvention().getPlugin(JavaPluginConvention.class)
          .getSourceSets();
      if (sourceSetContainer != null) {
        return sourceSetContainer.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getJava().getOutputDir();
      }
    } catch (IllegalStateException | UnknownDomainObjectException ex) {
      // No matching SourceSet was found
    }
    return gradleProject.getBuildDir().toPath().resolve(DEFAULT_CLASSES_DIR).toFile();
  }

  private static File findArtifact(Project gradleProject) {
    return gradleProject.getConfigurations().stream()
        .map(Configuration::getOutgoing)
        .map(ConfigurationPublications::getArtifacts)
        .map(PublishArtifactSet::getFiles)
        .map(FileCollection::getFiles)
        .flatMap(Set::stream)
        .sorted((o1, o2) -> (int)(o2.length() - o1.length()))
        .distinct()
        .filter(File::exists)
        .findFirst().orElse(null);
  }

}
