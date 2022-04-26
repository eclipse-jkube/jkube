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
package org.eclipse.jkube.kit.resource.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.validation.ConstraintViolationException;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;
import org.eclipse.jkube.kit.common.util.ValidationUtil;
import org.eclipse.jkube.kit.config.resource.EnricherManager;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.ResourceService;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.eclipse.jkube.kit.profile.Profile;
import org.eclipse.jkube.kit.profile.ProfileUtil;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.listResourceFragments;
import static org.eclipse.jkube.kit.resource.service.TemplateUtil.interpolateTemplateVariables;
import static org.eclipse.jkube.kit.resource.service.WriteUtil.writeResourcesIndividualAndComposite;

public class DefaultResourceService implements ResourceService {

  private final ResourceServiceConfig resourceServiceConfig;

  public DefaultResourceService(ResourceServiceConfig resourceServiceConfig) {
    this.resourceServiceConfig = resourceServiceConfig;
  }

  @Override
  public KubernetesList generateResources(PlatformMode platformMode, EnricherManager enricherManager, KitLogger log)
      throws IOException {

    // Generate all resources from the main resource directory, configuration and create them accordingly
    return generateAppResources(platformMode, enricherManager, log)
        .addAllToItems(generateProfiledResourcesFromSubdirectories(platformMode, enricherManager))
        .build();
  }

  @Override
  public File writeResources(KubernetesList resources, ResourceClassifier classifier, KitLogger log) throws IOException {
    final File targetDir = resourceServiceConfig.getTargetDir();
    final ResourceFileType resourceFileType = resourceServiceConfig.getResourceFileType();
    // write kubernetes.yml / openshift.yml
    File resourceFileBase = new File(targetDir, classifier.getValue());

    File file = writeResourcesIndividualAndComposite(resources, resourceFileBase, resourceFileType, log);
    // Resolve template placeholders
    if (resourceServiceConfig.isInterpolateTemplateParameters()) {
      interpolateTemplateVariables(resources, file);
    }

    return file;
  }

  private KubernetesListBuilder generateAppResources(PlatformMode platformMode, EnricherManager enricherManager, KitLogger log)
      throws IOException {

    final ResourceConfig resourceConfig = resourceServiceConfig.getResourceConfig();
    try {
      File[] resourceFiles = aggregateResourceFragments(resourceServiceConfig.getResourceDirs(), resourceConfig, log);
      final File[] processedResource = processResourceFiles(resourceFiles);
      KubernetesListBuilder builder = processResourceFragments(platformMode, processedResource);

      // Create default resources for app resources only
      enricherManager.createDefaultResources(platformMode, builder);

      // Enrich descriptors
      enricherManager.enrich(platformMode, builder);

      return builder;
    } catch (ConstraintViolationException e) {
      String message = ValidationUtil.createValidationMessage(e.getConstraintViolations());
      log.error("ConstraintViolationException: %s", message);
      throw new IOException(message, e);
    }
  }

  private KubernetesListBuilder processResourceFragments(PlatformMode platformMode, File[] resourceFiles) throws IOException {
    final KubernetesListBuilder builder = new KubernetesListBuilder();
    // Add resource files found in the JKube directory
    if (resourceFiles != null && resourceFiles.length > 0) {
      builder.addAllToItems(readResourceFragments(platformMode, resourceFiles).buildItems());
    }
    return builder;
  }

  private List<HasMetadata> generateProfiledResourcesFromSubdirectories(
      PlatformMode platformMode, EnricherManager enricherManager) throws IOException {

    final List<HasMetadata> ret = new ArrayList<>();
    final List<File> resourceDirs = resourceServiceConfig.getResourceDirs();
    for (File resourceDir : resourceDirs) {
      File[] profileDirs = resourceDir.listFiles(File::isDirectory);
      if (profileDirs != null) {
        for (File profileDir : profileDirs) {
          Profile foundProfile = ProfileUtil.findProfile(profileDir.getName(), resourceDir);
          ProcessorConfig enricherConfig = foundProfile.getEnricherConfig();
          File[] resourceFiles = listResourceFragments(profileDir);
          final File[] processedResources = processResourceFiles(resourceFiles);
          if (processedResources.length > 0) {
            KubernetesListBuilder profileBuilder = readResourceFragments(platformMode, processedResources);
            enricherManager.createDefaultResources(platformMode, enricherConfig, profileBuilder);
            enricherManager.enrich(platformMode, enricherConfig, profileBuilder);
            ret.addAll(profileBuilder.buildItems());
          }
        }
      }
    }
    return ret;
  }

  private KubernetesListBuilder readResourceFragments(PlatformMode platformMode, File[] resourceFiles) throws IOException {
    return KubernetesResourceUtil.readResourceFragmentsFrom(
        platformMode,
        KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING,
        JKubeProjectUtil.createDefaultResourceName(resourceServiceConfig.getProject().getArtifactId()),
        resourceFiles);
  }

  private File[] processResourceFiles(File[] resourceFiles) throws IOException {
    if (resourceServiceConfig.getResourceFilesProcessor() != null) {
      return resourceServiceConfig.getResourceFilesProcessor().processResources(resourceFiles);
    }
    return resourceFiles;
  }

  private File[] aggregateResourceFragments(List<File> resourceDirs, ResourceConfig resourceConfig, KitLogger log) {
    List<File> fragments = new ArrayList<>();
    for (File resourceDir : resourceDirs) {
      log.info("Using resource templates from %s", resourceDir);
      File[] resourceFiles = listResourceFragments(resourceDir, resourceConfig !=null ? resourceConfig.getRemotes() : null, log);
      if (resourceFiles != null && resourceFiles.length > 0) {
        fragments.addAll(Arrays.asList(resourceFiles));
      }
    }
    return fragments.toArray(new File[0]);
  }

}
