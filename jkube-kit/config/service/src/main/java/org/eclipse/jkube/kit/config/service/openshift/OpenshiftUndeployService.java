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
package org.eclipse.jkube.kit.config.service.openshift;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.config.service.kubernetes.KubernetesUndeployService;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import io.fabric8.openshift.api.model.DeploymentTriggerImageChangeParams;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicy;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.commons.lang3.StringUtils;

import static org.eclipse.jkube.kit.common.util.OpenshiftHelper.asOpenShiftClient;

public class OpenshiftUndeployService extends KubernetesUndeployService {

  public OpenshiftUndeployService(JKubeServiceHub jKubeServiceHub, KitLogger logger) {
    super(jKubeServiceHub, logger);
  }

  @Override
  protected Consumer<HasMetadata> resourceDeleter(String namespace, String fallbackNamespace) {
    final Consumer<HasMetadata> standardDeleter = super.resourceDeleter(namespace, fallbackNamespace);
    final OpenShiftClient oc = asOpenShiftClient(getjKubeServiceHub().getClient());
    return entity -> {
      final List<String> isTags = imageStreamTags(entity);
      if (oc != null && !isTags.isEmpty()) {
        for (String isTag  : isTags) {
          oc.builds().inNamespace(namespace).list().getItems().stream()
              .filter(labelMatcherFilter(entity))
              .filter(bc -> Objects.equals(bc.getSpec().getOutput().getTo().getName(), isTag))
              .forEach(standardDeleter);
          oc.buildConfigs().inNamespace(namespace).list().getItems().stream()
              .filter(labelMatcherFilter(entity))
              .filter(bc -> Objects.equals(bc.getSpec().getOutput().getTo().getName(), isTag))
              .forEach(standardDeleter);
        }
      }
      standardDeleter.accept(entity);
    };
  }

  private Predicate<HasMetadata> labelMatcherFilter(HasMetadata originalEntity) {
    return toFilter -> {
      final String originalEntityProvider = providerLabel(originalEntity);
      if (StringUtils.isNotBlank(originalEntityProvider)) {
        return originalEntityProvider.equals(providerLabel(toFilter));
      }
      return true;
    };
  }

  private static String providerLabel(HasMetadata entity) {
    return Optional.ofNullable(entity)
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getLabels)
        .map(labels -> labels.get("provider"))
        .orElse(null);
  }

  private List<String> imageStreamTags(HasMetadata entity) {
    if (entity instanceof ImageStream) {
      final ImageStream is = (ImageStream)entity;
      getLogger().info("ImageStream %s found, deleting related builds and build configs",
          is.getMetadata().getName());
      return is.getSpec().getTags().stream()
          .map(tr -> String.format("%s:%s", is.getMetadata().getName(), tr.getName()))
          .collect(Collectors.toList());
    } else if (entity instanceof DeploymentConfig) {
      final DeploymentConfig dc = (DeploymentConfig)entity;
      getLogger().info("DeploymentConfig %s found, deleting related builds and build configs",
          dc.getMetadata().getName());
      return Optional.ofNullable(dc.getSpec()).map(DeploymentConfigSpec::getTriggers).orElse(Collections.emptyList())
          .stream().filter(t -> t.getType().equalsIgnoreCase("ImageChange"))
          .map(DeploymentTriggerPolicy::getImageChangeParams)
          .map(DeploymentTriggerImageChangeParams::getFrom)
          .filter(or -> or.getKind().equalsIgnoreCase("ImageStreamTag"))
          .map(ObjectReference::getName)
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }
}
