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
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.springboot.SpringBootLayeredJar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.eclipse.jkube.kit.common.util.FileUtil.getRelativePath;

public class LayeredJarGenerator extends AbstractSpringBootNestedGenerator {

  private final SpringBootLayeredJar springBootLayeredJar;

  public LayeredJarGenerator(GeneratorContext generatorContext, GeneratorConfig generatorConfig, File layeredJar) {
    this(generatorContext, generatorConfig, new SpringBootLayeredJar(layeredJar, generatorContext.getLogger()));
  }

  // Package-private seam for testing: lets tests supply a SpringBootLayeredJar whose extraction
  // is stubbed, so the assembly configuration can be verified without forking a JVM per test.
  LayeredJarGenerator(GeneratorContext generatorContext, GeneratorConfig generatorConfig, SpringBootLayeredJar springBootLayeredJar) {
    super(generatorContext, generatorConfig);
    this.springBootLayeredJar = springBootLayeredJar;
  }

  @Override
  public Map<String, String> getEnv(Function<Boolean, Map<String, String>> javaExecEnvSupplier, boolean prePackagePhase) {
    final Map<String, String> res = super.getEnv(javaExecEnvSupplier, prePackagePhase);
    res.put("JAVA_MAIN_CLASS", springBootLayeredJar.getMainClass());
    return res;
  }

  @Override
  public AssemblyConfiguration createAssemblyConfiguration(List<AssemblyFileSet> defaultFileSets) {
    getLogger().info("Spring Boot layered jar detected");
    final List<Assembly> layerAssemblies = new ArrayList<>();
    layerAssemblies.add(Assembly.builder().id("jkube-includes").fileSets(defaultFileSets).build());

    File buildPackageDirectory = getProject().getBuildPackageDirectory();
    getLogger().debug("Extracting Spring Boot layers to: %s", buildPackageDirectory.getAbsolutePath());
    springBootLayeredJar.extractLayers(buildPackageDirectory);

    // With --destination . flag, layers are always extracted directly to buildPackageDirectory
    // No need to search for subdirectories - the extraction destination is controlled
    for (String springBootLayer : springBootLayeredJar.listLayers()) {
      File layerDir = new File(buildPackageDirectory, springBootLayer);

      layerAssemblies.add(Assembly.builder()
              .id(springBootLayer)
              .fileSet(AssemblyFileSet.builder()
                  .directory(getRelativePath(getProject().getBaseDirectory(), layerDir))
                  .outputDirectory(new File("."))  // Flat: all layers → /deployments
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
