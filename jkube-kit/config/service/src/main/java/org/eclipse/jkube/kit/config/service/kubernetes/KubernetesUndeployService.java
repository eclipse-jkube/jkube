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
package org.eclipse.jkube.kit.config.service.kubernetes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.common.GenericCustomResource;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.config.service.UndeployService;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionList;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getCrdContext;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.loadResources;
import static org.eclipse.jkube.kit.config.service.ApplyService.getK8sListWithNamespaceFirst;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.applicableNamespace;

public class KubernetesUndeployService implements UndeployService {

  private final JKubeServiceHub jKubeServiceHub;
  private final KitLogger logger;
  private static final Predicate<HasMetadata> isCustomResource = item -> item instanceof GenericCustomResource;

  public KubernetesUndeployService(JKubeServiceHub jKubeServiceHub, KitLogger logger) {
    this.jKubeServiceHub = jKubeServiceHub;
    this.logger = logger;
  }

  @Override
  public void undeploy(String fallbackNamespace, File resourceDir, ResourceConfig resourceConfig, File... manifestFiles) throws IOException {
    final List<File> manifests = Stream.of(manifestFiles)
        .filter(Objects::nonNull).filter(File::exists).filter(File::isFile)
        .collect(Collectors.toList());
    List<HasMetadata> entities = new ArrayList<>();
    for (File manifest : manifests) {
      entities.addAll(loadResources(manifest));
    }
    if (entities.isEmpty()) {
      logger.warn("No such generated manifests found for this project, ignoring.");
      return;
    }
    List<HasMetadata> undeployEntities = getK8sListWithNamespaceFirst(entities);
    Collections.reverse(undeployEntities);
    undeployCustomResources(resourceConfig.getNamespace(), fallbackNamespace, undeployEntities);
    undeployResources(resourceConfig.getNamespace(), fallbackNamespace, undeployEntities);
  }

  private void undeployCustomResources(String currentNamespace, String fallbackNamespace, List<HasMetadata> entities) {
    final Consumer<HasMetadata> customResourceDeleter = customResourceDeleter(currentNamespace, fallbackNamespace);
    entities.stream().filter(isCustomResource).forEach(customResourceDeleter);
  }

  private void undeployResources(String namespace, String fallbackNamespace, List<HasMetadata> entities) {
    final Consumer<HasMetadata> resourceDeleter = resourceDeleter(namespace, fallbackNamespace);
    entities.stream().filter(isCustomResource.negate()).forEach(resourceDeleter);
  }

  protected Consumer<HasMetadata> resourceDeleter(String namespace, String fallbackNamespace) {
    return resource -> {
      String undeployNamespace = applicableNamespace(resource, namespace, fallbackNamespace);
      logger.info("Deleting resource %s %s/%s", KubernetesHelper.getKind(resource), undeployNamespace, KubernetesHelper.getName(resource));
      jKubeServiceHub.getClient().resource(resource)
          .inNamespace(undeployNamespace)
          .withPropagationPolicy(DeletionPropagation.BACKGROUND)
          .delete();
    };
  }

  protected Consumer<HasMetadata> customResourceDeleter(String namespace, String fallbackNamespace) {
    return customResource -> {
      String undeployNamespace = applicableNamespace(customResource, namespace, fallbackNamespace);
      GenericCustomResource genericCustomResource = (GenericCustomResource) customResource;
      CustomResourceDefinitionList crdList = jKubeServiceHub.getClient().apiextensions().v1beta1().customResourceDefinitions().list();
      deleteCustomResourceIfCustomResourceDefinitionContextPresent(genericCustomResource, undeployNamespace, getCrdContext(crdList, genericCustomResource));
    };
  }

  private void deleteCustomResourceIfCustomResourceDefinitionContextPresent(GenericCustomResource customResource, String namespace, CustomResourceDefinitionContext crdContext) {
    if (crdContext != null) {
        deleteCustomResourceIfPresent(customResource, namespace, crdContext);
    }
  }

  private void deleteCustomResourceIfPresent(GenericCustomResource customResource, String namespace, CustomResourceDefinitionContext crdContext) {
    final GenericCustomResource cr = KubernetesClientUtil.doGetCustomResource(jKubeServiceHub.getClient(), crdContext, namespace, customResource.getMetadata().getName());
    if (cr != null) {
      deleteCustomResource(customResource, namespace, crdContext);
    }
  }

  private void deleteCustomResource(GenericCustomResource customResource, String namespace, CustomResourceDefinitionContext crdContext) {
    String name = customResource.getMetadata().getName();
    String apiVersionAndKind = KubernetesHelper.getFullyQualifiedApiGroupWithKind(crdContext);
    try {
      logger.info("Deleting Custom Resource %s %s", apiVersionAndKind, name);
      jKubeServiceHub.getClient().customResource(crdContext).inNamespace(namespace).withName(name).delete();
    } catch (Exception exception) {
      logger.error("Unable to undeploy %s %s/%s", apiVersionAndKind, namespace, name);
    }
  }

  protected JKubeServiceHub getjKubeServiceHub() {
    return jKubeServiceHub;
  }

  protected KitLogger getLogger() {
    return logger;
  }
}
