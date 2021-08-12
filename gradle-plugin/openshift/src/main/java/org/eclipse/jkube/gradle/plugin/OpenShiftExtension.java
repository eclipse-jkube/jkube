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
package org.eclipse.jkube.gradle.plugin;

import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

public abstract class OpenShiftExtension extends KubernetesExtension {

  @Override
  public RuntimeMode getRuntimeMode() {
    return RuntimeMode.OPENSHIFT;
  }

  @Override
  public PlatformMode getPlatformMode() {
    return PlatformMode.openshift;
  }
}
