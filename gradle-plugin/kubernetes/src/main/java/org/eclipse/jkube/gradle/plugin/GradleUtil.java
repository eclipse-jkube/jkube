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
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationPublications;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.PublishArtifactSet;
import org.gradle.api.file.FileCollection;

public class GradleUtil {

  private GradleUtil() {}

  public static JavaProject convertGradleProject(Project gradleProject) {
    return JavaProject.builder()
        .properties(extractProperties(gradleProject))
        .name(gradleProject.getName())
        .description(gradleProject.getDescription())
        .groupId(Objects.toString(gradleProject.getGroup()))
        .artifactId(gradleProject.getName())
        .version(Objects.toString(gradleProject.getVersion()))
        .baseDirectory(gradleProject.getProjectDir())
//        .documentationUrl(gradleProject.)
//        .compileClassPathElements(gradleProject.)
//        .packaging(gradleProject.)
        .dependencies(extractDependencies(gradleProject))
//        .dependenciesWithTransitive(gradleProject.getDependencies().)
//        .localRepositoryBaseDirectory(gradleProject.)
        .plugins(extractPlugins(gradleProject))
//
//        .site(gradleProject.)
//        .organizationName(gradleProject.)
//
        .outputDirectory(gradleProject.getBuildDir())
//        .buildFinalName(gradleProject.)
        .buildDirectory(gradleProject.getBuildDir())
//
//        .issueManagementSystem(gradleProject.)
//        .issueManagementUrl(gradleProject.)
//
//        .scmTag(gradleProject.)
//        .scmUrl(gradleProject.)
        .artifact(findArtifact(gradleProject))
        .build();
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

  private static File findArtifact(Project gradleProject) {
    return gradleProject.getConfigurations().stream()
        .map(Configuration::getOutgoing)
        .map(ConfigurationPublications::getArtifacts)
        .map(PublishArtifactSet::getFiles)
        .map(FileCollection::getFiles)
        .flatMap(Set::stream)
        .distinct()
        .filter(File::exists)
        .findAny().orElse(null);
  }

}
