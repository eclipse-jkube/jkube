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
import org.eclipse.jkube.kit.resource.helm.HelmConfig;

import javax.inject.Inject;

import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.initHelmConfig;
import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.initHelmPushConfig;

public class KubernetesHelmPushTask extends AbstractJKubeTask {
  @Inject
  public KubernetesHelmPushTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
    setDescription("Upload a helm chart to specified helm repository.");
  }

  @Override
  public void run() {
    if (kubernetesExtension.getSkipOrDefault()) {
      return;
    }
    try {
      HelmConfig helm = initHelmConfig(kubernetesExtension.getDefaultHelmType(), kubernetesExtension.javaProject,
        kubernetesExtension.getKubernetesManifestOrDefault(), kubernetesExtension.getKubernetesTemplateOrDefault(),
        kubernetesExtension.helm).build();
      helm = initHelmPushConfig(helm, kubernetesExtension.javaProject);
      jKubeServiceHub.getHelmService().uploadHelmChart(helm);
    } catch (Exception exp) {
      kitLogger.error("Error performing helm push", exp);
      throw new IllegalStateException(exp.getMessage(), exp);
    }
  }
}
