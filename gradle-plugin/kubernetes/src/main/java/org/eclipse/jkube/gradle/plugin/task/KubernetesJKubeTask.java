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
import org.gradle.api.tasks.Internal;

public interface KubernetesJKubeTask extends JKubeTask {

  String DEFAULT_LOG_PREFIX = "k8s: ";

  KubernetesExtension getExtension();

  @Internal
  default String getLogPrefix() {
    return DEFAULT_LOG_PREFIX;
  }
}
