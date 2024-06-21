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
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;

import java.nio.file.Path;

public class KubernetesMockServerUtil {

  private KubernetesMockServerUtil() { }

  // TODO: Remove after https://github.com/fabric8io/kubernetes-client/issues/6068 is fixed
  public static Path exportKubernetesClientConfigToFile(KubernetesMockServer mockServer, Path targetKubeConfig) {
    final KubernetesClient client = mockServer.createClient();
    final NamedContext mockServerContext = new NamedContextBuilder()
      .withName("mock-server")
      .withNewContext()
      .withNamespace(client.getNamespace())
      .withCluster(String.format("%s:%d", mockServer.getHostName(), mockServer.getPort()))
      .withUser("mock-server-user")
      .endContext()
      .build();
    final Config kubernetesClientConfig = new ConfigBuilder(client.getConfiguration())
      .addToContexts(mockServerContext)
      .withCurrentContext(mockServerContext)
      .build();
    return KubernetesHelper.exportKubernetesClientConfigToFile(kubernetesClientConfig, targetKubeConfig);
  }
}
