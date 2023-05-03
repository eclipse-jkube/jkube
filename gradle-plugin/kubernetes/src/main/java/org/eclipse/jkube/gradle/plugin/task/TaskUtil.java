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

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.ImagePullManager;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.BuildRecreateMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;

import static org.eclipse.jkube.kit.build.service.docker.ImagePullManager.createImagePullManager;

public class TaskUtil {

  private TaskUtil() { }

  public static BuildServiceConfig.BuildServiceConfigBuilder buildServiceConfigBuilder(
      KubernetesExtension kubernetesExtension) {

    final ImagePullManager imagePullManager = createImagePullManager(
        kubernetesExtension.getImagePullPolicyOrNull(),
        kubernetesExtension.getAutoPullOrNull(),
        kubernetesExtension.javaProject.getProperties());
    return BuildServiceConfig.builder()
        .imagePullManager(imagePullManager)
        .buildRecreateMode(BuildRecreateMode.fromParameter(kubernetesExtension.getBuildRecreateOrDefault()))
        .jKubeBuildStrategy(kubernetesExtension.getBuildStrategyOrDefault())
        .forcePull(kubernetesExtension.getForcePullOrDefault())
        .buildDirectory(kubernetesExtension.javaProject.getBuildDirectory().getAbsolutePath());
  }

  public static DockerAccess initDockerAccess(KubernetesExtension kubernetesExtension, KitLogger kitLogger) {
    if (!kubernetesExtension.isDockerAccessRequired()) {
      return null;
    }
    final DockerAccessFactory.DockerAccessContext dockerAccessContext = DockerAccessFactory.DockerAccessContext.builder()
        .log(kitLogger)
        .projectProperties(kubernetesExtension.javaProject.getProperties())
        .maxConnections(kubernetesExtension.getMaxConnectionsOrDefault())
        .dockerHost(kubernetesExtension.getDockerHostOrNull())
        .certPath(kubernetesExtension.getCertPathOrNull())
        .machine(kubernetesExtension.machine)
        .skipMachine(kubernetesExtension.getSkipMachineOrDefault())
        .build();
    DockerAccessFactory dockerAccessFactory = new DockerAccessFactory();
    return dockerAccessFactory.createDockerAccess(dockerAccessContext);
  }
}
