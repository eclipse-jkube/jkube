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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;

public class PodHelper {

  private PodHelper() {}

  public static boolean firstContainerHasEnvVars(Pod pod, Map<String, String> envVars) {
    final List<Container> containers = Optional.ofNullable(pod).map(Pod::getSpec).map(PodSpec::getContainers)
        .orElse(Collections.emptyList());
    return envVars.entrySet().stream()
        .allMatch(e -> firstContainerHasEnvVar(containers, e.getKey(), e.getValue()));
  }

  public static boolean firstContainerHasEnvVars(List<Container> containers, Map<String, String> envVars) {
    return envVars.entrySet().stream()
        .allMatch(e -> firstContainerHasEnvVar(containers, e.getKey(), e.getValue()));
  }

  public static boolean firstContainerHasEnvVar(List<Container> containers, String name, String value) {
    if (containers != null && !containers.isEmpty()) {
      Container container = containers.get(0);
      List<EnvVar> env = container.getEnv();
      if (env != null) {
        return env.stream()
            .anyMatch(e -> e.getName().equals(name) && e.getValue().equals(value));
      }
    }
    return false;
  }
}
