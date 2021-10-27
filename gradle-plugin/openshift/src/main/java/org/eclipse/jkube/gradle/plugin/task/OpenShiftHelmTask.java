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
import java.io.File;

public class OpenShiftHelmTask extends KubernetesHelmTask implements OpenShiftJKubeTask {

  @Inject
  public OpenShiftHelmTask(Class<? extends OpenShiftExtension> extensionClass) {
    super(extensionClass);
    setDescription(
      "Generates a Helm chart for the OpenShift resources.");
  }

  @Override
  protected void logManifestNotFoundWarning(File manifest) {
    kitLogger.warn("No OpenShift manifest file has been generated yet by the ocResource task at: " + manifest);
  }
}
