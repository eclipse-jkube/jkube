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
package org.eclipse.jkube.kit.common.util;

import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.api.model.NamedContextBuilder;
import io.fabric8.kubernetes.client.Config;
import org.eclipse.jkube.kit.common.JKubeException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class KubernetesMockServerUtil {
  private KubernetesMockServerUtil() { }

  public static NamedContext createOpinionatedKubernetesContextForMockKubernetesClientConfiguration(Config kubernetesClientConfig) {
    try {
      if (kubernetesClientConfig != null && kubernetesClientConfig.getCurrentContext() == null) {
        URI uri = new URI(kubernetesClientConfig.getMasterUrl());
        return new NamedContextBuilder()
          .withName("jkube-context")
          .withNewContext()
          .withNamespace(kubernetesClientConfig.getNamespace())
          .withCluster(String.format("%s:%d", uri.getHost(), uri.getPort()))
          .withUser(Optional.ofNullable(kubernetesClientConfig.getUsername())
            .orElse("jkube"))
          .endContext()
          .build();
      }
      return null;
    } catch (URISyntaxException uriSyntaxException) {
      throw new JKubeException("Invalid Kubernetes cluster url ", uriSyntaxException);
    }
  }
}
