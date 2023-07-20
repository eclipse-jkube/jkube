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
package org.eclipse.jkube.kit.config.service.plugins;

import org.eclipse.jkube.api.JKubePlugin;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import java.io.File;
import java.io.IOException;

public class DefaultPluginService implements PluginService {

  private final JKubeServiceHub jKubeServiceHub;

  public DefaultPluginService(JKubeServiceHub jKubeServiceHub) {
    this.jKubeServiceHub = jKubeServiceHub;
  }

  /** {@inheritDoc} */
  @Override
  public void addExtraFiles() throws JKubeServiceException {
    final File extraDirectory = new File(
      jKubeServiceHub.getConfiguration().getProject().getOutputDirectory(), JKubePlugin.JKUBE_EXTRA_DIRECTORY);
    try {
      FileUtil.createDirectory(extraDirectory);
    } catch (IOException exception) {
      throw new JKubeServiceException("Unable to create the jkube-extra directory", exception);
    }
    for (JKubePlugin plugin : jKubeServiceHub.getPluginManager().getPlugins()) {
      jKubeServiceHub.getLog().debug("Adding extra files for plugin %s", plugin.getClass().getName());
      try {
        plugin.addExtraFiles(extraDirectory);
        jKubeServiceHub.getLog().debug("Extra files for plugin %s added", plugin.getClass().getName());
      } catch (Exception ex) {
        jKubeServiceHub.getLog().debug("Problem adding extra files for plugin %s: %s",
          plugin.getClass().getName(), ex.getMessage());
      }
    }
  }
}
