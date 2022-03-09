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
package org.eclipse.jkube.kit.enricher.handler;

import java.util.List;

import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import io.fabric8.openshift.api.model.DeploymentConfigSpecBuilder;

public class DeploymentConfigHandler implements ControllerHandler<DeploymentConfig> {
  private final PodTemplateHandler podTemplateHandler;

  DeploymentConfigHandler(PodTemplateHandler podTemplateHandler) {
    this.podTemplateHandler = podTemplateHandler;
  }

  @Override
  public DeploymentConfig get(ResourceConfig config, List<ImageConfiguration> images) {
    return new DeploymentConfigBuilder()
        .withMetadata(createMetaData(config))
        .withSpec(createDeploymentConfigSpec(config, images))
        .build();
  }

  @Override
  public PodTemplateSpec getPodTemplateSpec(ResourceConfig config, List<ImageConfiguration> images) {
    return get(config, images).getSpec().getTemplate();
  }

  @Override
  public PodTemplateSpec getPodTemplate(DeploymentConfig controller) {
    return controller.getSpec().getTemplate();
  }

  @Override
  public void overrideReplicas(KubernetesListBuilder resources, int replicas) {
    resources.accept(new TypedVisitor<DeploymentConfigBuilder>() {
      @Override
      public void visit(DeploymentConfigBuilder builder) {
        builder.editOrNewSpec().withReplicas(replicas).endSpec();
      }
    });
  }

  private ObjectMeta createMetaData(ResourceConfig config) {
    return new ObjectMetaBuilder()
        .withName(KubernetesHelper.validateKubernetesId(config.getControllerName(), "controller name"))
        .build();
  }

  private DeploymentConfigSpec createDeploymentConfigSpec(ResourceConfig config, List<ImageConfiguration> images) {
    return new DeploymentConfigSpecBuilder()
        .withReplicas(config.getReplicas())
        .withTemplate(podTemplateHandler.getPodTemplate(config, config.getRestartPolicy(), images))
        .build();
  }
}
