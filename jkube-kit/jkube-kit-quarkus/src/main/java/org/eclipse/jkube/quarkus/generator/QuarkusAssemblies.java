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
package org.eclipse.jkube.quarkus.generator;

import java.io.File;
import java.util.Properties;

import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

import static org.eclipse.jkube.quarkus.QuarkusUtils.findSingleFileThatEndsWith;
import static org.eclipse.jkube.quarkus.QuarkusUtils.getQuarkusConfiguration;
import static org.eclipse.jkube.quarkus.QuarkusUtils.runnerSuffix;

public class QuarkusAssemblies {

  public static final QuarkusAssembly NATIVE = quarkusGenerator -> {
    final JavaProject project = quarkusGenerator.getContext().getProject();
    final Properties quarkusConfiguration = getQuarkusConfiguration(project);
    final AssemblyFileSet artifactFileSet = createFileSet(project)
        .directory(FileUtil.getRelativePath(project.getBaseDirectory(), project.getBuildDirectory()))
        .include(findSingleFileThatEndsWith(project, runnerSuffix(quarkusConfiguration)))
        .fileMode("0755")
        .build();
    return createAssemblyConfiguration(quarkusGenerator.getBuildWorkdir())
        .layer(Assembly.builder().fileSet(artifactFileSet).build())
        .build();
  };

  public static final QuarkusAssembly FAST_JAR = quarkusGenerator -> {
    final JavaProject project = quarkusGenerator.getContext().getProject();
    final File quarkusAppDirectory = new File(project.getBuildDirectory(), "quarkus-app");
    if (!quarkusAppDirectory.exists()) {
      throw new IllegalStateException("The quarkus-app directory required in Quarkus Fast Jar mode was not found");
    }
    AssemblyFileSet.AssemblyFileSetBuilder libFileSet = createFileSet(project)
        .directory(FileUtil.getRelativePath(project.getBaseDirectory(), quarkusAppDirectory))
        .include("lib")
        .fileMode("0640");
    AssemblyFileSet.AssemblyFileSetBuilder fastJarFileSet = createFileSet(project)
        .directory(FileUtil.getRelativePath(project.getBaseDirectory(), quarkusAppDirectory))
        .include("quarkus-run.jar")
        .include("*")
        .include("**/*")
        .exclude("lib/**/*")
        .exclude("lib/*")
        .fileMode("0640");
    return createAssemblyConfiguration(quarkusGenerator.getBuildWorkdir())
        .layer(Assembly.builder().id("lib").fileSet(libFileSet.build()).build())
        .layer(Assembly.builder().id("fast-jar").fileSet(fastJarFileSet.build()).build())
        .build();
  };

  public static final QuarkusAssembly LEGACY_JAR = quarkusGenerator -> {
    final JavaProject project = quarkusGenerator.getContext().getProject();
    AssemblyFileSet.AssemblyFileSetBuilder libFileSet = createFileSet(project)
        .directory(FileUtil.getRelativePath(project.getBaseDirectory(), project.getBuildDirectory()))
        .include("lib")
        .fileMode("0640");
    AssemblyFileSet.AssemblyFileSetBuilder artifactFileSet = createFileSet(project)
        .directory(FileUtil.getRelativePath(project.getBaseDirectory(), project.getBuildDirectory()))
        .include(findSingleFileThatEndsWith(project, runnerSuffix(getQuarkusConfiguration(project)) + ".jar"))
        .fileMode("0640");
    return createAssemblyConfiguration(quarkusGenerator.getBuildWorkdir())
        .layer(Assembly.builder().id("lib").fileSet(libFileSet.build()).build())
        .layer(Assembly.builder().id("artifact").fileSet(artifactFileSet.build()).build())
        .build();
  };

  public static final QuarkusAssembly UBER_JAR = quarkusGenerator -> {
    final JavaProject project = quarkusGenerator.getContext().getProject();
    final AssemblyFileSet.AssemblyFileSetBuilder fileSetBuilder = createFileSet(project)
        .directory(FileUtil.getRelativePath(project.getBaseDirectory(), project.getBuildDirectory()))
        .include(findSingleFileThatEndsWith(project, runnerSuffix(getQuarkusConfiguration(project)) + ".jar"))
        .fileMode("0640");
    return createAssemblyConfiguration(quarkusGenerator.getBuildWorkdir())
        .layer(Assembly.builder().fileSet(fileSetBuilder.build()).build())
        .build();
  };

  private static AssemblyFileSet.AssemblyFileSetBuilder createFileSet(JavaProject project) {
    final AssemblyFileSet.AssemblyFileSetBuilder assemblyFileSetBuilder = AssemblyFileSet.builder()
        .outputDirectory(new File("."));
    // We also need to exclude default jar file
    File defaultJarFile = JKubeProjectUtil.getFinalOutputArtifact(project);
    if (defaultJarFile != null) {
      assemblyFileSetBuilder.exclude(defaultJarFile.getName());
    }
    return assemblyFileSetBuilder;
  }

  private static AssemblyConfiguration.AssemblyConfigurationBuilder createAssemblyConfiguration(String targetDir) {
    return AssemblyConfiguration.builder()
        .targetDir(targetDir)
        .excludeFinalOutputArtifact(true);
  }

  @FunctionalInterface
  public interface QuarkusAssembly {
    AssemblyConfiguration createAssemblyConfiguration(QuarkusGenerator quarkusGenerator);
  }
}
