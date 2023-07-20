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

import org.eclipse.jkube.kit.common.IngressControllerDetector;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.PluginServiceFactory;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import java.util.List;

public class IngressControllerDetectorManager {

  private static final String[] SERVICE_PATHS = new String[] {
    "META-INF/jkube/ingress-detectors"
  };
  private final KitLogger log;
  private final List<IngressControllerDetector> ingressControllerDetectors;

  public IngressControllerDetectorManager(JKubeServiceHub jKubeServiceHub) {
    this(jKubeServiceHub.getLog(), new PluginServiceFactory<>(jKubeServiceHub.getClient())
      .createServiceObjects(SERVICE_PATHS));
  }

  IngressControllerDetectorManager(KitLogger log, List<IngressControllerDetector> ingressControllerDetectors) {
    this.log = log;
    this.ingressControllerDetectors = ingressControllerDetectors;
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
      log.warn("Ingress resources applied. However, no IngressController seems to be running at the moment, your service will most likely be not accessible.");
    }
    return false;
  }
}
