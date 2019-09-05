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
package io.jshift.watcher.api;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.jshift.kit.build.service.docker.BuildService;
import io.jshift.kit.build.service.docker.ServiceHub;
import io.jshift.kit.build.service.docker.WatchService;
import io.jshift.kit.common.KitLogger;
import io.jshift.kit.config.access.ClusterConfiguration;
import io.jshift.kit.config.resource.RuntimeMode;
import io.jshift.kit.config.resource.ProcessorConfig;
import io.jshift.kit.config.service.JshiftServiceHub;
import org.apache.maven.project.MavenProject;

/**
 * @author nicola
 * @since 06/02/17
 */
public class WatcherContext {

    private MavenProject project;
    private ProcessorConfig config;
    private KitLogger logger;
    private KitLogger newPodLogger;
    private KitLogger oldPodLogger;
    private RuntimeMode mode;
    private boolean useProjectClasspath;
    private ServiceHub serviceHub;
    private WatchService.WatchContext watchContext;
    private BuildService.BuildContext buildContext;
    private ClusterConfiguration clusterConfiguration;
    private KubernetesClient kubernetesClient;
    private JshiftServiceHub fabric8ServiceHub;

    private WatcherContext() {
    }

    public MavenProject getProject() {
        return project;
    }

    public ProcessorConfig getConfig() {
        return config;
    }

    public KitLogger getLogger() {
        return logger;
    }

    public RuntimeMode getMode() {
        return mode;
    }

    public boolean isUseProjectClasspath() {
        return useProjectClasspath;
    }

    public ServiceHub getServiceHub() {
        return serviceHub;
    }

    public WatchService.WatchContext getWatchContext() {
        return watchContext;
    }

    public BuildService.BuildContext getBuildContext() {
        return buildContext;
    }

    public ClusterConfiguration getClusterConfiguration() {
        return clusterConfiguration;
    }

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    public KitLogger getNewPodLogger() {
        return newPodLogger;
    }

    public KitLogger getOldPodLogger() {
        return oldPodLogger;
    }

    public JshiftServiceHub getFabric8ServiceHub() {
        return fabric8ServiceHub;
    }

    // ========================================================================

    public static class Builder {

        private WatcherContext ctx = new WatcherContext();

        public Builder project(MavenProject project) {
            ctx.project = project;
            return this;
        }

        public Builder config(ProcessorConfig config) {
            ctx.config = config;
            return this;
        }

        public Builder logger(KitLogger logger) {
            ctx.logger = logger;
            return this;
        }

        public Builder newPodLogger(KitLogger newPodLogger) {
            ctx.newPodLogger = newPodLogger;
            return this;
        }

        public Builder oldPodLogger(KitLogger oldPodLogger) {
            ctx.oldPodLogger = oldPodLogger;
            return this;
        }

        public Builder mode(RuntimeMode mode) {
            ctx.mode = mode;
            return this;
        }

        public Builder useProjectClasspath(boolean useProjectClasspath) {
            ctx.useProjectClasspath = useProjectClasspath;
            return this;
        }

        public Builder serviceHub(ServiceHub serviceHub) {
            ctx.serviceHub = serviceHub;
            return this;
        }

        public Builder watchContext(WatchService.WatchContext watchContext) {
            ctx.watchContext = watchContext;
            return this;
        }

        public Builder buildContext(BuildService.BuildContext buildContext) {
            ctx.buildContext = buildContext;
            return this;
        }

        public Builder clusterConfiguration(ClusterConfiguration clusterConfiguration) {
            ctx.clusterConfiguration = clusterConfiguration;
            return this;
        }

        public Builder kubernetesClient(KubernetesClient kubernetesClient) {
            ctx.kubernetesClient = kubernetesClient;
            return this;
        }

        public Builder fabric8ServiceHub(JshiftServiceHub fabric8ServiceHub) {
            ctx.fabric8ServiceHub = fabric8ServiceHub;
            return this;
        }

        public WatcherContext build() {
            return ctx;
        }
    }
}
