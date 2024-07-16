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

import javax.inject.Inject;

public class KubernetesHelmInstallTask extends AbstractHelmTask {
  @Inject
  public KubernetesHelmInstallTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
    setDescription("Installs a helm chart onto Kubernetes cluster");
  }

  @Override
  public void run() {
    try {
      jKubeServiceHub.getHelmService().install(kubernetesExtension.helm);
    } catch (Exception exp) {
      kitLogger.error("Error performing helm install", exp);
      throw new IllegalStateException(exp.getMessage(), exp);
    }
  }
}
