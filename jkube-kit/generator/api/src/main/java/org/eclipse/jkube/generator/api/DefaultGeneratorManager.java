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
package org.eclipse.jkube.generator.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.build.api.helper.ImageConfigResolver;
import org.eclipse.jkube.kit.build.api.helper.ImageNameFormatter;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ClassUtil;
import org.eclipse.jkube.kit.common.util.PluginServiceFactory;
import org.eclipse.jkube.kit.config.image.GeneratorManager;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

/**
 * Manager responsible for finding and calling generators
 */
public class DefaultGeneratorManager implements GeneratorManager {

  private static final String[] SERVICE_PATHS = new String[] {
      "META-INF/jkube/generator-default",
      "META-INF/jkube/jkube-generator-default",
      "META-INF/jkube/generator",
      "META-INF/jkube-generator"
  };
  private final GeneratorContext genCtx;

  public DefaultGeneratorManager(GeneratorContext context) {
    this.genCtx = context;
  }

  @Override
  public List<ImageConfiguration> generateAndMerge(List<ImageConfiguration> unresolvedImages) {
    ImageConfigResolver imageConfigResolver = new ImageConfigResolver();
    final ImageNameFormatter imageNameFormatter = new ImageNameFormatter(genCtx.getProject(), genCtx.getBuildTimestamp());
    // Resolve images
    final List<ImageConfiguration> resolvedImages = resolveImages(unresolvedImages, (ImageConfiguration image) -> imageConfigResolver.resolve(image, genCtx.getProject()));

    // Init and validate Image configurations. After this step, getResolvedImages() contains the valid configuration.
    for (ImageConfiguration imageConfiguration : resolvedImages) {
      imageConfiguration.setName(imageNameFormatter.format(imageConfiguration.getName()));
      if (imageConfiguration.getBuild() != null) {
        imageConfiguration.getBuild().initAndValidate();
      }
      printDockerfileInfoIfDockerfileMode(imageConfiguration);
    }

    return resolvedImages;
  }

  private void printDockerfileInfoIfDockerfileMode(ImageConfiguration imageConfiguration) {
    BuildConfiguration buildConfiguration = imageConfiguration.getBuildConfiguration();
    if (buildConfiguration != null &&  buildConfiguration.isDockerFileMode()) {
      genCtx.getLogger().info("Using Dockerfile: %s", buildConfiguration.getDockerFile().getAbsolutePath());
      genCtx.getLogger().info("Using Docker Context Directory: %s", buildConfiguration.getAbsoluteContextDirPath(genCtx.getSourceDirectory(), genCtx.getProject().getBaseDirectory().getAbsolutePath()));
    }
  }

  private List<ImageConfiguration> resolveImages(List<ImageConfiguration> images, Function<ImageConfiguration, List<ImageConfiguration>> imageResolver) {
    List<ImageConfiguration> ret = resolveConfiguration(imageResolver, images);
    ret = generate(ret);
    final List<ImageConfiguration> filtered =  filterImages(ret);
    if (!ret.isEmpty() && filtered.isEmpty() && genCtx.getFilter() != null) {
      final List<String> imageNames = ret.stream().map(ImageConfiguration::getName).collect(Collectors.toList());
      genCtx.getLogger().warn("None of the resolved images [%s] match the configured filter '%s'",
          String.join(",", imageNames), genCtx.getFilter());
    }
    return filtered;
  }

  private static List<ImageConfiguration> resolveConfiguration(Function<ImageConfiguration, List<ImageConfiguration>> imageResolver,
                                                               List<ImageConfiguration> unresolvedImages) {
    List<ImageConfiguration> ret = new ArrayList<>();
    if (unresolvedImages != null) {
      for (ImageConfiguration image : unresolvedImages) {
        ret.addAll(imageResolver.apply(image));
      }
      verifyImageNames(ret);
    }
    return ret;
  }

  private static void verifyImageNames(List<ImageConfiguration> ret) {
    for (ImageConfiguration config : ret) {
      if (config.getName() == null) {
        throw new IllegalArgumentException("Configuration error: <image> must have a non-null <name>");
      }
    }
  }

  private List<ImageConfiguration> filterImages(List<ImageConfiguration> imagesToFilter) {
    List<ImageConfiguration> ret = new ArrayList<>();
    for (ImageConfiguration imageConfig : imagesToFilter) {
      if (matchesConfiguredImages(genCtx.getFilter(), imageConfig)) {
        ret.add(imageConfig);
      }
    }
    return ret;
  }

  private boolean matchesConfiguredImages(String imageList, ImageConfiguration imageConfig) {
    if (imageList == null) {
      return true;
    }
    Set<String> imagesAllowed = new HashSet<>(Arrays.asList(imageList.split(",")));
    return imagesAllowed.contains(imageConfig.getName()) || imagesAllowed.contains(imageConfig.getAlias());
  }

  private List<ImageConfiguration> generate(List<ImageConfiguration> imageConfigs) {
    List<ImageConfiguration> ret = imageConfigs;
    final KitLogger log = genCtx.getLogger();
    List<Generator> usableGenerators = createUsableGeneratorList();
    log.verbose("Generators:");
    for (Generator generator : usableGenerators) {
      log.verbose(" - %s", generator.getName());
      if (generator.isApplicable(ret)) {
        log.info("Running generator %s", generator.getName());
        ret = generator.customize(ret, genCtx.isPrePackagePhase());
      }
    }
    return ret;
  }

  private List<Generator> createUsableGeneratorList() {
    final PluginServiceFactory<GeneratorContext> pluginFactory = new PluginServiceFactory<>(genCtx);
    if (genCtx.isUseProjectClasspath()) {
      pluginFactory.addAdditionalClassLoader(
          ClassUtil.createProjectClassLoader(genCtx.getProject().getCompileClassPathElements(), genCtx.getLogger()));
    }
    final List<Generator> generators = pluginFactory.createServiceObjects(SERVICE_PATHS);
    return genCtx.getConfig().prepareProcessors(generators, "generator");
  }
}
