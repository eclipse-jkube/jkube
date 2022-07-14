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
package org.eclipse.jkube.kit.config.service;

import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;

import java.util.Collection;

public abstract class AbstractImageBuildService implements BuildService {
  private final JKubeServiceHub jKubeServiceHub;

  protected AbstractImageBuildService(JKubeServiceHub jKubeServiceHub) {
    this.jKubeServiceHub = jKubeServiceHub;
  }

  protected abstract void buildSingleImage(ImageConfiguration imageConfiguration) throws JKubeServiceException;

  protected abstract void pushSingleImage(ImageConfiguration imageConfiguration, int retries, RegistryConfig registryConfig, boolean skipTag) throws JKubeServiceException;

  @Override
  public final void build(ImageConfiguration... imageConfigurations) throws JKubeServiceException {
    processImage(this::buildSingleImage, "Skipped building", imageConfigurations);
  }

  @Override
  public final void push(Collection<ImageConfiguration> imageConfigs, int retries, RegistryConfig registryConfig, boolean skipTag) throws JKubeServiceException {
    processImage(imageConfiguration -> pushSingleImage(imageConfiguration, retries, registryConfig, skipTag), "Skipped push", imageConfigs.toArray(new ImageConfiguration[0]));
  }

  @FunctionalInterface
  private interface ImageConfigurationProcessor {
    void process(ImageConfiguration imageConfiguration) throws JKubeServiceException;
  }

  private void processImage(ImageConfigurationProcessor imageConfigurationConsumer, String skipMessage, ImageConfiguration... imageConfigurations) throws JKubeServiceException {
    if (imageConfigurations != null) {
      for (ImageConfiguration imageConfiguration : imageConfigurations) {
        if (imageConfiguration.getBuildConfiguration() != null && imageConfiguration.getBuildConfiguration().getSkip()) {
          jKubeServiceHub.getLog().info("%s : %s", imageConfiguration.getDescription(), skipMessage);
        } else if (imageConfiguration.getBuildConfiguration() == null) {
          jKubeServiceHub.getLog().info("%s : %s (Image configuration has no build settings)", imageConfiguration.getDescription(), skipMessage);
        } else {
          imageConfigurationConsumer.process(imageConfiguration);
        }
      }
    }
  }
}
