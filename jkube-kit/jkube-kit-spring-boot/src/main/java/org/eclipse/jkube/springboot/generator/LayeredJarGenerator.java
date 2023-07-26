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
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.eclipse.jkube.kit.common.util.FileUtil.getRelativePath;
import static org.eclipse.jkube.springboot.SpringBootLayeredJarExecUtils.extractLayers;
import static org.eclipse.jkube.springboot.SpringBootLayeredJarExecUtils.listLayers;

public class LayeredJarGenerator extends AbstractSpringBootNestedGenerator {
  private final FatJarDetector.Result fatJarDetectorResult;
  public LayeredJarGenerator(GeneratorContext generatorContext, GeneratorConfig generatorConfig, FatJarDetector.Result result) {
    super(generatorContext, generatorConfig);
    fatJarDetectorResult = result;
  }

  @Override
  public Arguments getBuildEntryPoint() {
    return Arguments.builder()
        .exec(Arrays.asList("java", "org.springframework.boot.loader.JarLauncher"))
        .build();
  }

  @Override
  public Map<String, String> getEnv() {
    return Collections.singletonMap("JAVA_MAIN_CLASS", "org.springframework.boot.loader.JarLauncher");
  }

  @Override
  public AssemblyConfiguration createAssemblyConfiguration(List<AssemblyFileSet> defaultFileSets) {
    getLogger().info("Spring Boot layered jar detected");

    List<String> layerNames = listLayers(getLogger(), fatJarDetectorResult.getArchiveFile());
    List<Assembly> layerAssemblies = new ArrayList<>();
    layerAssemblies.add(Assembly.builder().id("jkube-includes").fileSets(defaultFileSets).build());
    extractLayers(getLogger(), getProject().getBuildPackageDirectory(), fatJarDetectorResult.getArchiveFile());

    for (String springBootLayer : layerNames) {
      File layerDir = new File(getProject().getBuildPackageDirectory(), springBootLayer);
      layerAssemblies.add(Assembly.builder()
              .id(springBootLayer)
              .fileSet(AssemblyFileSet.builder()
                  .outputDirectory(new File("."))
                  .directory(getRelativePath(getProject().getBaseDirectory(), layerDir))
                  .exclude("*")
                  .fileMode("0640")
                  .build())
          .build());
    }

    return AssemblyConfiguration.builder()
        .targetDir(getTargetDir())
        .excludeFinalOutputArtifact(true)
        .layers(layerAssemblies)
        .build();
  }
}
