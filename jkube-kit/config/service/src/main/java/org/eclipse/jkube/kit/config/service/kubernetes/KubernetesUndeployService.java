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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import static org.eclipse.jkube.kit.common.util.KubernetesHelper.getCustomResourcesFileToNameMap;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.loadResources;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.unmarshalCustomResourceFile;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.getCustomResourceDefinitionContext;

public class KubernetesUndeployService implements UndeployService {

  private final JKubeServiceHub jKubeServiceHub;
  private final KitLogger logger;

  public KubernetesUndeployService(JKubeServiceHub jKubeServiceHub, KitLogger logger) {
    this.jKubeServiceHub = jKubeServiceHub;
    this.logger = logger;
  }

  @Override
  public void undeploy(File resourceDir, ResourceConfig resourceConfig, File... manifestFiles) throws IOException {
    final List<File> manifests = Stream.of(manifestFiles)
        .filter(Objects::nonNull).filter(File::exists).filter(File::isFile)
        .collect(Collectors.toList());
    final List<HasMetadata> entities = new ArrayList<>();
    for (File manifest : manifests) {
      entities.addAll(loadResources(manifest));
    }
    if (entities.isEmpty()) {
      logger.warn("No such generated manifests found for this project, ignoring.");
      return;
    }
    Collections.reverse(entities);
    final String currentNamespace = currentNamespace(entities);
    undeployResources(currentNamespace, entities);
    undeployCustomResources(currentNamespace, resourceDir, resourceConfig);
  }

  private void undeployResources(String namespace, List<HasMetadata> entities) {
    final Consumer<HasMetadata> resourceDeleter = resourceDeleter(namespace);
    entities.stream()
        .filter(e -> !(e instanceof Namespace))
        .filter(e -> !(e instanceof Project))
        .forEach(resourceDeleter);
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

  private void undeployCustomResources(String namespace, File resourceDir, ResourceConfig resourceConfig) throws IOException {
    if (resourceConfig == null || resourceConfig.getCustomResourceDefinitions() == null || resourceConfig.getCustomResourceDefinitions().isEmpty()) {
      return;
    }

    List<CustomResourceDefinitionContext> crdContexts = getCustomResourceDefinitionContext(jKubeServiceHub.getClient(),resourceConfig.getCustomResourceDefinitions());
    Map<File, String> fileToCrdMap = getCustomResourcesFileToNameMap(resourceDir, resourceConfig.getRemotes(), logger);

    for(CustomResourceDefinitionContext customResourceDefinitionContext : crdContexts) {
      for(Map.Entry<File, String> entry : fileToCrdMap.entrySet()) {
        if(entry.getValue().equals(customResourceDefinitionContext.getGroup())) {
            deleteCustomResource(entry.getKey(), namespace, customResourceDefinitionContext);
        }
      }
    }
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

  private void deleteCustomResource(File customResourceFile, String namespace, CustomResourceDefinitionContext crdContext)
      throws IOException {

    Map<String, Object> customResource = unmarshalCustomResourceFile(customResourceFile);
    Map<String, Object> objectMeta = (Map<String, Object>)customResource.get("metadata");
    String name = objectMeta.get("name").toString();
    logger.info("Deleting Custom Resource %s", name);
    KubernetesClientUtil.doDeleteCustomResource(jKubeServiceHub.getClient(), crdContext, namespace, name);
  }

  protected JKubeServiceHub getjKubeServiceHub() {
    return jKubeServiceHub;
  }

  protected KitLogger getLogger() {
    return logger;
  }
}
