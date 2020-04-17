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
package org.eclipse.jkube.maven.plugin.mojo.develop;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.jkube.kit.config.service.PodLogService;
import org.eclipse.jkube.maven.plugin.mojo.build.ApplyMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.Set;

/**
 * This goal tails the log of the most recent pod for the app that was deployed via <code>k8s:deploy</code>
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
        return PodLogService.PodLogServiceContext.builder()
                .log(log)
                .logContainerName(logContainerName)
                .podName(podName)
                .newPodLog(createLogger("[[C]][NEW][[C]] "))
                .oldPodLog(createLogger("[[R]][OLD][[R]] "))
                .s2iBuildNameSuffix(s2iBuildNameSuffix)
                .build();
    }
}
