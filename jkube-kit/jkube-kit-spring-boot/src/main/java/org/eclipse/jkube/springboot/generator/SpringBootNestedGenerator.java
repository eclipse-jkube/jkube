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

import org.eclipse.jkube.generator.api.GeneratorConfig;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.javaexec.FatJarDetector;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.springboot.SpringBootLayeredJar;

import java.util.List;
import java.util.Map;

import static org.eclipse.jkube.generator.javaexec.JavaExecGenerator.JOLOKIA_PORT_DEFAULT;
import static org.eclipse.jkube.generator.javaexec.JavaExecGenerator.PROMETHEUS_PORT_DEFAULT;
import java.io.File;
import java.util.function.Function;

import static org.eclipse.jkube.kit.common.util.SpringBootUtil.findNativeArtifactFile;
import static org.eclipse.jkube.kit.common.util.SpringBootUtil.getNativePlugin;

public interface SpringBootNestedGenerator {
  JavaProject getProject();

  default AssemblyConfiguration createAssemblyConfiguration(List<AssemblyFileSet> defaultFileSets) {
    return null;
  }

  default String getFrom() {
    return null;
  }

  default String getDefaultJolokiaPort() {
    return JOLOKIA_PORT_DEFAULT;
  }

  default String getDefaultPrometheusPort() {
    return PROMETHEUS_PORT_DEFAULT;
  }

  default Arguments getBuildEntryPoint() {
    return null;
  }

  String getBuildWorkdir();

  String getTargetDir();

  Map<String, String> getEnv(Function<Boolean, Map<String, String>> javaExecEnvSupplier, boolean prePackagePhase);

  static SpringBootNestedGenerator from(GeneratorContext generatorContext, GeneratorConfig generatorConfig, FatJarDetector.Result fatJarDetectorResult) {
    if (getNativePlugin(generatorContext.getProject()) != null) {
      File nativeBinary = findNativeArtifactFile(generatorContext.getProject());
      if (nativeBinary != null) {
        return new NativeGenerator(generatorContext, generatorConfig, nativeBinary);
      }
    }
    if (fatJarDetectorResult != null && fatJarDetectorResult.getArchiveFile() != null &&
        new SpringBootLayeredJar(fatJarDetectorResult.getArchiveFile(), generatorContext.getLogger()).isLayeredJar()) {
      return new LayeredJarGenerator(generatorContext, generatorConfig, fatJarDetectorResult.getArchiveFile());
    }
    return new FatJarGenerator(generatorContext, generatorConfig);
  }
}
