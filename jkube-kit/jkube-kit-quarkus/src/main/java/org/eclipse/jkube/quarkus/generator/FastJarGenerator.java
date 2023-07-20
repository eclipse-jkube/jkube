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
package org.eclipse.jkube.quarkus.generator;

import org.eclipse.jkube.generator.api.GeneratorConfig;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.FileUtil;

import java.io.File;

public class FastJarGenerator extends AbstractQuarkusNestedGenerator {

  public FastJarGenerator(GeneratorContext generatorContext, GeneratorConfig generatorConfig) {
    super(generatorContext, generatorConfig);
  }

  @Override
  public AssemblyConfiguration createAssemblyConfiguration() {
    final JavaProject project = getProject();
    final File quarkusAppDirectory = new File(project.getBuildDirectory(), "quarkus-app");
    if (!quarkusAppDirectory.exists()) {
      throw new IllegalStateException("The quarkus-app directory required in Quarkus Fast Jar mode was not found");
    }
    AssemblyFileSet.AssemblyFileSetBuilder libFileSet = createFileSet()
      .directory(FileUtil.getRelativePath(project.getBaseDirectory(), quarkusAppDirectory))
      .include("lib")
      .fileMode("0640");
    AssemblyFileSet.AssemblyFileSetBuilder fastJarFileSet = createFileSet()
      .directory(FileUtil.getRelativePath(project.getBaseDirectory(), quarkusAppDirectory))
      .include("quarkus-run.jar")
      .include("*")
      .include("**/*")
      .exclude("lib/**/*")
      .exclude("lib/*")
      .fileMode("0640");
    return AssemblyConfiguration.builder()
      .targetDir(getTargetDir())
      .excludeFinalOutputArtifact(true)
      .layer(Assembly.builder().id("lib").fileSet(libFileSet.build()).build())
      .layer(Assembly.builder().id("fast-jar").fileSet(fastJarFileSet.build()).build())
      .build();
  }
}
