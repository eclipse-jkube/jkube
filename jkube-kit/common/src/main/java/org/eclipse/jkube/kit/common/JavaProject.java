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

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Model class that represents a Java Project to be processed by JKube
 */
@SuppressWarnings("JavaDoc")
@Getter
@Setter
@EqualsAndHashCode
public class JavaProject implements Serializable {

  private static final long serialVersionUID = 6438404976521622633L;

  /**
   * Project's name.
   *
   * @param name New name for the project.
   * @return The name of the project.
   */
  private String name;
  /**
   * Maven group ID
   *
   * @param groupId New maven group ID for the project.
   * @return The maven group ID for the project.
   */
  private String groupId;
  /**
   * Maven artifact ID
   *
   * @param artifactId New maven artifact ID for the project.
   * @return The maven artifact ID for the project.
   */
  private String artifactId;
  /**
   * Maven version
   *
   * @param version New maven version for the project.
   * @return The maven version for the project.
   */
  private String version;
  /**
   *  Directory where compiled application classes are located (e.g. target/classes).
   *
   * @param outputDirectory New output directory for the project.
   * @return The output directory for the project.
   */
  private File outputDirectory;
  /**
   * Project's base directory
   *
   * @param baseDirectory New base directory for the project.
   * @return The base directory for the project.
   */
  private File baseDirectory;
  /**
   * Directory where all build files are located (e.g. target)
   *
   * @param buildDirectory New build directory for the project.
   * @return The build directory for the project.
   */
  private File buildDirectory;

  /**
   * Directory where build packages or artifacts (jar/war/ear) are output.
   * This can be <code>target</code> in case of Maven and <code>build/libs</code> in case of Gradle.
   *
   * <p>
   * This directory is used by generators and other build related tools to locate the
   * files and packages to be included in the Container Image.
   *
   * <p>
   * Please note that it's different from buildDirectory or outputDirectory which may point to directory
   * used by build tool or generated classes.
   *
   * @param buildPackageDirectory directory where the project artifacts and packages are output
   * @return the directory where the project artifacts and packages are output
   */
  private File buildPackageDirectory;

  /**
   * Project configuration properties to be used in generators and enrichers
   *
   * @param properties New configuration properties for the project.
   * @return The properties for the project.
   */
  private Properties properties;
  /**
   * Project Classpath entries.
   *
   * @param compileClassPathElements New classpath entries for the project.
   * @return The project's compile classpath elements.
   */
  private List<String> compileClassPathElements;
  /**
   * Direct dependencies for the project.
   *
   * @param dependencies New direct dependencies for the project.
   * @return The project's direct dependency list.
   */
  private List<Dependency> dependencies;
  /**
   * All dependencies (including transitive) for the project
   *
   * @param dependenciesWithTransitive New dependencies for the project.
   * @return The project's dependency list.
   */
  private List<Dependency> dependenciesWithTransitive;
  /**
   * List of plugins (e.g. Maven plugins) for the project.
   *
   * @param plugins New plugins for the project.
   * @return The project's plugins.
   */
  private List<Plugin> plugins;
  /**
   * URL to the project's homepage.
   *
   * @param site New homepage for the project.
   * @return The project's homepage.
   */
  private String site;
  /**
   * Project's description.
   *
   * @param description New description for the project.
   * @return The project's description.
   */
  private String description;
  /**
   * Full name of the project's organization.
   *
   * @param organizationName New organization name for the project.
   * @return The project's organization full name.
   */
  private String organizationName;
  /**
   * URL to the project's documentation.
   *
   * @param documentationUrl New documentation URL for the project.
   * @return The project's documentation URL.
   */
  private String documentationUrl;
  /**
   * Filename (excluding the extension, and with no path information) that the produced project artifact will be called.
   *
   * <p> (e.g. The default value for Maven is ${artifactId}-${version}).
   *
   * @param buildFinalName New filename for the project's produced artifact.
   * @return The project's produced artifact filename.
   */
  private String buildFinalName;
  /**
   * Generated Artifact File for the project
   *
   * @param artifact New project's generated artifact file.
   * @return The project's generated artifact file.
   */
  private File artifact;
  /**
   * Project's packaging type. Specifies the type of artifact the project produces.
   *
   * <p> (e.g. war, jar, ear...)
   *
   * @param packaging New packaging type for the project.
   * @return The project's packaging type.
   */
  private String packaging;
  /**
   * Name of the issue management system for the project (e.g. Bugzilla).
   *
   * @param issueManagementSystem New issue management system for the project.
   * @return The project's issue management system.
   */
  private String issueManagementSystem;
  /**
   * URL for the issue management system used by the project.
   *
   * @param issueManagementUrl New issue management URL for the project.
   * @return The project's issue management URL.
   */
  private String issueManagementUrl;

  /**
   * URL for website of the project.
   *
   * @param url URL for website of project.
   * @return The project's website URL.
   */
  private String url;
  /**
   * URL for the project's browsable SCM repository.
   *
   * @param scmUrl New SCM URL for the project.
   * @return The project's SCM URL.
   */
  private String scmUrl;
  /**
   * Tag of the project's current code.
   *
   * @param scmTag New SCM tag for the project.
   * @return The project's SCM tag.
   */
  private String scmTag;

  /**
   * List of Maintainers involved with the project.
   * @param maintainers New maintainers for the project.
   * @return The project maintainers.
   */
  private List<Maintainer> maintainers;

  @Builder
  public JavaProject(
      String name, String groupId, String artifactId, String version,
      File outputDirectory, File baseDirectory, File buildDirectory, File buildPackageDirectory,
      Properties properties, @Singular List<String> compileClassPathElements, @Singular List<Dependency> dependencies,
      List<Dependency> dependenciesWithTransitive, @Singular List<Plugin> plugins,
      String site, String description, String organizationName, String documentationUrl,
      String buildFinalName, File artifact, String packaging, String issueManagementSystem, String issueManagementUrl,
      String url, String scmUrl, String scmTag,
      @Singular List<Maintainer> maintainers) {

    this.name = name;
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.outputDirectory = outputDirectory;
    this.baseDirectory = baseDirectory;
    this.buildDirectory = buildDirectory;
    this.properties = Optional.ofNullable(properties).orElse(new Properties());
    this.compileClassPathElements = compileClassPathElements;
    this.dependencies = dependencies;
    this.dependenciesWithTransitive = dependenciesWithTransitive;
    this.plugins = plugins;
    this.site = site;
    this.description = description;
    this.organizationName = organizationName;
    this.documentationUrl = documentationUrl;
    this.buildFinalName = buildFinalName;
    this.artifact = artifact;
    this.packaging = packaging;
    this.issueManagementSystem = issueManagementSystem;
    this.issueManagementUrl = issueManagementUrl;
    this.url = url;
    this.scmUrl = scmUrl;
    this.scmTag = scmTag;
    this.buildPackageDirectory = buildPackageDirectory;
    this.maintainers = maintainers;
  }
}

