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
package org.eclipse.jkube.kit.common.util;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;

import java.util.List;
import java.util.Map;

public class PodHelper {

  private PodHelper() {}

  public static boolean firstContainerHasEnvVars(Pod pod, Map<String, String> envVars) {
    return envVars.entrySet().stream()
        .allMatch(e -> firstContainerHasEnvVar(pod, e.getKey(), e.getValue()));
  }

  public static boolean firstContainerHasEnvVar(Pod pod, String name, String value) {
    PodSpec spec = pod.getSpec();
    if (spec != null) {
      List<Container> containers = spec.getContainers();
      if (containers != null && !containers.isEmpty()) {
        Container container = containers.get(0);
        List<EnvVar> env = container.getEnv();
        if (env != null) {
          return env.stream()
              .anyMatch(e -> e.getName().equals(name) && e.getValue().equals(value));
        }
      }
    }
    return false;
  }
}
