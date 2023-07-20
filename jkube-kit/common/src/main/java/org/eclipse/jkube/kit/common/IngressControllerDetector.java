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
package org.eclipse.jkube.kit.common;

import io.fabric8.kubernetes.client.KubernetesClientException;

public interface IngressControllerDetector {
  /**
   * Returns boolean value whether Ingress Controller is running in Kubernetes Cluster
   * @return true if Ingress Controller is present, false otherwise
   */
  default boolean isDetected() {
    try {
      return hasIngressClass() && isIngressControllerReady();
    } catch (KubernetesClientException exception) {
      return false;
    }
  }

  /**
   * Returns whether IngressClass for corresponding controller is installed in cluster
   *
   * @return true if IngressClass is installed, false otherwise
   */
  boolean hasIngressClass();

  /**
   * Is Ingress Controller pod running in the cluster
   *
   * @return true if Ingress Controller is running, false otherwise.
   */
  boolean isIngressControllerReady();

  /**
   * Returns boolean value whether currently logged user has got enough permissions to
   * check for current IngressController implementation
   * @return true if it has permissions, false otherwise
   */
  boolean hasPermissions();
}
