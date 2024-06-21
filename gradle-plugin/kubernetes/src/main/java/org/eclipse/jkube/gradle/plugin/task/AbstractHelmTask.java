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

import java.io.IOException;

import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.initHelmConfig;

public abstract class AbstractHelmTask extends AbstractJKubeTask {

  protected AbstractHelmTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
  }

  @Override
  protected void init() {
    super.init();

    try {
      kubernetesExtension.helm = initHelmConfig(
        kubernetesExtension.getDefaultHelmType(),
        kubernetesExtension.javaProject,
        kubernetesExtension.getKubernetesTemplateOrDefault(),
        kubernetesExtension.helm
      ).build();
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

}
