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
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.build.api.config.property.PropertyConfigResolver;
import org.eclipse.jkube.kit.build.api.helper.ImageNameFormatter;
import org.eclipse.jkube.kit.common.JKubeException;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ClassUtil;
import org.eclipse.jkube.kit.common.util.PluginServiceFactory;
import org.eclipse.jkube.kit.config.image.GeneratorManager;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import static org.eclipse.jkube.kit.build.api.helper.ImageNameFormatter.DOCKER_IMAGE_USER;

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
  private final PropertyConfigResolver propertyConfigResolver;

  public DefaultGeneratorManager(GeneratorContext context) {
    this.genCtx = context;
    propertyConfigResolver = new PropertyConfigResolver();
    addOpenShiftBuildRelatedProperties();
  }

  @Override
  public List<ImageConfiguration> generateAndMerge(List<ImageConfiguration> unresolvedImages) {
    final List<ImageConfiguration> resolvedImages = resolveImages(unresolvedImages);
    final List<ImageConfiguration> generatedImages = generateImages(resolvedImages);
    final List<ImageConfiguration> filteredImages = filterImages(generatedImages);
    // Init and validate Image configurations. These images will contain the valid configurations.
    final ImageNameFormatter imageNameFormatter = new ImageNameFormatter(genCtx.getProject(), genCtx.getBuildTimestamp());
    for (ImageConfiguration imageConfiguration : filteredImages) {
      imageConfiguration.setName(imageNameFormatter.format(imageConfiguration.getName()));
      if (imageConfiguration.getBuild() != null) {
        BuildConfiguration updatedBuildConfig = mergeGlobalConfigParamsWithSingleImageBuildConfig(imageConfiguration.getBuild());
        imageConfiguration.setBuild(updatedBuildConfig);
        imageConfiguration.getBuild().initAndValidate();
      }
      final BuildConfiguration buildConfiguration = imageConfiguration.getBuildConfiguration();
      if (buildConfiguration != null && buildConfiguration.isDockerFileMode()) {
        genCtx.getLogger().info("Using Dockerfile: %s", buildConfiguration.getDockerFile().getAbsolutePath());
        genCtx.getLogger().info("Using Docker Context Directory: %s", buildConfiguration.getAbsoluteContextDirPath(genCtx.getSourceDirectory(), genCtx.getProject().getBaseDirectory().getAbsolutePath()));
      }
    }
    return filteredImages;
  }

  private List<ImageConfiguration> resolveImages(List<ImageConfiguration> unresolvedImages) {
    final List<ImageConfiguration> resolvedImages = new ArrayList<>();
    if (unresolvedImages != null) {
      for (ImageConfiguration unresolvedImage : unresolvedImages) {
        final ImageConfiguration resolvedImage = propertyConfigResolver.resolve(unresolvedImage, genCtx.getProject());
        if (resolvedImage.getName() == null) {
          throw new JKubeException("Configuration error: <image> must have a non-null <name>");
        }
        resolvedImages.add(resolvedImage);
      }
    }
    return resolvedImages;
  }

  private List<ImageConfiguration> generateImages(List<ImageConfiguration> imageConfigs) {
    List<ImageConfiguration> ret = imageConfigs;
    final KitLogger log = genCtx.getLogger();
    final List<Generator> usableGenerators = createUsableGeneratorList();
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

  private List<ImageConfiguration> filterImages(List<ImageConfiguration> imagesToFilter) {
    final List<ImageConfiguration> filteredImages = new ArrayList<>();
    for (ImageConfiguration imageConfig : imagesToFilter) {
      if (matchesConfiguredImages(genCtx.getFilter(), imageConfig)) {
        filteredImages.add(imageConfig);
      }
    }
    if (!imagesToFilter.isEmpty() && filteredImages.isEmpty() && genCtx.getFilter() != null) {
      final List<String> imageNames = imagesToFilter.stream().map(ImageConfiguration::getName).collect(Collectors.toList());
      genCtx.getLogger().warn("None of the resolved images [%s] match the configured filter '%s'",
        String.join(",", imageNames), genCtx.getFilter());
    }
    return filteredImages;
  }

  // TODO: Should be moved to a more suitable place (Probably within the JavaProject class)
  private void addOpenShiftBuildRelatedProperties() {
    if (genCtx.getRuntimeMode() == RuntimeMode.OPENSHIFT) {
      final Properties properties = genCtx.getProject().getProperties();
      final String namespaceToBeUsed = genCtx.getOpenshiftNamespace();
      if (!properties.contains(DOCKER_IMAGE_USER) && StringUtils.isNotBlank(namespaceToBeUsed)) {
        genCtx.getLogger().info("Using container image name of namespace: " + namespaceToBeUsed);
        properties.setProperty(DOCKER_IMAGE_USER, namespaceToBeUsed);
      }
      if (!properties.contains(RuntimeMode.JKUBE_EFFECTIVE_PLATFORM_MODE)) {
        properties.setProperty(RuntimeMode.JKUBE_EFFECTIVE_PLATFORM_MODE, genCtx.getRuntimeMode().toString());
      }
    }
  }

  private boolean matchesConfiguredImages(String imageList, ImageConfiguration imageConfig) {
    if (imageList == null) {
      return true;
    }
    Set<String> imagesAllowed = new HashSet<>(Arrays.asList(imageList.split(",")));
    return imagesAllowed.contains(imageConfig.getName()) || imagesAllowed.contains(imageConfig.getAlias());
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

  private BuildConfiguration mergeGlobalConfigParamsWithSingleImageBuildConfig(BuildConfiguration build) {
    BuildConfiguration.BuildConfigurationBuilder buildConfigBuilder = build.toBuilder();
    if (!build.isOpenshiftForcePull() && genCtx.isOpenshiftForcePull()) {
      buildConfigBuilder.openshiftForcePull(true);
    }
    if (StringUtils.isBlank(build.getOpenshiftS2iBuildNameSuffix()) &&
        StringUtils.isNotBlank(genCtx.getOpenshiftS2iBuildNameSuffix())) {
      buildConfigBuilder.openshiftS2iBuildNameSuffix(genCtx.getOpenshiftS2iBuildNameSuffix());
    }
    if (!build.isOpenshiftS2iImageStreamLookupPolicyLocal() && genCtx.isOpenshiftS2iImageStreamLookupPolicyLocal()) {
      buildConfigBuilder.openshiftS2iImageStreamLookupPolicyLocal(true);
    }
    if (StringUtils.isBlank(build.getOpenshiftPullSecret()) && StringUtils.isNotBlank(genCtx.getOpenshiftPullSecret())) {
      buildConfigBuilder.openshiftPullSecret(genCtx.getOpenshiftPullSecret());
    }
    if (StringUtils.isBlank(build.getOpenshiftPushSecret()) && StringUtils.isNotBlank(genCtx.getOpenshiftPushSecret())) {
      buildConfigBuilder.openshiftPushSecret(genCtx.getOpenshiftPushSecret());
    }
    if (StringUtils.isBlank(build.getOpenshiftBuildOutputKind()) && StringUtils.isNotBlank(genCtx.getOpenshiftBuildOutputKind())) {
      buildConfigBuilder.openshiftBuildOutputKind(genCtx.getOpenshiftBuildOutputKind());
    }
    if (build.getOpenshiftBuildRecreateMode() == null) {
      buildConfigBuilder.openshiftBuildRecreateMode(genCtx.getOpenshiftBuildRecreate());
    }
    return buildConfigBuilder.build();
  }
}
