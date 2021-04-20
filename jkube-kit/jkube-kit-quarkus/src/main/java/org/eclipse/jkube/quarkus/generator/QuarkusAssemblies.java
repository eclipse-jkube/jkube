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

import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.eclipse.jkube.quarkus.QuarkusUtils.findSingleFileThatEndsWith;
import static org.eclipse.jkube.quarkus.QuarkusUtils.getQuarkusConfiguration;
import static org.eclipse.jkube.quarkus.QuarkusUtils.runnerSuffix;

public class QuarkusAssemblies {

  public static final QuarkusAssembly NATIVE = quarkusGenerator -> {
    final JavaProject project = quarkusGenerator.getContext().getProject();
    final Properties quarkusConfiguration = getQuarkusConfiguration(project);
    final List<String> relativePaths = new ArrayList<>();
    relativePaths.add(findSingleFileThatEndsWith(project, runnerSuffix(quarkusConfiguration)));
    final AssemblyFileSet fileSet = AssemblyFileSet.builder()
        .directory(FileUtil.getRelativePath(project.getBaseDirectory(), project.getBuildDirectory()))
        .includes(relativePaths)
        .fileMode("0755")
        .build();
    return createAssemblyConfiguration(quarkusGenerator.getBuildWorkdir(), fileSet);
  };

  public static final QuarkusAssembly FAST_JAR = quarkusGenerator -> {
    final JavaProject project = quarkusGenerator.getContext().getProject();
    final File quarkusAppDirectory = new File(project.getBuildDirectory(), "quarkus-app");
    if (!quarkusAppDirectory.exists()) {
      throw new IllegalStateException("The quarkus-app directory required in Quarkus Fast Jar mode was not found");
    }
    AssemblyFileSet.AssemblyFileSetBuilder fileSetBuilder = AssemblyFileSet.builder()
        .directory(FileUtil.getRelativePath(project.getBaseDirectory(), quarkusAppDirectory))
        .include("quarkus-run.jar")
        .include("*")
        .include("**/*")
        .fileMode("0640");
    addDefaultArtifactExclude(project, fileSetBuilder);
    return createAssemblyConfiguration(quarkusGenerator.getBuildWorkdir(), fileSetBuilder.build());
  };

  public static final QuarkusAssembly LEGACY_JAR = quarkusGenerator -> {
    final JavaProject project = quarkusGenerator.getContext().getProject();
    AssemblyFileSet.AssemblyFileSetBuilder fileSetBuilder = AssemblyFileSet.builder()
        .directory(FileUtil.getRelativePath(project.getBaseDirectory(), project.getBuildDirectory()))
        .include(findSingleFileThatEndsWith(project, runnerSuffix(getQuarkusConfiguration(project)) + ".jar"))
        .include("lib")
        .fileMode("0640");
    addDefaultArtifactExclude(project, fileSetBuilder);
    return createAssemblyConfiguration(quarkusGenerator.getBuildWorkdir(), fileSetBuilder.build());
  };

  public static final QuarkusAssembly UBER_JAR = quarkusGenerator -> {
    final JavaProject project = quarkusGenerator.getContext().getProject();
    AssemblyFileSet.AssemblyFileSetBuilder fileSetBuilder = AssemblyFileSet.builder()
        .directory(FileUtil.getRelativePath(project.getBaseDirectory(), project.getBuildDirectory()))
        .include(findSingleFileThatEndsWith(project, runnerSuffix(getQuarkusConfiguration(project)) + ".jar"))
        .fileMode("0640");
    addDefaultArtifactExclude(project, fileSetBuilder);
    return createAssemblyConfiguration(quarkusGenerator.getBuildWorkdir(), fileSetBuilder.build());
  };

  private static void addDefaultArtifactExclude(JavaProject project, AssemblyFileSet.AssemblyFileSetBuilder fileSetBuilder) {
    // We also need to exclude default jar file
    File defaultJarFile = JKubeProjectUtil.getFinalOutputArtifact(project);
    if (defaultJarFile != null) {
      fileSetBuilder.exclude(defaultJarFile.getName());
    }
  }

  private static AssemblyConfiguration createAssemblyConfiguration(String targetDir, AssemblyFileSet jKubeAssemblyFileSet) {
    jKubeAssemblyFileSet.setOutputDirectory(".");
    return AssemblyConfiguration.builder()
        .targetDir(targetDir)
        .excludeFinalOutputArtifact(true)
        .inline(Assembly.builder().fileSet(jKubeAssemblyFileSet).build())
        .build();
  }

  @FunctionalInterface
  public interface QuarkusAssembly {
    AssemblyConfiguration createAssemblyConfiguration(QuarkusGenerator quarkusGenerator);
  }
}
