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

import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;

import javax.inject.Inject;

public class OpenShiftRemoteDevTask extends KubernetesRemoteDevTask implements OpenShiftJKubeTask {
  @Inject
  public OpenShiftRemoteDevTask(Class<? extends OpenShiftExtension> extensionClass) {
    super(extensionClass);
    setDescription(
        "Starts a new JKube remote development session.");
  }
}
