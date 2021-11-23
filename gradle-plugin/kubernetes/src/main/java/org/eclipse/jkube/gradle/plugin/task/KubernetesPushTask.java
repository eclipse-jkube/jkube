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
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import javax.inject.Inject;
import java.util.Collections;

public class KubernetesPushTask extends AbstractJKubeTask {
  @Inject
  public KubernetesPushTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
    setDescription("Uploads the built Docker images to a Docker registry");
  }


  @Override
  protected JKubeServiceHub.JKubeServiceHubBuilder initJKubeServiceHubBuilder() {
    return TaskUtil.addDockerServiceHubToJKubeServiceHubBuilder(
        super.initJKubeServiceHubBuilder(), kubernetesExtension, kitLogger)
      .buildServiceConfig(buildServiceConfigBuilder().build());
  }

  @Override
  public void run() {
    try {
      jKubeServiceHub.getBuildService().push(resolvedImages, kubernetesExtension.getPushRetriesOrDefault(), initRegistryConfig(kubernetesExtension.getPushRegistry().getOrNull()), kubernetesExtension.getSkipTagOrDefault());
    } catch (JKubeServiceException e) {
      throw new IllegalStateException("Error in pushing image: " + e.getMessage(), e);
    }
  }


  @Override
  protected boolean canExecute() {
    return super.canExecute() && !kubernetesExtension.getSkipPushOrDefault();
  }

  private RegistryConfig initRegistryConfig(String specificRegistry) {
    return RegistryConfig.builder()
      .settings(Collections.emptyList())
      .authConfig(kubernetesExtension.authConfig != null ? kubernetesExtension.authConfig.toMap() : null)
      .skipExtendedAuth(kubernetesExtension.getSkipExtendedAuthOrDefault())
      .registry(specificRegistry != null ? specificRegistry : kubernetesExtension.getRegistry().getOrNull())
      .passwordDecryptionMethod(password -> password).build();
  }

  protected BuildServiceConfig.BuildServiceConfigBuilder buildServiceConfigBuilder() {
    return TaskUtil.buildServiceConfigBuilder(kubernetesExtension);
  }
}
