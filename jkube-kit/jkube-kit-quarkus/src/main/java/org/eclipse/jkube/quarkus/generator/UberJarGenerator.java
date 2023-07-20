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

import static org.eclipse.jkube.quarkus.QuarkusUtils.findSingleFileThatEndsWith;
import static org.eclipse.jkube.quarkus.QuarkusUtils.getQuarkusConfiguration;
import static org.eclipse.jkube.quarkus.QuarkusUtils.runnerSuffix;

public class UberJarGenerator extends AbstractQuarkusNestedGenerator {

  public UberJarGenerator(GeneratorContext generatorContext, GeneratorConfig generatorConfig) {
    super(generatorContext, generatorConfig);
  }

  @Override
  public boolean isFatJar() {
    return true;
  }

  @Override
  public AssemblyConfiguration createAssemblyConfiguration() {
    final JavaProject project = getProject();
    final AssemblyFileSet.AssemblyFileSetBuilder fileSetBuilder = createFileSet()
      .directory(FileUtil.getRelativePath(project.getBaseDirectory(), project.getBuildDirectory()))
      .include(findSingleFileThatEndsWith(project, runnerSuffix(getQuarkusConfiguration(project)) + ".jar"))
      .fileMode("0640");
    return AssemblyConfiguration.builder()
      .targetDir(getTargetDir())
      .excludeFinalOutputArtifact(true)
      .layer(Assembly.builder().fileSet(fileSetBuilder.build()).build())
      .build();
  }
}
