/*
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
package io.jkube.maven.plugin.mojo.develop;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.jkube.kit.config.service.PodLogService;
import io.jkube.maven.plugin.mojo.build.ApplyMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.Set;

/**
 * This goal tails the log of the most recent pod for the app that was deployed via <code>kubernetes:deploy</code>
 * <p>
 * To terminate the log hit
 * <code>Ctrl+C</code>
 */
@Mojo(name = "log", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.VALIDATE)
public class LogMojo extends ApplyMojo {

    @Parameter(property = "jkube.log.follow", defaultValue = "true")
    private boolean followLog;
    @Parameter(property = "jkube.log.container")
    private String logContainerName;
    @Parameter(property = "jkube.log.pod")
    private String podName;

    @Override
    protected void applyEntities(final KubernetesClient kubernetes, final String namespace, String fileName, final Set<HasMetadata> entities) throws Exception {
        getLogService().tailAppPodsLogs(kubernetes, namespace, entities, false, null, followLog, null, true);
    }


    protected PodLogService getLogService() {
        return new PodLogService(getLogServiceContext());
    }

    protected PodLogService.PodLogServiceContext getLogServiceContext() {
        return new PodLogService.PodLogServiceContext.Builder()
                .log(log)
                .logContainerName(logContainerName)
                .podName(podName)
                .newPodLog(createLogger("[[C]][NEW][[C]] "))
                .oldPodLog(createLogger("[[R]][OLD][[R]] "))
                .s2iBuildNameSuffix(s2iBuildNameSuffix)
                .build();
    }
}
