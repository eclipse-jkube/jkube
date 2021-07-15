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

import java.util.Collections;

import org.eclipse.jkube.gradle.plugin.GradleUtil;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.Slf4jKitLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import static org.eclipse.jkube.gradle.plugin.KubernetesExtension.DEFAULT_OFFLINE;

public abstract class AbstractJKubeTask extends DefaultTask implements JKubeTask {

  private final KubernetesExtension kubernetesExtension;
  protected JavaProject javaProject;
  protected KitLogger kitLogger;
  protected ClusterAccess clusterAccess;
  protected JKubeServiceHub jKubeServiceHub;

  protected AbstractJKubeTask(Class<? extends KubernetesExtension> extensionClass) {
    kubernetesExtension = getProject().getExtensions().getByType(extensionClass);
  }

  @TaskAction
  public final void runTask() {
    javaProject = GradleUtil.convertGradleProject(getProject());
    kitLogger = new Slf4jKitLogger(getLogger());
    clusterAccess = new ClusterAccess(kitLogger, initClusterConfiguration());
    jKubeServiceHub = initJKubeServiceHubBuilder().build();
    run();
  }

  protected JKubeServiceHub.JKubeServiceHubBuilder initJKubeServiceHubBuilder() {
    return JKubeServiceHub.builder()
        .log(kitLogger)
        .configuration(JKubeConfiguration.builder()
            .project(javaProject)
            .reactorProjects(Collections.singletonList(javaProject))
            .build())
        .clusterAccess(clusterAccess)
        .offline(kubernetesExtension.getOffline().getOrElse(DEFAULT_OFFLINE))
        .platformMode(kubernetesExtension.getRuntimeMode());
  }

  protected ClusterConfiguration initClusterConfiguration() {
    return ClusterConfiguration.from(kubernetesExtension.access,
        System.getProperties(), javaProject.getProperties()).build();
  }

}
