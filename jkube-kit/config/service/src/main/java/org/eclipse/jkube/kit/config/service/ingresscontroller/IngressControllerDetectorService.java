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

import org.eclipse.jkube.kit.common.IngressControllerDetector;
import org.eclipse.jkube.kit.common.KitLogger;

import java.util.ArrayList;
import java.util.List;

public class IngressControllerDetectorService {
  private final KitLogger log;
  private final List<IngressControllerDetector> ingressControllerDetectors;

  public IngressControllerDetectorService(KitLogger log) {
    this.log = log;
    ingressControllerDetectors = new ArrayList<>();
  }

  public void setIngressControllerDetectors(List<IngressControllerDetector> detectors) {
    ingressControllerDetectors.addAll(detectors);
  }

  public boolean detect() {
    boolean anyDetectorHadPermission = false;
    for (IngressControllerDetector detector : ingressControllerDetectors) {
      boolean permitted = detector.hasPermissions();
      anyDetectorHadPermission |= permitted;
      if (permitted && detector.isDetected()) {
        return true;
      }
    }
    if (anyDetectorHadPermission) {
      logNoIngressControllerWarning();
    }
    return false;
  }

  private void logNoIngressControllerWarning() {
    log.warn("Applying Ingress resources, but no Ingress Controller seems to be running");
  }
}
