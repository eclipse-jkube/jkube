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
import org.eclipse.jkube.kit.common.util.LazyBuilder;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.config.service.ResourceServiceConfig;
import org.eclipse.jkube.kit.resource.service.DefaultResourceService;

import java.io.File;

@SuppressWarnings("CdiInjectionPointsInspection")
public class KubernetesResourceTask extends AbstractJKubeTask {

  @Override
  protected JKubeServiceHub.JKubeServiceHubBuilder initJKubeServiceHubBuilder() {
    JKubeServiceHub.JKubeServiceHubBuilder builder = super.initJKubeServiceHubBuilder();
    File realResourceDir = kubernetesExtension.getResourceDirectory(javaProject);
    ResourceConfig resourceConfig = kubernetesExtension.resources;
    if (kubernetesExtension.getNamespace().getOrNull() != null) {
      resourceConfig = ResourceConfig.toBuilder(resourceConfig).namespace(kubernetesExtension.getNamespace().getOrNull()).build();
    }
    final ResourceServiceConfig resourceServiceConfig = ResourceServiceConfig.builder()
      .project(javaProject)
      .resourceDir(realResourceDir)
      .targetDir(kubernetesExtension.getResourceTargetDirectoryOrDefault(javaProject))
      .resourceFileType(kubernetesExtension.getResourceFileType())
      .resourceConfig(resourceConfig)
      .interpolateTemplateParameters(kubernetesExtension.getInterpolateTemplateParametersOrDefault())
      .build();
    builder.resourceService(new LazyBuilder<>(() -> new DefaultResourceService(resourceServiceConfig)));

    return builder;
  }

  @Inject
  public KubernetesResourceTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
    setDescription("Generates cluster resource configuration manifests.");
  }

  @Override
  public void run() {
    throw new UnsupportedOperationException("To be implemented");
  }
}
