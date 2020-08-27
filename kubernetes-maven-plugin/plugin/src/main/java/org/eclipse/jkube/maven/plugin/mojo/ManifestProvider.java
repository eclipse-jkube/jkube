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
package org.eclipse.jkube.maven.plugin.mojo;

import java.io.File;

import org.eclipse.jkube.kit.common.util.OpenshiftHelper;

import io.fabric8.kubernetes.client.KubernetesClient;

public interface ManifestProvider extends KitLoggerProvider{

  File getKubernetesManifest();

  default File getManifest(KubernetesClient kubernetesClient) {
    if (OpenshiftHelper.isOpenShift(kubernetesClient)) {
      getKitLogger().warn("OpenShift cluster detected, using Kubernetes manifests");
      getKitLogger().warn("Switch to openshift-maven-plugin in case there are any problems");
    }
    return getKubernetesManifest();
  }
}
