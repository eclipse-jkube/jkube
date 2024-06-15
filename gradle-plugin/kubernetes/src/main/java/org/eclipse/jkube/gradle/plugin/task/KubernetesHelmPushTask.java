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

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;

import javax.inject.Inject;

import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.initHelmConfig;
import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.initHelmPushConfig;

public class KubernetesHelmPushTask extends AbstractHelmTask {
  @Inject
  public KubernetesHelmPushTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
    setDescription("Upload a Helm chart to specified Helm repository.");
  }

  @Override
  protected void executeTask(HelmConfig helmConfig) {
    try {
      initHelmPushConfig(helmConfig, kubernetesExtension.javaProject);
      jKubeServiceHub.getHelmService().uploadHelmChart(helmConfig);
    } catch (Exception exp) {
      kitLogger.error("Error performing Helm push", exp);
      throw new IllegalStateException(exp.getMessage(), exp);
    }
  }
}
