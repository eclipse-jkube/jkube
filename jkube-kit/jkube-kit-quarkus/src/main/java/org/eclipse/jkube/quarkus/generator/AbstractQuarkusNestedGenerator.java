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

import org.eclipse.jkube.generator.api.GeneratorConfig;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import java.io.File;

public abstract class AbstractQuarkusNestedGenerator implements QuarkusNestedGenerator {


  private final GeneratorContext generatorContext;
  private final GeneratorConfig generatorConfig;

  AbstractQuarkusNestedGenerator(GeneratorContext generatorContext, GeneratorConfig generatorConfig) {
    this.generatorContext = generatorContext;
    this.generatorConfig = generatorConfig;
  }

  @Override
  public final JavaProject getProject() {
    return generatorContext.getProject();
  }

  @Override
  public final RuntimeMode getRuntimeMode() {
    return generatorContext.getRuntimeMode();
  }

  @Override
  public String getBuildWorkdir() {
    return generatorConfig.get(JavaExecGenerator.Config.TARGET_DIR);
  }

  @Override
  public String getTargetDir() {
    return generatorConfig.get(JavaExecGenerator.Config.TARGET_DIR);
  }

  AssemblyFileSet.AssemblyFileSetBuilder createFileSet() {
    final AssemblyFileSet.AssemblyFileSetBuilder assemblyFileSetBuilder = AssemblyFileSet.builder()
      .outputDirectory(new File("."));
    // We also need to exclude default jar file
    File defaultJarFile = JKubeProjectUtil.getFinalOutputArtifact(getProject());
    if (defaultJarFile != null) {
      assemblyFileSetBuilder.exclude(defaultJarFile.getName());
    }
    return assemblyFileSetBuilder;
  }
}
