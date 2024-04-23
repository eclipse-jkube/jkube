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
package org.eclipse.jkube.gradle.plugin.task;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.jkube.kit.config.service.DebugContext;
import org.gradle.api.GradleException;

public class KubernetesDebugTask extends AbstractJKubeTask {

  @Inject
  public KubernetesDebugTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
    setDescription(
        "Ensures that the current app has debug enabled, then opens the debug port so that you can debug the latest Pod from your IDE.");
  }

  @Override
  public void run() {
    try (KubernetesClient kubernetes = jKubeServiceHub.getClient()) {
      final File manifest = getManifest(kubernetes);
      final List<HasMetadata> entities = KubernetesHelper.loadResources(manifest);
      jKubeServiceHub.getDebugService().debug(DebugContext.builder()
              .namespace(kubernetesExtension.getNamespaceOrNull())
              .fileName(manifest.getName())
              .localDebugPort("" + kubernetesExtension.getLocalDebugPortOrDefault())
              .debugSuspend(kubernetesExtension.getDebugSuspendOrDefault())
              .podWaitLog(createLogger("[[Y]][W][[Y]] [[s]]"))
              .jKubeBuildStrategy(kubernetesExtension.getBuildStrategyOrDefault())
              .build(), entities);
    } catch (IOException ex) {
      throw new GradleException("Failure in debug task", ex);
    }
  }
}
