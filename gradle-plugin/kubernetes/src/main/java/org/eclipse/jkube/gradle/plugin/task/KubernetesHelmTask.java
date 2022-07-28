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

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.kit.common.util.SummaryUtil;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.initHelmConfig;

public class KubernetesHelmTask extends AbstractJKubeTask {
  @Inject
  public KubernetesHelmTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
    setDescription("Generates a Helm chart for the kubernetes resources.");
  }

  @Override
  public void run() {
    try {
      File manifest = kubernetesExtension.getKubernetesManifestOrDefault();
      if (manifest == null || !manifest.isFile()) {
        logManifestNotFoundWarning(manifest);
      }
      HelmConfig helm = initHelmConfig(kubernetesExtension.getDefaultHelmType(), kubernetesExtension.javaProject,
      kubernetesExtension.getKubernetesManifestOrDefault(), kubernetesExtension.getKubernetesTemplateOrDefault(),
        kubernetesExtension.helm).build();
      jKubeServiceHub.getHelmService().generateHelmCharts(helm);
    } catch (IOException exception) {
      SummaryUtil.setFailureIfSummaryEnabledOrThrow(kubernetesExtension.getSummaryEnabledOrDefault(),
          exception.getMessage(),
          () -> new IllegalStateException(exception.getMessage(), exception));
    }
  }

  protected void logManifestNotFoundWarning(File manifest) {
    kitLogger.warn("No kubernetes manifest file has been generated yet by the k8sResource task at: " + manifest);
  }
}
