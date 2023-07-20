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
package org.eclipse.jkube.maven.plugin.mojo.develop;

import java.util.Collection;

import org.eclipse.jkube.kit.config.service.PodLogService;
import org.eclipse.jkube.maven.plugin.mojo.build.ApplyMojo;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * This goal tails the log of the most recent pod for the app that was deployed via <code>k8s:deploy</code>
 * <p> To terminate the log hit
 * <code>Ctrl+C</code>
 */
@Mojo(name = "log", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.VALIDATE)
public class LogMojo extends ApplyMojo {

  @Parameter(property = "jkube.log.follow", defaultValue = "true")
  protected boolean logFollow;
  @Parameter(property = "jkube.log.container")
  protected String logContainerName;
  @Parameter(property = "jkube.log.pod")
  protected String logPodName;

  @Override
  protected void applyEntities(final KubernetesClient kubernetes, String fileName, final Collection<HasMetadata> entities) {
    new PodLogService(podLogServiceContextBuilder().build()).tailAppPodsLogs(
        kubernetes,
        applyService.getNamespace(),
        entities,
        false,
        null,
        logFollow,
        null,
        true
    );
  }

  protected PodLogService.PodLogServiceContext.PodLogServiceContextBuilder podLogServiceContextBuilder() {
    return PodLogService.PodLogServiceContext.builder()
        .log(log)
        .logContainerName(logContainerName)
        .podName(logPodName)
        .newPodLog(createLogger("[[C]][NEW][[C]] "))
        .oldPodLog(createLogger("[[R]][OLD][[R]] "));
  }
}
