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

import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;

public class OpenShiftPushTask extends KubernetesPushTask implements OpenShiftJKubeTask {
  @Inject
  public OpenShiftPushTask(Class<? extends OpenShiftExtension> extensionClass) {
    super(extensionClass);
    setDescription(
      "Uploads the built Docker images to a Docker registry");
  }

}
