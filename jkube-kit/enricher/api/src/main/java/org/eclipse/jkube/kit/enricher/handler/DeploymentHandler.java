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
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;

public class DeploymentHandler implements ControllerHandler<Deployment> {
  private final PodTemplateHandler podTemplateHandler;

  DeploymentHandler(PodTemplateHandler podTemplateHandler) {
    this.podTemplateHandler = podTemplateHandler;
  }

  @Override
  public Deployment get(ControllerResourceConfig config, List<ImageConfiguration> images) {
    return new DeploymentBuilder()
        .withMetadata(createDeploymentMetaData(config))
        .withSpec(createDeploymentSpec(config, images))
        .build();
  }

  @Override
  public PodTemplateSpec getPodTemplateSpec(ControllerResourceConfig config, List<ImageConfiguration> images) {
    return get(config, images).getSpec().getTemplate();
  }

  @Override
  public PodTemplateSpec getPodTemplate(Deployment controller) {
    return controller.getSpec().getTemplate();
  }

  @Override
  public void overrideReplicas(KubernetesListBuilder resources, int replicas) {
    resources.accept(new TypedVisitor<DeploymentBuilder>() {
      @Override
      public void visit(DeploymentBuilder builder) {
        builder.editOrNewSpec().withReplicas(replicas).endSpec();
      }
    });
  }

  private ObjectMeta createDeploymentMetaData(ControllerResourceConfig config) {
    return new ObjectMetaBuilder()
        .withName(KubernetesHelper.validateKubernetesId(config.getControllerName(), "controller name"))
        .build();
  }

  private DeploymentSpec createDeploymentSpec(ControllerResourceConfig config, List<ImageConfiguration> images) {
    return new DeploymentSpecBuilder()
        .withReplicas(config.getReplicas())
        .withTemplate(podTemplateHandler.getPodTemplate(config, config.getRestartPolicy(), images))
        .build();
  }
}
