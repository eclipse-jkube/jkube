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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;

import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationPublications;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.PublishArtifactSet;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.internal.deprecation.DeprecatableConfiguration;

public class GradleUtil {

  private static final Path DEFAULT_CLASSES_DIR = Paths.get("classes", "java", "main");

  private GradleUtil() {}

  public static JavaProject convertGradleProject(Project gradleProject) {
    final File artifact = findArtifact(gradleProject);
    return JavaProject.builder()
        .properties(extractProperties(gradleProject))
        .name(gradleProject.getName())
        .description(gradleProject.getDescription())
        .groupId(Objects.toString(gradleProject.getGroup()))
        .artifactId(gradleProject.getName())
        .version(Objects.toString(gradleProject.getVersion()))
        .baseDirectory(gradleProject.getProjectDir())
//        .documentationUrl(gradleProject.)
        .compileClassPathElements(extractClassPath(gradleProject))
//        .packaging(gradleProject)
        .dependencies(extractDependencies(gradleProject))
        .dependenciesWithTransitive(extractDependenciesWithTransitive(gradleProject))
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
        .artifact(artifact)
        .buildPackageDirectory(artifact != null ? artifact.getParentFile() : null)
        .build();
  }

  private static Properties extractProperties(Project gradleProject) {
    return Stream.concat(gradleProject.getProperties().entrySet().stream(), System.getProperties().entrySet().stream())
      .filter(e -> Objects.nonNull(e.getValue()))
      .reduce(new Properties(), (acc, e) -> {
        acc.put(e.getKey(), e.getValue());
        return acc;
      }, (acc, e) -> acc);
  }

  private static List<Dependency> extractDependencies(Project gradleProject) {
    return extractDependencies(gradleProject, rr -> rr.getRoot().getDependencies());
  }

  private static List<Dependency> extractDependenciesWithTransitive(Project gradleProject) {
    return extractDependencies(gradleProject, ResolutionResult::getAllDependencies);
  }

  private static List<Dependency> extractDependencies(Project gradleProject,
      Function<ResolutionResult, Set<? extends DependencyResult>> resolutionToDependency) {
    return gradleProject.getConfigurations().stream()
        .filter(GradleUtil::canBeResolved)
        .map(Configuration::getIncoming)
        .map(ResolvableDependencies::getResolutionResult)
        .map(resolutionToDependency)
        .flatMap(Set::stream)
        .filter(ResolvedDependencyResult.class::isInstance)
        .map(ResolvedDependencyResult.class::cast)
        .map(ResolvedDependencyResult::getSelected)
        .map(ResolvedComponentResult::getModuleVersion)
        .filter(Objects::nonNull)
        .map(mv -> Dependency.builder().groupId(mv.getGroup()).artifactId(mv.getName()).version(mv.getVersion()).build())
        .distinct()
        .collect(Collectors.toList());
  }

  private static List<Plugin> extractPlugins(Project gradleProject) {
    return gradleProject.getBuildscript().getConfigurations().stream()
        .filter(GradleUtil::canBeResolved)
        .map(Configuration::getAllDependencies).flatMap(DependencySet::stream)
        .filter(d -> d.getName().toLowerCase(Locale.ENGLISH).endsWith("gradle.plugin"))
        .map(d -> Plugin.builder().groupId(d.getGroup()).artifactId(d.getName()).version(d.getVersion()).build())
        .distinct()
        .collect(Collectors.toList());
  }

  private static SourceSetContainer extractSourceSets(Project gradleProject) {
    return gradleProject.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
  }

  private static List<String> extractClassPath(Project gradleProject) {
    final SourceSetContainer sourceSetContainer = extractSourceSets(gradleProject);
    if (sourceSetContainer != null) {
      return sourceSetContainer.stream()
          .map(SourceSet::getCompileClasspath)
          .map(FileCollection::getFiles)
          .flatMap(Collection::stream)
          .map(File::getAbsolutePath)
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  private static File findClassesOutputDirectory(Project gradleProject) {
    try {
      final SourceSetContainer sourceSetContainer = extractSourceSets(gradleProject);
      if (sourceSetContainer != null) {
        return sourceSetContainer.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getJava().getDestinationDirectory()
            .getAsFile().getOrNull();
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

  static boolean canBeResolved(Configuration configuration) {
    boolean isDeprecatedForResolving = configuration instanceof DeprecatableConfiguration
        && ((DeprecatableConfiguration) configuration).getResolutionAlternatives() != null;
    return configuration.isCanBeResolved() && !isDeprecatedForResolving;
  }
}
