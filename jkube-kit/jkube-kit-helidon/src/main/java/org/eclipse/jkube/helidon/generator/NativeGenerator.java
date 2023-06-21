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
package org.eclipse.jkube.helidon.generator;

import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.api.GeneratorConfig;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;

import java.io.File;

import static org.eclipse.jkube.kit.common.util.FileUtil.getRelativePath;

public class NativeGenerator extends AbstractHelidonNestedGenerator {

  private final FromSelector fromSelector;

  public NativeGenerator(GeneratorContext generatorContext, GeneratorConfig generatorConfig) {
    super(generatorContext, generatorConfig);
    fromSelector = new FromSelector.Default(generatorContext, "helidon-native");
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
      .execArgument("./" + getProject().getArtifactId())
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
  public AssemblyConfiguration createAssemblyConfiguration() {
    final JavaProject project = getProject();
    final AssemblyFileSet.AssemblyFileSetBuilder artifactFileSetBuilder = AssemblyFileSet.builder()
      .outputDirectory(new File("."))
      .directory(getRelativePath(project.getBaseDirectory(), project.getBuildDirectory()))
      .fileMode("0755");
    File nativeBinary = new File(project.getBuildDirectory(), project.getBuildFinalName());
    if (nativeBinary.exists()) {
      artifactFileSetBuilder.include(nativeBinary.getName());
    }
    return AssemblyConfiguration.builder()
      .targetDir(getTargetDir())
      .excludeFinalOutputArtifact(true)
      .layer(Assembly.builder().fileSet(artifactFileSetBuilder.build()).build())
      .build();
  }
}
