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

import javax.inject.Inject;

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.ImagePullManager;
import org.eclipse.jkube.kit.build.service.docker.ServiceHubFactory;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogOutputSpecFactory;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.BuildRecreateMode;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import java.io.IOException;

import static org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory.DockerAccessContext.DEFAULT_MAX_CONNECTIONS;
import static org.eclipse.jkube.kit.build.service.docker.ImagePullManager.createImagePullManager;
import static org.eclipse.jkube.kit.common.util.BuildReferenceDateUtil.getBuildTimestamp;
import static org.eclipse.jkube.kit.common.util.BuildReferenceDateUtil.getBuildTimestampFile;
import static org.eclipse.jkube.kit.common.util.EnvUtil.storeTimestamp;

@SuppressWarnings("CdiInjectionPointsInspection")
public class KubernetesBuildTask extends AbstractJKubeTask {

  @Inject
  public KubernetesBuildTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
    setDescription(
        "Builds the container images configured for this project via a Docker, S2I binary build or any of the other available build strategies.");
  }

  @Override
  protected JKubeServiceHub.JKubeServiceHubBuilder initJKubeServiceHubBuilder() {
    JKubeServiceHub.JKubeServiceHubBuilder builder = super.initJKubeServiceHubBuilder();
    DockerAccess access = null;
    if (isDockerAccessRequired()) {
      DockerAccessFactory.DockerAccessContext dockerAccessContext = DockerAccessFactory.DockerAccessContext.builder()
          .log(kitLogger)
          .projectProperties(javaProject.getProperties())
          .maxConnections(kubernetesExtension.getMaxConnections().getOrElse(DEFAULT_MAX_CONNECTIONS))
          .dockerHost(kubernetesExtension.getDockerHost().getOrNull())
          .certPath(kubernetesExtension.getCertPath().getOrNull())
          .machine(kubernetesExtension.machine)
          .minimalApiVersion(kubernetesExtension.getMinimalApiVersion().getOrNull())
          .skipMachine(kubernetesExtension.getSkipMachine().getOrElse(false))
          .build();
      DockerAccessFactory dockerAccessFactory = new DockerAccessFactory();
      access = dockerAccessFactory.createDockerAccess(dockerAccessContext);
    }
    ServiceHubFactory serviceHubFactory = new ServiceHubFactory();
    LogOutputSpecFactory logSpecFactory = new LogOutputSpecFactory(true, true, null);
    builder.dockerServiceHub(serviceHubFactory.createServiceHub(access, kitLogger, logSpecFactory));
    ImagePullManager imagePullManager = createImagePullManager(
      kubernetesExtension.getImagePullPolicy().getOrElse("Always"),
      kubernetesExtension.getAutoPull().getOrElse("true"),
      javaProject.getProperties());
    builder.buildServiceConfig(BuildServiceConfig.builder()
      .buildRecreateMode(BuildRecreateMode.fromParameter(kubernetesExtension.getBuildRecreate().getOrElse("none")))
      .jKubeBuildStrategy(kubernetesExtension.getBuildStrategy())
      .forcePull(kubernetesExtension.getForcePull().getOrElse(false))
      .buildDirectory(javaProject.getBuildDirectory().getAbsolutePath())
      .imagePullManager(imagePullManager)
      .enricherTask(e -> {
        enricherManager.enrich(PlatformMode.kubernetes, e);
        enricherManager.enrich(PlatformMode.openshift, e);
      })
      .build());
    return builder;
  }

  @Override
  public void run() {
    if (kubernetesExtension.getRuntimeMode() != RuntimeMode.OPENSHIFT) {
      kitLogger.info("Building container image in Kubernetes mode");
    }
    try {
      for (ImageConfiguration imageConfig : resolvedImages) {
        storeTimestamp(
            getBuildTimestampFile(javaProject.getBuildDirectory().getAbsolutePath(), DOCKER_BUILD_TIMESTAMP),
            getBuildTimestamp(null, null, javaProject.getBuildDirectory().getAbsolutePath(), DOCKER_BUILD_TIMESTAMP));
        jKubeServiceHub.getBuildService().build(imageConfig);
      }
    } catch (JKubeServiceException | IOException e) {
      kitLogger.error(e.getMessage());
    }
  }

  private boolean isDockerAccessRequired() {
    return kubernetesExtension.getBuildStrategy() != JKubeBuildStrategy.jib;
  }

}
