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
package org.eclipse.jkube.gradle.plugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jkube.gradle.plugin.task.KubernetesApplyTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesBuildTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesConfigViewTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesHelmInstallTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesHelmTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesLogTask;
import org.eclipse.jkube.gradle.plugin.task.KubernetesResourceTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftApplyTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftBuildTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftDebugTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftHelmDependencyUpdateTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftHelmInstallTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftHelmLintTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftHelmPushTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftHelmTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftHelmTestTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftHelmUninstallTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftPushTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftRemoteDevTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftResourceTask;
import org.eclipse.jkube.gradle.plugin.task.OpenShiftUndeployTask;

import org.eclipse.jkube.gradle.plugin.task.OpenShiftWatchTask;
import org.gradle.api.Project;
import org.gradle.api.Task;

public class OpenShiftPlugin extends AbstractJKubePlugin<OpenShiftExtension> {

  public OpenShiftPlugin() {
    super("openshift", OpenShiftExtension.class);
  }

  @Override
  public Map<String, Collection<Class<? extends Task>>> getTaskPrecedence() {
    final Map<String, Collection<Class<? extends Task>>> ret = new HashMap<>();
    ret.put("ocApply", Arrays.asList(KubernetesResourceTask.class, OpenShiftResourceTask.class));
    ret.put("ocDebug", Arrays.asList(KubernetesBuildTask.class, OpenShiftBuildTask.class,
        KubernetesResourceTask.class, OpenShiftResourceTask.class, KubernetesApplyTask.class, OpenShiftApplyTask.class));
    ret.put("ocPush", Arrays.asList(KubernetesBuildTask.class, OpenShiftBuildTask.class));
    ret.put("ocHelm", Arrays.asList(KubernetesResourceTask.class, OpenShiftResourceTask.class));
    ret.put("ocHelmPush", Arrays.asList(KubernetesHelmTask.class, OpenShiftHelmTask.class));
    ret.put("ocHelmLint", Arrays.asList(KubernetesHelmTask.class, OpenShiftHelmTask.class));
    ret.put("ocHelmDependencyUpdate", Arrays.asList(KubernetesHelmTask.class, OpenShiftHelmTask.class));
    ret.put("ocHelmInstall", Arrays.asList(KubernetesHelmTask.class, OpenShiftHelmTask.class));
    ret.put("ocHelmUninstall", Arrays.asList(KubernetesHelmTask.class, OpenShiftHelmTask.class, KubernetesHelmInstallTask.class, OpenShiftHelmInstallTask.class));
    ret.put("ocHelmTest", Arrays.asList(KubernetesHelmTask.class, OpenShiftHelmTask.class, KubernetesHelmInstallTask.class, OpenShiftHelmInstallTask.class));
    return ret;
  }

  @Override
  protected void jKubeApply(Project project) {
    register(project, "ocApply", OpenShiftApplyTask.class);
    register(project, "ocBuild", OpenShiftBuildTask.class);
    register(project, "ocConfigView", KubernetesConfigViewTask.class);
    register(project, "ocDebug", OpenShiftDebugTask.class);
    register(project, "ocLog", KubernetesLogTask.class);
    register(project, "ocPush", OpenShiftPushTask.class);
    register(project, "ocResource", OpenShiftResourceTask.class);
    register(project, "ocUndeploy", OpenShiftUndeployTask.class);
    register(project, "ocHelm", OpenShiftHelmTask.class);
    register(project, "ocHelmPush", OpenShiftHelmPushTask.class);
    register(project, "ocHelmLint", OpenShiftHelmLintTask.class);
    register(project, "ocHelmDependencyUpdate", OpenShiftHelmDependencyUpdateTask.class);
    register(project, "ocHelmInstall", OpenShiftHelmInstallTask.class);
    register(project, "ocHelmUninstall", OpenShiftHelmUninstallTask.class);
    register(project, "ocHelmTest", OpenShiftHelmTestTask.class);
    register(project, "ocRemoteDev", OpenShiftRemoteDevTask.class);
    register(project, "ocWatch", OpenShiftWatchTask.class);
  }

}
