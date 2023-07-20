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

import org.eclipse.jkube.generator.api.GeneratorConfig;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

import java.io.File;

import static org.eclipse.jkube.kit.common.util.FileUtil.getRelativePath;

public class StandardGenerator extends AbstractHelidonNestedGenerator {

  public StandardGenerator(GeneratorContext generatorContext, GeneratorConfig generatorConfig) {
    super(generatorContext, generatorConfig);
  }

  @Override
  public AssemblyConfiguration createAssemblyConfiguration() {
    final JavaProject project = getProject();
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
    return AssemblyConfiguration.builder()
      .targetDir(getTargetDir())
      .excludeFinalOutputArtifact(true)
      .layer(Assembly.builder().id("libs").fileSet(libFileSet.build()).build())
      .layer(Assembly.builder().id("artifact").fileSet(artifactFileSet.build()).build())
      .build();
  }
}
