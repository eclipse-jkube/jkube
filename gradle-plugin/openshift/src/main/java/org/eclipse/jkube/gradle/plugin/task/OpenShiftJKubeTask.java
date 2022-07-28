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
package org.eclipse.jkube.gradle.plugin.task;

import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.gradle.api.tasks.Internal;

import java.util.HashMap;
import java.util.Map;

public interface OpenShiftJKubeTask extends KubernetesJKubeTask {

  String DEFAULT_LOG_PREFIX = "oc: ";

  @Internal
  default OpenShiftExtension getOpenShiftExtension() {
    return (OpenShiftExtension) getExtension();
  }

  @Internal
  @Override
  default String getLogPrefix() {
    return DEFAULT_LOG_PREFIX;
  }

  @Internal
  @Override
  default Map<String, Integer> getTaskPrioritiesMap() {
    Map<String, Integer> openshiftPluginTaskPrioritiesMap = new HashMap<>();
    openshiftPluginTaskPrioritiesMap.put("ocBuild", 1);
    openshiftPluginTaskPrioritiesMap.put("ocResource", 2);
    openshiftPluginTaskPrioritiesMap.put("ocPush", 2);
    openshiftPluginTaskPrioritiesMap.put("ocHelm", 3);
    openshiftPluginTaskPrioritiesMap.put("ocHelmPush", 4);
    openshiftPluginTaskPrioritiesMap.put("ocApply", 5);
    openshiftPluginTaskPrioritiesMap.put("ocLog", 6);
    openshiftPluginTaskPrioritiesMap.put("ocDebug", 6);
    openshiftPluginTaskPrioritiesMap.put("ocWatch", 6);
    openshiftPluginTaskPrioritiesMap.put("ocUndeploy", 7);
    return openshiftPluginTaskPrioritiesMap;
  }
}
