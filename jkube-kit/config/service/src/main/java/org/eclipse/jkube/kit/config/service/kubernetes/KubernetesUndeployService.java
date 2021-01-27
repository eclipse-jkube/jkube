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
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionList;
import org.eclipse.jkube.kit.common.GenericCustomResource;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.config.service.UndeployService;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.openshift.api.model.Project;

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getCrdContext;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.loadResources;
import static org.eclipse.jkube.kit.config.service.ApplyService.getK8sListWithNamespaceFirst;

public class KubernetesUndeployService implements UndeployService {

  private final JKubeServiceHub jKubeServiceHub;
  private final KitLogger logger;
  private static final Predicate<HasMetadata> isCustomResource = item -> item instanceof GenericCustomResource;

  public KubernetesUndeployService(JKubeServiceHub jKubeServiceHub, KitLogger logger) {
    this.jKubeServiceHub = jKubeServiceHub;
    this.logger = logger;
  }

  @Override
  public void undeploy(File resourceDir, ResourceConfig resourceConfig, File... manifestFiles) throws IOException {
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
    final String currentNamespace = currentNamespace(undeployEntities);
    undeployCustomResources(currentNamespace, undeployEntities);
    undeployResources(currentNamespace, undeployEntities);
  }

  private void undeployCustomResources(String currentNamespace, List<HasMetadata> entities) {
    final Consumer<HasMetadata> customResourceDeleter = customResourceDeleter(currentNamespace);
    entities.stream().filter(isCustomResource).forEach(customResourceDeleter);
  }

  private void undeployResources(String namespace, List<HasMetadata> entities) {
    final Consumer<HasMetadata> resourceDeleter = resourceDeleter(namespace);
    entities.stream().filter(isCustomResource.negate()).forEach(resourceDeleter);
  }

  protected Consumer<HasMetadata> resourceDeleter(String namespace) {
    return resource -> {
      logger.info("Deleting resource %s %s/%s", KubernetesHelper.getKind(resource), namespace, KubernetesHelper.getName(resource));
      jKubeServiceHub.getClient().resource(resource)
          .inNamespace(namespace)
          .withPropagationPolicy(DeletionPropagation.BACKGROUND)
          .delete();
    };
  }

  protected Consumer<HasMetadata> customResourceDeleter(String namespace) {
    return customResource -> {
      GenericCustomResource genericCustomResource = (GenericCustomResource) customResource;
      CustomResourceDefinitionList crdList = jKubeServiceHub.getClient().apiextensions().v1beta1().customResourceDefinitions().list();
      deleteCustomResourceIfCustomResourceDefinitionContextPresent(genericCustomResource, namespace, getCrdContext(crdList, genericCustomResource));
    };
  }

  // Visible for testing
  String currentNamespace(List<HasMetadata> entities) {
    for (HasMetadata entity : entities) {
      if (entity instanceof Namespace || entity instanceof Project) {
        return entity.getMetadata().getName();
      }
    }
    return jKubeServiceHub.getClusterAccess().getNamespace();
  }

  private void deleteCustomResourceIfCustomResourceDefinitionContextPresent(GenericCustomResource customResource, String namespace, CustomResourceDefinitionContext crdContext) {
    if (crdContext != null) {
        deleteCustomResourceIfPresent(customResource, namespace, crdContext);
    }
  }

  private void deleteCustomResourceIfPresent(GenericCustomResource customResource, String namespace, CustomResourceDefinitionContext crdContext) {
    Map<String, Object> cr = KubernetesClientUtil.doGetCustomResource(jKubeServiceHub.getClient(), crdContext, namespace, customResource.getMetadata().getName());
    if (cr != null) {
      deleteCustomResource(customResource, namespace, crdContext);
    }
  }

  private void deleteCustomResource(GenericCustomResource customResource, String namespace, CustomResourceDefinitionContext crdContext) {
    String name = customResource.getMetadata().getName();
    String apiVersionAndKind = KubernetesHelper.getFullyQualifiedApiGroupWithKind(crdContext);
    try {
      logger.info("Deleting Custom Resource %s %s", apiVersionAndKind, name);
      KubernetesClientUtil.doDeleteCustomResource(jKubeServiceHub.getClient(), crdContext, namespace, name);
    } catch (IOException exception) {
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
