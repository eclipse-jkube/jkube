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

import javax.inject.Inject;

import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;

public class OpenShiftResourceTask extends KubernetesResourceTask implements OpenShiftJKubeTask {

  @Inject
  public OpenShiftResourceTask(Class<? extends OpenShiftExtension> extensionClass) {
    super(extensionClass);
    setDescription(
      "Generates or copies the OpenShift JSON file and attaches it to the build so its installed and released to maven repositories like other build artifacts.");
  }
}
