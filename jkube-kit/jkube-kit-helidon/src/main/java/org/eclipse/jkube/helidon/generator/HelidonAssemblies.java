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
package org.eclipse.jkube.helidon.generator;

import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

import java.io.File;

import static org.eclipse.jkube.kit.common.util.FileUtil.getRelativePath;

public class HelidonAssemblies {
  public static final HelidonAssembly NATIVE = helidonGenerator -> {
    final JavaProject project = helidonGenerator.getContext().getProject();
    final AssemblyFileSet.AssemblyFileSetBuilder artifactFileSetBuilder = AssemblyFileSet.builder()
        .outputDirectory(new File("."))
        .directory(getRelativePath(project.getBaseDirectory(), project.getBuildDirectory()))
        .fileMode("0755");
    File nativeBinary = new File(project.getBuildDirectory(), project.getBuildFinalName());
    if (nativeBinary.exists()) {
      artifactFileSetBuilder.include(nativeBinary.getName());
    }
    return createAssemblyConfiguration(helidonGenerator.getBuildWorkdir())
        .layer(Assembly.builder().fileSet(artifactFileSetBuilder.build()).build())
        .build();
  };

  public static final HelidonAssembly STANDARD = helidonGenerator -> {
    final JavaProject project = helidonGenerator.getContext().getProject();
    AssemblyFileSet.AssemblyFileSetBuilder libFileSet = AssemblyFileSet.builder()
        .outputDirectory(new File("."))
        .directory(getRelativePath(project.getBaseDirectory(), project.getBuildPackageDirectory()))
        .include("libs")
        .fileMode("0640");
    AssemblyFileSet.AssemblyFileSetBuilder artifactFileSet = AssemblyFileSet.builder()
        .outputDirectory(new File("."))
        .directory(getRelativePath(project.getBaseDirectory(), project.getBuildPackageDirectory()))
        .fileMode("0640");
    File defaultArtifactFile = JKubeProjectUtil.getFinalOutputArtifact(project);
    if (defaultArtifactFile != null) {
      artifactFileSet.include(getRelativePath(project.getBuildPackageDirectory(), defaultArtifactFile).getPath());
    }
    return createAssemblyConfiguration(helidonGenerator.getBuildWorkdir())
        .layer(Assembly.builder().id("libs").fileSet(libFileSet.build()).build())
        .layer(Assembly.builder().id("artifact").fileSet(artifactFileSet.build()).build())
        .build();
  };

  private static AssemblyConfiguration.AssemblyConfigurationBuilder createAssemblyConfiguration(String targetDir) {
    return AssemblyConfiguration.builder()
        .targetDir(targetDir)
        .excludeFinalOutputArtifact(true);
  }

  @FunctionalInterface
  public interface HelidonAssembly {
    AssemblyConfiguration createAssemblyConfiguration(HelidonGenerator quarkusGenerator);
  }
}
