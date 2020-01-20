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
package org.eclipse.jkube.maven.plugin.watcher;

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ClassUtil;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.common.util.PluginServiceFactory;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.watcher.api.Watcher;
import org.eclipse.jkube.watcher.api.WatcherContext;

import java.util.List;
import java.util.Set;

/**
 * Manager responsible for finding and calling watchers
 *
 * @author nicola
 * @since 06/02/17
 */
public class WatcherManager {

    public static void watch(List<ImageConfiguration> ret, Set<HasMetadata> resources, WatcherContext watcherCtx) throws Exception {

        PluginServiceFactory<WatcherContext> pluginFactory =
                watcherCtx.isUseProjectClasspath() ?
            new PluginServiceFactory<>(watcherCtx, ClassUtil.createProjectClassLoader(watcherCtx.getProject().getCompileClassPathElements(), watcherCtx.getLogger())) :
            new PluginServiceFactory<>(watcherCtx);

        boolean isOpenshift = OpenshiftHelper.isOpenShift(watcherCtx.getKubernetesClient());
        PlatformMode mode = isOpenshift ? PlatformMode.openshift : PlatformMode.kubernetes;

        List<Watcher> watchers =
            pluginFactory.createServiceObjects("META-INF/jkube/watcher-default",
                                               "META-INF/jkube/jkube-watcher-default",
                                               "META-INF/jkube/watcher",
                                               "META-INF/jkube-watcher");

        ProcessorConfig config = watcherCtx.getConfig();
        KitLogger log = watcherCtx.getLogger();
        List<Watcher> usableWatchers  = config.prepareProcessors(watchers, "watcher");
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
        chosen.watch(ret, resources, mode);
    }
}
