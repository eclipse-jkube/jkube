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
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.gradle.api.GradleException;

import java.io.IOException;

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
    return TaskUtil.addDockerServiceHubToJKubeServiceHubBuilder(super.initJKubeServiceHubBuilder(), kubernetesExtension, javaProject, kitLogger)
      .buildServiceConfig(buildServiceConfigBuilder().build());
  }

  @Override
  public void run() {
    try {
      for (ImageConfiguration imageConfig : resolvedImages) {
        storeTimestamp(
            getBuildTimestampFile(javaProject.getBuildDirectory().getAbsolutePath(), DOCKER_BUILD_TIMESTAMP),
            getBuildTimestamp(null, null, javaProject.getBuildDirectory().getAbsolutePath(), DOCKER_BUILD_TIMESTAMP));
        jKubeServiceHub.getBuildService().build(imageConfig);
      }
    } catch (JKubeServiceException | IOException e) {
      kitLogger.error(e.getMessage());
      throw new GradleException(e.getMessage(), e);
    }
  }

  protected BuildServiceConfig.BuildServiceConfigBuilder buildServiceConfigBuilder() {
    return TaskUtil.buildServiceConfigBuilder(kubernetesExtension, javaProject);
  }
}
