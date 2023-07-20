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
package org.eclipse.jkube.kit.config.service.ingresscontroller;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.jkube.kit.common.IngressControllerDetector;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.hasAccessForAction;

public class NginxIngressControllerDetector implements IngressControllerDetector {
  private final KubernetesClient client;
  private static final String INGRESS_NGINX_NAMESPACE = "ingress-nginx";

  public NginxIngressControllerDetector(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public boolean hasIngressClass() {
    return !client.network().v1().ingressClasses()
        .withLabel("app.kubernetes.io/name", INGRESS_NGINX_NAMESPACE)
        .list()
        .getItems()
        .isEmpty();
  }

  @Override
  public boolean isIngressControllerReady() {
    return client.pods()
        .inNamespace(INGRESS_NGINX_NAMESPACE)
        .withLabel("app.kubernetes.io/name", INGRESS_NGINX_NAMESPACE)
        .list()
        .getItems()
        .stream()
        .anyMatch(KubernetesHelper::isPodReady);
  }

  @Override
  public boolean hasPermissions() {
    return hasAccessForAction(client, null, "networking.k8s.io", "ingressclasses", "list")
      && hasAccessForAction(client, INGRESS_NGINX_NAMESPACE, null, "pods", "list");
  }
}
