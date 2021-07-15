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
public class KubernetesBuildTask extends AbstractJKubeTask {

  @Inject
  public KubernetesBuildTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
    setDescription("Builds the container images configured for this project via a Docker, S2I binary build or any of the other available build strategies.");
  }

  @Override
  public void run() {
    throw new UnsupportedOperationException("To be implemented");
  }
}
