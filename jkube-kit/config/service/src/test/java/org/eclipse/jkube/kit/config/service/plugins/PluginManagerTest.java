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
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.LazyBuilder;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PluginManagerTest {

  private JKubeServiceHub jKubeServiceHub;

  @BeforeEach
  void setUp() {
    jKubeServiceHub = new JKubeServiceHub(null, RuntimeMode.KUBERNETES, new KitLogger.StdoutLogger(),
      null, new JKubeConfiguration(), new BuildServiceConfig(), new ResourceServiceConfig(),
      new LazyBuilder<>(hub -> null), true);
  }

  @Test
  void pluginsCanBeLoadedInOrder() {
    // When
    final List<JKubePlugin> result = jKubeServiceHub.getPluginManager().getPlugins();
    // Then
    assertThat(result)
      .extracting(p -> p.getClass().getSimpleName())
      .containsExactly("PluginThree", "PluginOne", "PluginTwo");

  }

  @Test
  void defaultPluginServiceIsLoaded() {
    // When
    final PluginService pluginService = jKubeServiceHub.getPluginManager().resolvePluginService();
    // Then
    assertThat(pluginService).isInstanceOf(DefaultPluginService.class);
  }

}
