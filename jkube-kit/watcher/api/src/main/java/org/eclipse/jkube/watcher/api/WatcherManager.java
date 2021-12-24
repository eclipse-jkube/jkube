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
package org.eclipse.jkube.watcher.api;

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ClassUtil;
import org.eclipse.jkube.kit.common.util.PluginServiceFactory;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;

import java.util.Collection;
import java.util.List;

/**
 * Manager responsible for finding and calling watchers
 */
public class WatcherManager {

  private static final String[] SERVICE_PATHS = new String[] {
      "META-INF/jkube/watcher-default",
      "META-INF/jkube/jkube-watcher-default",
      "META-INF/jkube/watcher",
      "META-INF/jkube-watcher"
  };

  private WatcherManager() {
  }

  public static void watch(List<ImageConfiguration> ret, String namespace, Collection<HasMetadata> resources, WatcherContext watcherCtx)
      throws Exception {

    final PluginServiceFactory<WatcherContext> pluginFactory = new PluginServiceFactory<>(watcherCtx);
    if (watcherCtx.isUseProjectClasspath()) {
      pluginFactory.addAdditionalClassLoader(ClassUtil.createProjectClassLoader(
          watcherCtx.getBuildContext().getProject().getCompileClassPathElements(), watcherCtx.getLogger()));
    }

    final boolean isOpenshift = watcherCtx.getJKubeServiceHub().getClusterAccess().isOpenShift();
    final PlatformMode mode = isOpenshift ? PlatformMode.openshift : PlatformMode.kubernetes;

    final KitLogger log = watcherCtx.getLogger();
    final List<Watcher> watchers = pluginFactory.createServiceObjects(SERVICE_PATHS);
    final List<Watcher> usableWatchers = watcherCtx.getConfig().prepareProcessors(watchers, "watcher");
    log.verbose("Watchers:");
    Watcher chosen = null;
    for (Watcher watcher : usableWatchers) {
      if (watcher.isApplicable(ret, resources, mode)) {
        if (chosen == null) {
          log.verbose(" - %s [selected]", watcher.getName());
          chosen = watcher;
        } else {
          log.verbose(" - %s", watcher.getName());
        }
      } else {
        log.verbose(" - %s [not applicable]", watcher.getName());
      }
    }

    if (chosen == null) {
      throw new IllegalStateException("No watchers can be used for the current project");
    }

    log.info("Running watcher %s", chosen.getName());
    chosen.watch(ret, namespace, resources, mode);
  }
}
