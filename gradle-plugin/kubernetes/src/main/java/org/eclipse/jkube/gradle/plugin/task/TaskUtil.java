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
import org.eclipse.jkube.kit.build.service.docker.ServiceHubFactory;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogOutputSpecFactory;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.BuildRecreateMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

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

  public static JKubeServiceHub.JKubeServiceHubBuilder addDockerServiceHubToJKubeServiceHubBuilder(
      JKubeServiceHub.JKubeServiceHubBuilder builder, KubernetesExtension kubernetesExtension, KitLogger kitLogger, ServiceHubFactory serviceHubFactory) {
    DockerAccess access = null;
    if (kubernetesExtension.isDockerAccessRequired()) {
      DockerAccessFactory.DockerAccessContext dockerAccessContext = DockerAccessFactory.DockerAccessContext.builder()
          .log(kitLogger)
          .projectProperties(kubernetesExtension.javaProject.getProperties())
          .maxConnections(kubernetesExtension.getMaxConnectionsOrDefault())
          .dockerHost(kubernetesExtension.getDockerHostOrNull())
          .certPath(kubernetesExtension.getCertPathOrNull())
          .machine(kubernetesExtension.machine)
          .minimalApiVersion(kubernetesExtension.getMinimalApiVersion().getOrNull())
          .skipMachine(kubernetesExtension.getSkipMachineOrDefault())
          .build();
      DockerAccessFactory dockerAccessFactory = new DockerAccessFactory();
      access = dockerAccessFactory.createDockerAccess(dockerAccessContext);
    }
    LogOutputSpecFactory logSpecFactory = new LogOutputSpecFactory(true, true, null);
    builder.dockerServiceHub(serviceHubFactory.createServiceHub(access, kitLogger, logSpecFactory));
    return builder;
  }
}
