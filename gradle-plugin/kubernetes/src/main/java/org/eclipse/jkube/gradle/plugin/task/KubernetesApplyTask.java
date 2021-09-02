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

import javax.inject.Inject;

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;

@SuppressWarnings("CdiInjectionPointsInspection")
public class KubernetesApplyTask extends AbstractJKubeTask {

  @Inject
  public KubernetesApplyTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
    setDescription("Deploys the generated cluster resource configuration manifests into Kubernetes.");
  }

  @Override
  public void run() {
    throw new UnsupportedOperationException("To be implemented");
  }
}
