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
package org.eclipse.jkube.kit.config.service.plugins;

import org.eclipse.jkube.api.JKubePlugin;
import org.eclipse.jkube.kit.common.util.PluginServiceFactory;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import java.util.List;

public class PluginManager {

  private static final String PLUGIN_SERVICE_PATH = "META-INF/jkube/plugin-service";
  private static final String[] PLUGIN_PATHS = new String[] {
    "META-INF/maven/io.fabric8/dmp-plugin", // legacy-compat
    "META-INF/jkube/plugin",
    "META-INF/jkube-plugin"
  };

  private final List<PluginService> pluginServices;
  private final List<JKubePlugin> plugins;

  public PluginManager(JKubeServiceHub jKubeServiceHub) {
    pluginServices = new PluginServiceFactory<>(jKubeServiceHub).createServiceObjects(PLUGIN_SERVICE_PATH);
    plugins = new PluginServiceFactory<>(null).createServiceObjects(PLUGIN_PATHS);
  }

  public List<JKubePlugin> getPlugins() {
    return plugins;
  }

  /**
   * Returns the {@link PluginService} with the highest score/priority from the list of available services.
   *
   * @return the first applicable PluginService or an {@link IllegalStateException} if none was found.
   */
  public PluginService resolvePluginService() {
    return pluginServices.stream().findFirst()
      .orElseThrow(() -> new IllegalStateException("No suitable Plugin Service was found for your current configuration"));
  }
}
