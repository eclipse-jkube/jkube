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
package org.eclipse.jkube.kit.config.service.ingresscontroller;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.jkube.kit.common.IngressControllerDetector;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.hasAccessForAction;

public class ContourIngressControllerDetector implements IngressControllerDetector {

  private static final String INGRESS_CONTOUR_NAMESPACE = "projectcontour";
  private final KubernetesClient client;

  public ContourIngressControllerDetector(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public boolean hasIngressClass() {
    return !client.network().v1().ingressClasses()
        .withLabel("app.kubernetes.io/name", "contour")
        .list()
        .getItems()
        .isEmpty();
  }

  @Override
  public boolean isIngressControllerReady() {
    return isComponentReady("envoy") &&
        isComponentReady("contour");
  }

  @Override
  public boolean hasPermissions() {
    return hasAccessForAction(client, null, "networking.k8s.io", "ingressclasses", "list")
      && hasAccessForAction(client, INGRESS_CONTOUR_NAMESPACE, null, "pods", "list");
  }

  private boolean isComponentReady(String componentName) {
    return client.pods()
        .inNamespace(INGRESS_CONTOUR_NAMESPACE)
        .withLabel("app.kubernetes.io/component", componentName)
        .list()
        .getItems()
        .stream()
        .anyMatch(KubernetesHelper::isPodReady);
  }
}
