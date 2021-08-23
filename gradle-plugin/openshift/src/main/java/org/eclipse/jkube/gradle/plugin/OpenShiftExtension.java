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

import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.gradle.api.provider.Property;

import java.io.File;

public abstract class OpenShiftExtension extends KubernetesExtension {
  public static final String DEFAULT_OPENSHIFT_MANIFEST = "build/META-INF/jkube/openshift.yml";

  public abstract Property<File> getOpenShiftManifest();

  @Override
  public RuntimeMode getRuntimeMode() {
    return RuntimeMode.OPENSHIFT;
  }

  @Override
  public PlatformMode getPlatformMode() {
    return PlatformMode.openshift;
  }

  @Override
  public File getManifest(KitLogger kitLogger, KubernetesClient kubernetesClient, JavaProject javaProject) {
    if (OpenshiftHelper.isOpenShift(kubernetesClient)) {
      return getOpenShiftManifest().getOrElse(new File(javaProject.getBaseDirectory(), DEFAULT_OPENSHIFT_MANIFEST));
    }
    return getKubernetesManifestOrDefault(javaProject);
  }
}
