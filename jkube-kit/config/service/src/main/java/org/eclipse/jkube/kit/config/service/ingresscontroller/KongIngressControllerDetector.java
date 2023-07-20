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

public class KongIngressControllerDetector implements IngressControllerDetector {
  private final KubernetesClient client;
  private static final String INGRESS_KONG_NAMESPACE = "kong";

  public KongIngressControllerDetector(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public boolean hasIngressClass() {
    return client.network().v1().ingressClasses()
        .list()
        .getItems()
        .stream()
        .anyMatch(i -> i.getSpec().getController().equals("ingress-controllers.konghq.com/kong"));
  }

  @Override
  public boolean isIngressControllerReady() {
    return checkHelmInstallationAvailable() || checkManualInstallationAvailable();
  }

  @Override
  public boolean hasPermissions() {
    return hasAccessForAction(client, null, "networking.k8s.io", "ingressclasses", "list")
      && hasAccessForAction(client, null, null, "pods", "list");
  }

  private boolean checkManualInstallationAvailable() {
    return client.pods()
        .inNamespace(INGRESS_KONG_NAMESPACE)
        .withLabel("app", "ingress-kong")
        .list()
        .getItems()
        .stream()
        .anyMatch(KubernetesHelper::isPodReady);
  }

  private boolean checkHelmInstallationAvailable() {
    return client.pods()
        .inAnyNamespace()
        .withLabel("app.kubernetes.io/name", "kong")
        .list()
        .getItems()
        .stream()
        .anyMatch(KubernetesHelper::isPodReady);
  }
}
