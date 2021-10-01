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
package org.eclipse.jkube.gradle.plugin.task;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.service.PodLogService;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class KubernetesLogTask extends AbstractJKubeTask {

  @Inject
  public KubernetesLogTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
    setDescription(
      "This goal tails the log of the most recent pod for the app that was deployed via k8sApply task. To terminate the log hit Ctrl+C");
  }

  @Override
  public void run() {
    try (KubernetesClient kubernetes = jKubeServiceHub.getClient()) {
      final File manifest = kubernetesExtension.getManifest(kitLogger, kubernetes);
      List<HasMetadata> entities = KubernetesHelper.loadResources(manifest);

      new PodLogService(podLogServiceContextBuilder().build()).tailAppPodsLogs(
        kubernetes,
          kubernetesExtension.getNamespaceOrNull(),
        entities,
        false,
        null,
        kubernetesExtension.getLogFollowOrDefault(),
        null,
        true
      );
    } catch (IOException exception) {
      throw new IllegalStateException("Failure in getting logs", exception);
    }
  }

  protected PodLogService.PodLogServiceContext.PodLogServiceContextBuilder podLogServiceContextBuilder() {
    return PodLogService.PodLogServiceContext.builder()
        .log(kitLogger)
        .logContainerName(kubernetesExtension.getLogContainerNameOrNull())
        .podName(kubernetesExtension.getLogPodNameOrNull())
        .newPodLog(createLogger("[NEW]"))
        .oldPodLog(createLogger("[OLD]"));
  }
}
