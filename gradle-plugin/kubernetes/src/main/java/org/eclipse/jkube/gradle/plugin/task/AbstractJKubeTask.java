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
package org.eclipse.jkube.gradle.plugin.task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.GeneratorManager;
import org.eclipse.jkube.gradle.plugin.GradleLogger;
import org.eclipse.jkube.gradle.plugin.GradleUtil;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.util.LazyBuilder;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.enricher.api.DefaultEnricherManager;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.profile.ProfileUtil;

import org.eclipse.jkube.kit.resource.service.DefaultResourceService;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.configuration.ConsoleOutput;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import static org.eclipse.jkube.kit.build.service.docker.helper.ConfigHelper.initImageConfiguration;
import static org.eclipse.jkube.kit.common.JKubeFileInterpolator.interpolate;
import static org.eclipse.jkube.kit.common.util.BuildReferenceDateUtil.getBuildTimestamp;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.updateResourceConfigNamespace;

public abstract class AbstractJKubeTask extends DefaultTask implements KubernetesJKubeTask {

  protected final KubernetesExtension kubernetesExtension;
  protected KitLogger kitLogger;
  protected ClusterAccess clusterAccess;
  protected JKubeServiceHub jKubeServiceHub;
  protected static final String DOCKER_BUILD_TIMESTAMP = "docker/build.timestamp";
  protected List<ImageConfiguration> resolvedImages;
  protected DefaultEnricherManager enricherManager;

  protected AbstractJKubeTask(Class<? extends KubernetesExtension> extensionClass) {
    kubernetesExtension = getProject().getExtensions().getByType(extensionClass);
  }

  @TaskAction
  public final void runTask() {
    init();
    if (shouldSkip()) {
        kitLogger.info("`%s` task is skipped.", this.getName());
        return;
    }
    run();
  }

  private void init() {
    kubernetesExtension.javaProject = GradleUtil.convertGradleProject(getProject());
    kitLogger = createLogger(null);
    clusterAccess = new ClusterAccess(initClusterConfiguration());
    jKubeServiceHub = initJKubeServiceHubBuilder().build();
    kubernetesExtension.resources = updateResourceConfigNamespace(kubernetesExtension.getNamespaceOrNull(), kubernetesExtension.resources);
    ImageConfigResolver imageConfigResolver = new ImageConfigResolver();
    try {
      resolvedImages = resolveImages(imageConfigResolver);
      final JKubeEnricherContext context = JKubeEnricherContext.builder()
          .project(kubernetesExtension.javaProject)
          .processorConfig(ProfileUtil.blendProfileWithConfiguration(ProfileUtil.ENRICHER_CONFIG,
              kubernetesExtension.getProfileOrNull(),
              resolveResourceSourceDirectory(),
              kubernetesExtension.enricher))
          .images(resolvedImages)
          .resources(kubernetesExtension.resources)
          .log(kitLogger)
          .jKubeBuildStrategy(kubernetesExtension.getBuildStrategyOrDefault())
          .build();
      final List<String> extraClasspathElements = kubernetesExtension.getUseProjectClassPathOrDefault() ?
          kubernetesExtension.javaProject.getCompileClassPathElements() : Collections.emptyList();
      enricherManager = new DefaultEnricherManager(context, extraClasspathElements);
    } catch (IOException exception) {
      kitLogger.error("Error in fetching Build timestamps: " + exception.getMessage());
    }
  }

  protected boolean shouldSkip() {
    return kubernetesExtension.getSkipOrDefault();
  }

  @Internal
  @Override
  public KubernetesExtension getExtension() {
    return kubernetesExtension;
  }

  private List<ImageConfiguration> customizeConfig(List<ImageConfiguration> configs) {
    return GeneratorManager.generate(configs, initGeneratorContextBuilder().build(), false);
  }

  private boolean isAnsiEnabled() {
    return kubernetesExtension.getUseColorOrDefault()
        && getProject().getGradle().getStartParameter().getConsoleOutput() != ConsoleOutput.Plain;
  }

  protected final KitLogger createLogger(String prefix) {
    return new GradleLogger(getLogger(), isAnsiEnabled(), getLogPrefix() + Optional.ofNullable(prefix).map(" "::concat).orElse(""));
  }

  protected JKubeServiceHub.JKubeServiceHubBuilder initJKubeServiceHubBuilder() {
    return JKubeServiceHub.builder()
        .log(kitLogger)
        .configuration(JKubeConfiguration.builder()
            .project(kubernetesExtension.javaProject)
            .reactorProjects(Collections.singletonList(kubernetesExtension.javaProject))
            .sourceDirectory(kubernetesExtension.getBuildSourceDirectoryOrDefault())
            .outputDirectory(kubernetesExtension.getBuildOutputDirectoryOrDefault())
            .registryConfig(RegistryConfig.builder()
                .settings(Collections.emptyList())
                .authConfig(kubernetesExtension.authConfig != null ? kubernetesExtension.authConfig.toMap() : null)
                .skipExtendedAuth(kubernetesExtension.getSkipExtendedAuth().getOrElse(false))
                .passwordDecryptionMethod(s -> s)
                .registry(kubernetesExtension.getPullRegistryOrDefault())
                .build())
            .build())
        .clusterAccess(clusterAccess)
        .offline(kubernetesExtension.getOfflineOrDefault())
        .platformMode(kubernetesExtension.getRuntimeMode())
        .resourceServiceConfig(initResourceServiceConfig())
        .resourceService(new LazyBuilder<>(hub -> new DefaultResourceService(hub.getResourceServiceConfig())));
  }

  private ResourceServiceConfig initResourceServiceConfig() {
    ResourceConfig resourceConfig = kubernetesExtension.resources;
    if (kubernetesExtension.getNamespaceOrNull() != null) {
      resourceConfig = ResourceConfig.toBuilder(resourceConfig).namespace(kubernetesExtension.getNamespaceOrNull()).build();
    }
    return ResourceServiceConfig.builder()
      .project(kubernetesExtension.javaProject)
      .resourceDirs(resolveResourceSourceDirectory())
      .targetDir(kubernetesExtension.getResourceTargetDirectoryOrDefault())
      .resourceFileType(kubernetesExtension.getResourceFileTypeOrDefault())
      .resourceConfig(resourceConfig)
      .interpolateTemplateParameters(kubernetesExtension.getInterpolateTemplateParametersOrDefault())
      .resourceFilesProcessor(this::gradleFilterFiles)
      .build();
  }

  protected GeneratorContext.GeneratorContextBuilder initGeneratorContextBuilder() {
    return GeneratorContext.builder()
        .config(extractGeneratorConfig())
        .project(kubernetesExtension.javaProject)
        .logger(kitLogger)
        .runtimeMode(kubernetesExtension.getRuntimeMode())
        .strategy(kubernetesExtension.getBuildStrategyOrDefault())
        .useProjectClasspath(kubernetesExtension.getUseProjectClassPathOrDefault());
  }

  protected ClusterConfiguration initClusterConfiguration() {
    return ClusterConfiguration.from(kubernetesExtension.access,
        System.getProperties(), kubernetesExtension.javaProject.getProperties()).build();
  }

  protected final List<File> resolveResourceSourceDirectory() {
    return ResourceUtil.getFinalResourceDirs(kubernetesExtension.getResourceSourceDirectoryOrDefault(),
        kubernetesExtension.getResourceEnvironmentOrNull());
  }


  protected ProcessorConfig extractGeneratorConfig() {
    try {
      return ProfileUtil.blendProfileWithConfiguration(ProfileUtil.GENERATOR_CONFIG, kubernetesExtension.getProfileOrNull(), ResourceUtil.getFinalResourceDirs(kubernetesExtension.getResourceSourceDirectoryOrDefault(), kubernetesExtension.getResourceEnvironmentOrNull()), kubernetesExtension.generator);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot extract generator config: " + e, e);
    }
  }

  protected List<ImageConfiguration> resolveImages(ImageConfigResolver imageConfigResolver) throws IOException {
    return initImageConfiguration(
        getBuildTimestamp(null, null, kubernetesExtension.javaProject.getBuildDirectory().getAbsolutePath(),
            DOCKER_BUILD_TIMESTAMP),
        kubernetesExtension.images, imageConfigResolver, kitLogger,
      kubernetesExtension.getFilter().getOrNull(),
      this::customizeConfig,
      jKubeServiceHub.getConfiguration());
  }

  protected File getManifest(KubernetesClient kc) {
    final File manifest = kubernetesExtension.getManifest(kitLogger, kc);
    if (!manifest.exists() || !manifest.isFile()) {
      if (kubernetesExtension.getFailOnNoKubernetesJsonOrDefault()) {
        throw new IllegalStateException("No such generated manifest file: " + manifest);
      } else {
        kitLogger.warn("No such generated manifest file %s for this project so ignoring", manifest);
      }
    }
    return manifest;
  }

  private File[] gradleFilterFiles(File[] resourceFiles) throws IOException {
    if (resourceFiles == null) {
      return new File[0];
    }
    final File outDir = kubernetesExtension.getWorkDirectoryOrDefault();
    if (!outDir.exists() && !outDir.mkdirs()) {
      throw new IOException("Cannot create working dir " + outDir);
    }
    File[] ret = new File[resourceFiles.length];
    int i = 0;
    for (File resource : resourceFiles) {
      File targetFile = new File(outDir, resource.getName());
      String resourceFragmentInterpolated = interpolate(resource, kubernetesExtension.javaProject.getProperties(),
        kubernetesExtension.getFilter().getOrNull());
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
        writer.write(resourceFragmentInterpolated);
      }
      ret[i++] = targetFile;
    }
    return ret;
  }
}
