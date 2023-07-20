/*
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
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpecBuilder;

/**
 * @author roland
 */
public class ReplicationControllerHandler implements ControllerHandler<ReplicationController> {

  private final PodTemplateHandler podTemplateHandler;

  ReplicationControllerHandler(PodTemplateHandler podTemplateHandler) {
    this.podTemplateHandler = podTemplateHandler;
  }

  @Override
  public ReplicationController get(ControllerResourceConfig config, List<ImageConfiguration> images) {
    return new ReplicationControllerBuilder()
        .withMetadata(createRcMetaData(config))
        .withSpec(createRcSpec(config, images))
        .build();
  }

  @Override
  public PodTemplateSpec getPodTemplateSpec(ControllerResourceConfig config, List<ImageConfiguration> images) {
    return get(config, images).getSpec().getTemplate();
  }

  @Override
  public PodTemplateSpec getPodTemplate(ReplicationController controller) {
    return controller.getSpec().getTemplate();
  }

  @Override
  public void overrideReplicas(KubernetesListBuilder resources, int replicas) {
    resources.accept(new TypedVisitor<ReplicationControllerBuilder>() {
      @Override
      public void visit(ReplicationControllerBuilder builder) {
        builder.editOrNewSpec().withReplicas(replicas).endSpec();
      }
    });
  }
// ===========================================================
  // TODO: "replica set" config used

  private ObjectMeta createRcMetaData(ControllerResourceConfig config) {
    return new ObjectMetaBuilder()
        .withName(KubernetesHelper.validateKubernetesId(config.getControllerName(), "replication controller name"))
        .build();
  }

  private ReplicationControllerSpec createRcSpec(ControllerResourceConfig config, List<ImageConfiguration> images) {
    return new ReplicationControllerSpecBuilder()
        .withReplicas(config.getReplicas())
        .withTemplate(podTemplateHandler.getPodTemplate(config, config.getRestartPolicy(), images))
        .build();
  }
}
