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
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import java.util.Properties;

import static org.eclipse.jkube.quarkus.QuarkusUtils.findSingleFileThatEndsWith;
import static org.eclipse.jkube.quarkus.QuarkusUtils.getQuarkusConfiguration;
import static org.eclipse.jkube.quarkus.QuarkusUtils.runnerSuffix;

public class NativeGenerator extends AbstractQuarkusNestedGenerator {

  public NativeGenerator(GeneratorContext generatorContext, GeneratorConfig generatorConfig) {
    super(generatorContext, generatorConfig);
  }

  @Override
  public String getFrom() {
    // TODO: use value from property
    if (getRuntimeMode() != RuntimeMode.OPENSHIFT) {
      return "registry.access.redhat.com/ubi8/ubi-minimal:8.6";
    }
    return "quay.io/quarkus/ubi-quarkus-native-binary-s2i:1.0";
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
    final Arguments.ArgumentsBuilder ab = Arguments.builder();
    ab.execArgument("./" + findSingleFileThatEndsWith(getProject(), runnerSuffix(getQuarkusConfiguration(getProject()))));
    return ab.build();
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
    final Properties quarkusConfiguration = getQuarkusConfiguration(project);
    final AssemblyFileSet artifactFileSet = createFileSet()
      .directory(FileUtil.getRelativePath(project.getBaseDirectory(), project.getBuildDirectory()))
      .include(findSingleFileThatEndsWith(project, runnerSuffix(quarkusConfiguration)))
      .fileMode("0755")
      .build();
    return AssemblyConfiguration.builder()
      .targetDir(getTargetDir())
      .excludeFinalOutputArtifact(true)
      .layer(Assembly.builder().fileSet(artifactFileSet).build())
      .build();
  }
}
