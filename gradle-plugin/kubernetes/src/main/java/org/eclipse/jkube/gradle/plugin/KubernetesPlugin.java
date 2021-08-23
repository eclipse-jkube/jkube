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
package org.eclipse.jkube.gradle.plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jkube.gradle.plugin.task.KubernetesApplyTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesBuildTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesConfigViewTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesLogTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesResourceTask;

import org.gradle.api.Project;
import org.gradle.api.Task;

public class KubernetesPlugin extends AbstractJKubePlugin<KubernetesExtension> {

  public KubernetesPlugin() {
    super("kubernetes", KubernetesExtension.class);
  }

  @Override
  public Map<String, Collection<Class<? extends Task>>> getTaskPrecedence() {
    final Map<String, Collection<Class<? extends Task>>> ret = new HashMap<>();
    ret.put("k8sApply", Collections.singletonList(KubernetesResourceTask.class));
    return ret;
  }

  @Override
  protected void jKubeApply(Project project) {
    register(project, "k8sConfigView", KubernetesConfigViewTask.class);
    register(project, "k8sBuild", KubernetesBuildTask.class);
    register(project, "k8sResource", KubernetesResourceTask.class);
    register(project, "k8sApply", KubernetesApplyTask.class);
    register(project, "k8sLog", KubernetesLogTask.class);
  }

}
