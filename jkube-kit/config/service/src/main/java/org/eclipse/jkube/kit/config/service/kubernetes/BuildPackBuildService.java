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
package org.eclipse.jkube.kit.config.service.kubernetes;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

import org.eclipse.jkube.kit.build.service.docker.DockerServiceHub;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.service.AbstractImageBuildService;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.service.buildpacks.BuildPackBuildOptions;
import org.eclipse.jkube.kit.service.buildpacks.BuildPackCliDownloader;
import org.eclipse.jkube.kit.service.buildpacks.controller.BuildPackCliController;

import static org.apache.commons.lang3.StringUtils.strip;
import static org.eclipse.jkube.kit.common.util.EnvUtil.getUserHome;
import static org.eclipse.jkube.kit.common.util.PropertiesUtil.readProperties;

public class BuildPackBuildService extends AbstractImageBuildService {
  private static final String DEFAULT_BUILDER_IMAGE = "paketobuildpacks/builder:base";
  private static final String PACK_CONFIG_DIR = ".pack";
  private static final String PACK_CONFIG_FILE = "config.toml";

  private final BuildServiceConfig buildServiceConfig;
  private final KitLogger kitLogger;
  private final BuildPackCliDownloader buildPackCliDownloader;
  private final DockerServiceHub dockerServiceHub;

  public BuildPackBuildService(JKubeServiceHub jKubeServiceHub) {
    this(jKubeServiceHub, null);
  }

  BuildPackBuildService(JKubeServiceHub jKubeServiceHub, Properties packProperties) {
    super(jKubeServiceHub);
    this.buildServiceConfig = Objects.requireNonNull(jKubeServiceHub.getBuildServiceConfig(),
        "BuildServiceConfig is required");
    this.kitLogger = Objects.requireNonNull(jKubeServiceHub.getLog());
    if (packProperties == null) {
      this.buildPackCliDownloader = new BuildPackCliDownloader(kitLogger);
    } else {
      this.buildPackCliDownloader = new BuildPackCliDownloader(kitLogger, packProperties);
    }
    this.dockerServiceHub = jKubeServiceHub.getDockerServiceHub();
  }

  @Override
  protected void buildSingleImage(ImageConfiguration imageConfiguration) {
    kitLogger.info("Delegating container image building process to BuildPacks");
    final File packCli = buildPackCliDownloader.getPackCLIIfPresentOrDownload();
    kitLogger.info("Using pack %s", packCli.getAbsolutePath());
    final String builderImage;
    final Properties localPackConfig =
      readProperties(getUserHome().toPath().resolve(PACK_CONFIG_DIR).resolve(PACK_CONFIG_FILE));
    if (localPackConfig.get("default-builder-image") != null) {
      builderImage = strip(localPackConfig.getProperty("default-builder-image"), "\"");
    } else {
      builderImage = DEFAULT_BUILDER_IMAGE;
    }
    new BuildPackCliController(packCli, kitLogger)
      .build(BuildPackBuildOptions.builder()
        .imageName(imageConfiguration.getName())
        .builderImage(builderImage)
        .creationTime("now")
        .build());
  }

  @Override
  protected void pushSingleImage(ImageConfiguration imageConfiguration, int retries, RegistryConfig registryConfig, boolean skipTag) throws JKubeServiceException {
    try {
      dockerServiceHub.getRegistryService().pushImage(imageConfiguration, retries, registryConfig, skipTag);
    } catch (IOException ex) {
      throw new JKubeServiceException("Error while trying to push the image: " + ex.getMessage(), ex);
    }
  }

  @Override
  public boolean isApplicable() {
    return buildServiceConfig.getJKubeBuildStrategy() != null &&
        buildServiceConfig.getJKubeBuildStrategy().equals(JKubeBuildStrategy.buildpacks);
  }

  @Override
  public void postProcess() {
    // NOOP
  }
}
