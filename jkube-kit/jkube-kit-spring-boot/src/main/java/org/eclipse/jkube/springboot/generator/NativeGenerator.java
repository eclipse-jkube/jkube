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
package org.eclipse.jkube.springboot.generator;

import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.api.GeneratorConfig;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;

import java.io.File;
import java.util.List;

import static org.eclipse.jkube.kit.common.util.FileUtil.getRelativePath;

public class NativeGenerator extends AbstractSpringBootNestedGenerator {
  private final File nativeBinary;
  private final FromSelector fromSelector;

  public NativeGenerator(GeneratorContext generatorContext, GeneratorConfig generatorConfig, File nativeBinary) {
    super(generatorContext, generatorConfig);
    this.nativeBinary = nativeBinary;
    fromSelector = new FromSelector.Default(generatorContext, "springboot-native");
  }


  @Override
  public String getFrom() {
    return fromSelector.getFrom();
  }

  @Override
  public String getDefaultJolokiaPort() {
    return "0";
  }

  @Override
  public String getDefaultPrometheusPort() {
    return "0";
  }

  @Override
  public Arguments getBuildEntryPoint() {
    return Arguments.builder()
        .execArgument("./" + nativeBinary.getName())
        .build();
  }

  @Override
  public String getBuildWorkdir() {
    return "/";
  }

  @Override
  public String getTargetDir() {
    return "/";
  }

  @Override
  public AssemblyConfiguration createAssemblyConfiguration(List<AssemblyFileSet> defaultFileSets) {
    Assembly.AssemblyBuilder assemblyBuilder = Assembly.builder();
    final JavaProject project = getProject();
    final AssemblyFileSet.AssemblyFileSetBuilder artifactFileSetBuilder = AssemblyFileSet.builder()
        .outputDirectory(new File("."))
        .directory(getRelativePath(project.getBaseDirectory(), nativeBinary.getParentFile()))
        .fileMode("0755");
    artifactFileSetBuilder.include(nativeBinary.getName());

    assemblyBuilder.fileSets(defaultFileSets);
    assemblyBuilder.fileSet(artifactFileSetBuilder.build());

    return AssemblyConfiguration.builder()
        .targetDir(getTargetDir())
        .excludeFinalOutputArtifact(true)
        .layer(assemblyBuilder.build())
        .build();
  }
}