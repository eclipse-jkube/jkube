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
package org.eclipse.jkube.generator.api;

import java.util.List;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.service.SummaryService;
import org.eclipse.jkube.kit.common.util.ClassUtil;
import org.eclipse.jkube.kit.common.util.PluginServiceFactory;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;

/**
 * Manager responsible for finding and calling generators
 */
public class GeneratorManager {

  private static final String[] SERVICE_PATHS = new String[] {
      "META-INF/jkube/generator-default",
      "META-INF/jkube/jkube-generator-default",
      "META-INF/jkube/generator",
      "META-INF/jkube-generator"
  };

  private GeneratorManager() {
  }

  public static List<ImageConfiguration> generate(List<ImageConfiguration> imageConfigs,
                                                  GeneratorContext genCtx, boolean prePackagePhase, SummaryService summaryService) {

    final PluginServiceFactory<GeneratorContext> pluginFactory = new PluginServiceFactory<>(genCtx);
    if (genCtx.isUseProjectClasspath()) {
      pluginFactory.addAdditionalClassLoader(
          ClassUtil.createProjectClassLoader(genCtx.getProject().getCompileClassPathElements(), genCtx.getLogger()));
    }

    List<ImageConfiguration> ret = imageConfigs;
    final KitLogger log = genCtx.getLogger();
    final List<Generator> generators = pluginFactory.createServiceObjects(SERVICE_PATHS);
    final List<Generator> usableGenerators = genCtx.getConfig().prepareProcessors(generators, "generator");
    log.verbose("Generators:");
    for (Generator generator : usableGenerators) {
      log.verbose(" - %s", generator.getName());
      if (generator.isApplicable(ret)) {
        log.info("Running generator %s", generator.getName());
        summaryService.addToGenerators(generator.getName());
        ret = generator.customize(ret, prePackagePhase);
      }
    }
    return ret;
  }
}
