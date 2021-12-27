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
package org.eclipse.jkube.gradle.plugin.task;

import java.io.File;
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
import org.eclipse.jkube.kit.build.service.docker.access.log.LogOutputSpecFactory;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.enricher.api.DefaultEnricherManager;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.profile.ProfileUtil;

import org.gradle.api.DefaultTask;
import org.gradle.api.logging.configuration.ConsoleOutput;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import static org.eclipse.jkube.kit.build.service.docker.helper.ConfigHelper.initImageConfiguration;
import static org.eclipse.jkube.kit.common.util.BuildReferenceDateUtil.getBuildTimestamp;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.updateResourceConfigNamespace;

public abstract class AbstractJKubeTask extends DefaultTask implements KubernetesJKubeTask {

  protected final KubernetesExtension kubernetesExtension;
  protected KitLogger kitLogger;
  protected LogOutputSpecFactory logOutputSpecFactory;
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
    if (!canExecute()) {
        kitLogger.info("`%s` task is skipped.", this.getName());
        return;
    }
    run();
  }

  private void init() {
    kubernetesExtension.javaProject = GradleUtil.convertGradleProject(getProject());
    kitLogger = createLogger(null);
    logOutputSpecFactory = new LogOutputSpecFactory(isAnsiEnabled(), kubernetesExtension.getLogStdoutOrDefault(),
        kubernetesExtension.getLogDateOrNull());
    clusterAccess = new ClusterAccess(kitLogger, initClusterConfiguration());
    jKubeServiceHub = initJKubeServiceHubBuilder().build();
    kubernetesExtension.resources = updateResourceConfigNamespace(kubernetesExtension.getNamespaceOrNull(), kubernetesExtension.resources);
    ImageConfigResolver imageConfigResolver = new ImageConfigResolver();
    try {
      resolvedImages = resolveImages(imageConfigResolver);

      enricherManager = new DefaultEnricherManager(JKubeEnricherContext.builder()
          .project(kubernetesExtension.javaProject)
          .processorConfig(ProfileUtil.blendProfileWithConfiguration(ProfileUtil.ENRICHER_CONFIG,
              kubernetesExtension.getProfileOrNull(),
              resolveResourceSourceDirectory(),
              kubernetesExtension.enricher))
        .images(resolvedImages)
        .resources(kubernetesExtension.resources)
        .log(kitLogger)
        .build());
    } catch (IOException exception) {
      kitLogger.error("Error in fetching Build timestamps: " + exception.getMessage());
    }
  }

  protected boolean canExecute() {
    return !kubernetesExtension.getSkipOrDefault();
  }

  @Internal
  @Override
  public KubernetesExtension getExtension() {
    return kubernetesExtension;
  }

  private List<ImageConfiguration> customizeConfig(List<ImageConfiguration> configs) {
    kitLogger.info("Running in [[B]]%s[[B]] mode", kubernetesExtension.getRuntimeMode().getLabel());
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
        .platformMode(kubernetesExtension.getRuntimeMode());
  }

  protected GeneratorContext.GeneratorContextBuilder initGeneratorContextBuilder() {
    return GeneratorContext.builder()
        .config(extractGeneratorConfig())
        .project(kubernetesExtension.javaProject)
        .logger(kitLogger)
        .runtimeMode(kubernetesExtension.getRuntimeMode())
        .useProjectClasspath(kubernetesExtension.getUseProjectClassPathOrDefault());
  }

  protected ClusterConfiguration initClusterConfiguration() {
    return ClusterConfiguration.from(kubernetesExtension.access,
        System.getProperties(), kubernetesExtension.javaProject.getProperties()).build();
  }

  protected final File resolveResourceSourceDirectory() {
    return ResourceUtil.getFinalResourceDir(kubernetesExtension.getResourceSourceDirectoryOrDefault(),
        kubernetesExtension.getResourceEnvironmentOrNull());
  }


  protected ProcessorConfig extractGeneratorConfig() {
    try {
      return ProfileUtil.blendProfileWithConfiguration(ProfileUtil.GENERATOR_CONFIG, kubernetesExtension.getProfileOrNull(), kubernetesExtension.getResourceTargetDirectoryOrDefault(), kubernetesExtension.generator);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot extract generator config: " + e, e);
    }
  }

  protected List<ImageConfiguration> resolveImages(ImageConfigResolver imageConfigResolver) throws IOException {
    return initImageConfiguration(
        kubernetesExtension.getApiVersionOrNull(),
        getBuildTimestamp(null, null, kubernetesExtension.javaProject.getBuildDirectory().getAbsolutePath(),
            DOCKER_BUILD_TIMESTAMP),
        kubernetesExtension.javaProject, kubernetesExtension.images, imageConfigResolver, kitLogger,
      kubernetesExtension.getFilter().getOrNull(),
      this::customizeConfig);
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

}
