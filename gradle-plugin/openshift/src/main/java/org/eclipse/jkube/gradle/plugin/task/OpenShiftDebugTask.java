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

import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;

import javax.inject.Inject;

public class OpenShiftDebugTask extends KubernetesDebugTask implements OpenShiftJKubeTask {
  @Inject
  public OpenShiftDebugTask(Class<? extends OpenShiftExtension> extensionClass) {
    super(extensionClass);
    setDescription(
      "Ensures that the current app has debug enabled, then opens the debug port so that you can debug the latest pod from your IDE");
  }
}
