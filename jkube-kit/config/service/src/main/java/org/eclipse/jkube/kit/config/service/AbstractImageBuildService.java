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

import org.eclipse.jkube.kit.config.image.ImageConfiguration;

import java.util.Collection;

public abstract class AbstractImageBuildService implements BuildService {
  private final JKubeServiceHub jKubeServiceHub;

  protected AbstractImageBuildService(JKubeServiceHub jKubeServiceHub) {
    this.jKubeServiceHub = jKubeServiceHub;
  }

  @Override
  public void build(Collection<ImageConfiguration> imageConfigurations) throws JKubeServiceException {
    if (imageConfigurations != null) {
      for (ImageConfiguration imageConfig : imageConfigurations) {
        if (imageConfig.getBuildConfiguration() != null && imageConfig.getBuildConfiguration().getSkip()) {
          jKubeServiceHub.getLog().info("%s : Skipped building", imageConfig.getDescription());
        } else {
          build(imageConfig);
        }
      }
    }
  }
}
