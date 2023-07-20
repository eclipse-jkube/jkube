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
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.common.IngressControllerDetector;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;

import java.util.Objects;

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.hasAccessForAction;

public class OpenShiftIngressControllerDetector implements IngressControllerDetector {
  private final KubernetesClient client;
  private static final String INGRESS_OPENSHIFT_NAMESPACE = "openshift-ingress-operator";

  public OpenShiftIngressControllerDetector(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public boolean hasIngressClass() {
    return client.network().v1().ingressClasses()
        .list()
        .getItems()
        .stream()
        .anyMatch(i -> i.getSpec().getController().equals("openshift.io/ingress-to-route"));
  }

  @Override
  public boolean isIngressControllerReady() {
    if (OpenshiftHelper.isOpenShift(client)) {
      OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
      return openShiftClient.operator().ingressControllers()
          .inNamespace(INGRESS_OPENSHIFT_NAMESPACE)
          .list()
          .getItems()
          .stream()
          .anyMatch(o -> Objects.equals(o.getSpec().getReplicas(), o.getStatus().getAvailableReplicas()));
    }
    return false;
  }

  @Override
  public boolean hasPermissions() {
    return hasAccessForAction(client, null, "networking.k8s.io", "ingressclasses", "list")
      && hasAccessForAction(client, INGRESS_OPENSHIFT_NAMESPACE, "operator.openshift.io", "ingresscontrollers", "list");
  }
}

