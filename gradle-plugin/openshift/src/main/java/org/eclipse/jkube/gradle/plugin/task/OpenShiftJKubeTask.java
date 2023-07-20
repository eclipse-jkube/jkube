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
import org.gradle.api.tasks.Internal;

public interface OpenShiftJKubeTask extends KubernetesJKubeTask {

  String DEFAULT_LOG_PREFIX = "oc: ";

  @Internal
  default OpenShiftExtension getOpenShiftExtension() {
    return (OpenShiftExtension) getExtension();
  }

  @Internal
  @Override
  default String getLogPrefix() {
    return DEFAULT_LOG_PREFIX;
  }
}
