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
import javax.validation.ConstraintViolationException;

import io.fabric8.kubernetes.api.model.KubernetesList;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.kit.common.util.LazyBuilder;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.config.service.ResourceServiceConfig;
import org.eclipse.jkube.kit.resource.service.DefaultResourceService;

import java.io.File;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;
import org.eclipse.jkube.kit.common.util.validator.ResourceValidator;

import java.io.IOException;

import static org.eclipse.jkube.kit.common.util.DekorateUtil.DEFAULT_RESOURCE_LOCATION;
import static org.eclipse.jkube.kit.common.util.DekorateUtil.useDekorate;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil.updateKindFilenameMappings;

@SuppressWarnings("CdiInjectionPointsInspection")
public class KubernetesResourceTask extends AbstractJKubeTask {


  @Inject
  public KubernetesResourceTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
    setDescription("Generates cluster resource configuration manifests.");
  }

  @Override
  protected JKubeServiceHub.JKubeServiceHubBuilder initJKubeServiceHubBuilder() {
    JKubeServiceHub.JKubeServiceHubBuilder builder = super.initJKubeServiceHubBuilder();
    ResourceConfig resourceConfig = kubernetesExtension.resources;
    if (kubernetesExtension.getNamespaceOrNull() != null) {
      resourceConfig = ResourceConfig.toBuilder(resourceConfig).namespace(kubernetesExtension.getNamespaceOrNull()).build();
    }
    final ResourceServiceConfig resourceServiceConfig = ResourceServiceConfig.builder()
        .project(kubernetesExtension.javaProject)
        .resourceDir(resolveResourceSourceDirectory())
        .targetDir(kubernetesExtension.getResourceTargetDirectoryOrDefault())
        .resourceFileType(kubernetesExtension.getResourceFileTypeOrDefault())
        .resourceConfig(resourceConfig)
        .interpolateTemplateParameters(kubernetesExtension.getInterpolateTemplateParametersOrDefault())
        .build();
    builder.resourceService(new LazyBuilder<>(() -> new DefaultResourceService(resourceServiceConfig)));

    return builder;
  }

  @Override
  public void run() {
    if (useDekorate(kubernetesExtension.javaProject)
        && Boolean.TRUE.equals(kubernetesExtension.getMergeWithDekorateOrDefault())) {
      kitLogger.info("Dekorate detected, merging JKube and Dekorate resources");
      System.setProperty("dekorate.input.dir", DEFAULT_RESOURCE_LOCATION);
      System.setProperty("dekorate.output.dir", DEFAULT_RESOURCE_LOCATION);
    } else if (useDekorate(kubernetesExtension.javaProject)) {
      kitLogger.info("Dekorate detected, delegating resource build");
      System.setProperty("dekorate.output.dir", DEFAULT_RESOURCE_LOCATION);
      return;
    }

    updateKindFilenameMappings(kubernetesExtension.mappings);
    try {
      jKubeServiceHub.setPlatformMode(kubernetesExtension.getRuntimeMode());
      if (Boolean.FALSE.equals(kubernetesExtension.getSkipOrDefault())) {
        ResourceClassifier resourceClassifier = kubernetesExtension.getResourceClassifier();
        KubernetesList resourceList =  jKubeServiceHub.getResourceService().generateResources(kubernetesExtension.getPlatformMode(), enricherManager, kitLogger);
        final File resourceClassifierDir = new File(kubernetesExtension.getResourceTargetDirectoryOrDefault(), resourceClassifier.getValue());
        jKubeServiceHub.getResourceService().writeResources(resourceList, resourceClassifier, kitLogger);
        validateIfRequired(resourceClassifierDir, resourceClassifier);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to generate kubernetes descriptor", e);
    }
  }

  private void validateIfRequired(File resourceDir, ResourceClassifier classifier) {
    try {
      if (Boolean.FALSE.equals(kubernetesExtension.getSkipResourceValidationOrDefault())) {
        new ResourceValidator(resourceDir, classifier, kitLogger).validate();
      }
    } catch (ConstraintViolationException e) {
      if (Boolean.TRUE.equals(kubernetesExtension.getFailOnValidationErrorOrDefault())) {
        kitLogger.error(e.getMessage());
        kitLogger.error("Set skipResourceValidation=true option to skip the validation");
        throw new IllegalStateException("Failed to generate kubernetes descriptor");
      } else {
        kitLogger.warn(e.getMessage());
      }
    } catch (Exception e) {
      if (Boolean.TRUE.equals(kubernetesExtension.getFailOnValidationErrorOrDefault())) {
        throw new IllegalStateException("Failed to validate resources", e);
      } else {
        kitLogger.warn("Failed to validate resources: %s", e.getMessage());
      }
    }
  }
}
