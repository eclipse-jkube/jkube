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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.GeneratorManager;
import org.eclipse.jkube.gradle.plugin.GradleLogger;
import org.eclipse.jkube.gradle.plugin.GradleUtil;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import org.eclipse.jkube.kit.enricher.api.DefaultEnricherManager;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.profile.ProfileUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import static org.eclipse.jkube.kit.build.service.docker.helper.ConfigHelper.initImageConfiguration;
import static org.eclipse.jkube.kit.common.util.BuildReferenceDateUtil.getBuildTimestamp;

public abstract class AbstractJKubeTask extends DefaultTask implements JKubeTask {

  private static final String DEFAULT_LOG_PREFIX = "k8s: ";
  private static final Path DEFAULT_RESOURCE_SOURCE_DIR = Paths.get("src", "main", "jkube");

  protected final KubernetesExtension kubernetesExtension;
  protected JavaProject javaProject;
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
    javaProject = GradleUtil.convertGradleProject(getProject());
    kitLogger = new GradleLogger(getLogger(), getLogPrefix());
    clusterAccess = new ClusterAccess(kitLogger, initClusterConfiguration());
    jKubeServiceHub = initJKubeServiceHubBuilder().build();
    ImageConfigResolver imageConfigResolver = new ImageConfigResolver();
    try {
      resolvedImages = initImageConfiguration(
          kubernetesExtension.getApiVersion().getOrNull(),
          getBuildTimestamp(null, null, javaProject.getBuildDirectory().getAbsolutePath(), DOCKER_BUILD_TIMESTAMP),
          javaProject, kubernetesExtension.images, imageConfigResolver, kitLogger,
          kubernetesExtension.getFilter().getOrNull(),
          this::customizeConfig);
      enricherManager = new DefaultEnricherManager(JKubeEnricherContext.builder()
        .project(javaProject)
          .processorConfig(ProfileUtil.blendProfileWithConfiguration(ProfileUtil.ENRICHER_CONFIG,
              kubernetesExtension.getProfile().getOrNull(),
              resolveResourceSourceDirectory(),
              kubernetesExtension.enricher))
        .images(resolvedImages)
        .resources(kubernetesExtension.resources)
        .log(kitLogger)
        .build());
    } catch (IOException exception) {
      kitLogger.error("Error in fetching Build timestamps: " + exception.getMessage());
    }
    run();
  }

  @Internal
  protected String getLogPrefix() {
    return DEFAULT_LOG_PREFIX;
  }

  public List<ImageConfiguration> customizeConfig(List<ImageConfiguration> configs) {
    kitLogger.info("Running in %s mode", kubernetesExtension.getRuntimeMode().getLabel());
    if (kubernetesExtension.getRuntimeMode() != RuntimeMode.OPENSHIFT) {
      kitLogger.info("Building Docker image in Kubernetes mode");
    }
    // TODO: Run Generators
    return GeneratorManager.generate(configs, generatorContextBuilder().build(), false);
  }

  public KitLogger createLogger(String prefix) {
    return new GradleLogger(getLogger(), getLogPrefix() + Optional.ofNullable(prefix).map(" "::concat).orElse(""));
  }

  protected JKubeServiceHub.JKubeServiceHubBuilder initJKubeServiceHubBuilder() {
    return JKubeServiceHub.builder()
        .log(kitLogger)
        .configuration(JKubeConfiguration.builder()
            .project(javaProject)
            .reactorProjects(Collections.singletonList(javaProject))
            .sourceDirectory(kubernetesExtension.getBuildSourceDirectory().getOrElse("src/main/docker"))
            .outputDirectory(kubernetesExtension.getBuildOutputDirectory().getOrElse("build/docker"))
            .registryConfig(RegistryConfig.builder()
                .settings(Collections.emptyList())
                .authConfig(kubernetesExtension.authConfig != null ? kubernetesExtension.authConfig.toMap() : null)
                .skipExtendedAuth(kubernetesExtension.getSkipExtendedAuth().getOrElse(false))
                .passwordDecryptionMethod(s -> s)
                .registry(
                    kubernetesExtension.getPullRegistry().getOrElse(kubernetesExtension.getRegistry().getOrElse("docker.io")))
                .build())
            .build())
        .clusterAccess(clusterAccess)
        .offline(kubernetesExtension.getOfflineOrDefault())
        .platformMode(kubernetesExtension.getRuntimeMode());
  }

  protected ClusterConfiguration initClusterConfiguration() {
    return ClusterConfiguration.from(kubernetesExtension.access,
        System.getProperties(), javaProject.getProperties()).build();
  }

  protected final File resolveResourceSourceDirectory() {
    return ResourceUtil.getFinalResourceDir(kubernetesExtension.getResourceSourceDirectory()
        .getOrElse(javaProject.getBaseDirectory().toPath().resolve(DEFAULT_RESOURCE_SOURCE_DIR).toFile()),
        kubernetesExtension.getResourceEnvironment().getOrNull());
  }

  protected GeneratorContext.GeneratorContextBuilder generatorContextBuilder() {
    return GeneratorContext.builder()
      .config(extractGeneratorConfig())
      .project(javaProject)
      .logger(kitLogger)
      .runtimeMode(kubernetesExtension.getRuntimeMode())
      .useProjectClasspath(kubernetesExtension.getUseProjectClassPath().getOrElse(false))
      .artifactResolver(jKubeServiceHub.getArtifactResolverService());
  }

  protected ProcessorConfig extractGeneratorConfig() {
    try {
      return ProfileUtil.blendProfileWithConfiguration(ProfileUtil.GENERATOR_CONFIG, kubernetesExtension.getProfile().getOrNull(), kubernetesExtension.getResourceTargetDirectory().getOrNull(), kubernetesExtension.generator);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot extract generator config: " + e, e);
    }
  }
}
