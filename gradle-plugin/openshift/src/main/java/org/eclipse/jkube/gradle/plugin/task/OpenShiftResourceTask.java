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
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.eclipse.jkube.kit.build.service.docker.helper.ImageNameFormatter.DOCKER_IMAGE_USER;

public class OpenShiftResourceTask extends KubernetesResourceTask {
  @Inject
  public OpenShiftResourceTask(Class<? extends OpenShiftExtension> extensionClass) {
    super(extensionClass);
    setDescription(
      "Generates or copies the OpenShift JSON file and attaches it to the build so its installed and released to maven repositories like other build artifacts.");
  }

  @Override
  public List<ImageConfiguration> resolveImages(ImageConfigResolver imageConfigResolver) throws IOException {
    RuntimeMode runtimeMode = kubernetesExtension.getRuntimeMode();
    final Properties properties = kubernetesExtension.javaProject.getProperties();
    if (!properties.contains(DOCKER_IMAGE_USER)) {
      String namespaceToBeUsed = kubernetesExtension.getNamespace().getOrElse(clusterAccess.getNamespace());
      kitLogger.info("Using docker image name of namespace: " + namespaceToBeUsed);
      properties.setProperty(DOCKER_IMAGE_USER, namespaceToBeUsed);
    }
    if (!properties.contains(RuntimeMode.JKUBE_EFFECTIVE_PLATFORM_MODE)) {
      properties.setProperty(RuntimeMode.JKUBE_EFFECTIVE_PLATFORM_MODE, runtimeMode.toString());
    }
    return super.resolveImages(imageConfigResolver);
  }
}
