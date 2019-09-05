/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jshift.maven.plugin.watcher;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.jshift.kit.build.service.docker.ImageConfiguration;
import io.jshift.kit.common.KitLogger;
import io.jshift.kit.common.util.ClassUtil;
import io.jshift.kit.common.util.OpenshiftHelper;
import io.jshift.kit.common.util.PluginServiceFactory;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.kit.config.resource.ProcessorConfig;
import io.jshift.watcher.api.Watcher;
import io.jshift.watcher.api.WatcherContext;

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
            new PluginServiceFactory<>(watcherCtx, ClassUtil.createProjectClassLoader(watcherCtx.getProject().getCompileClasspathElements(), watcherCtx.getLogger())) :
            new PluginServiceFactory<>(watcherCtx);

        boolean isOpenshift = OpenshiftHelper.isOpenShift(watcherCtx.getKubernetesClient());
        PlatformMode mode = isOpenshift ? PlatformMode.openshift : PlatformMode.kubernetes;

        List<Watcher> watchers =
            pluginFactory.createServiceObjects("META-INF/jshift/watcher-default",
                                               "META-INF/jshift/jshift-watcher-default",
                                               "META-INF/jshift/watcher",
                                               "META-INF/jshift-watcher");

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
