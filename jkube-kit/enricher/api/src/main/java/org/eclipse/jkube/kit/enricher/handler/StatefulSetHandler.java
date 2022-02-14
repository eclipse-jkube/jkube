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
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpecBuilder;

/**
 * Handler for StatefulSets
 *
 * @author matthew on 26/10/16.
 */
public class StatefulSetHandler implements ControllerHandler<StatefulSet> {

  private final PodTemplateHandler podTemplateHandler;

  StatefulSetHandler(PodTemplateHandler podTemplateHandler) {
    this.podTemplateHandler = podTemplateHandler;
  }

  @Override
  public StatefulSet get(ResourceConfig config, List<ImageConfiguration> images) {
    return new StatefulSetBuilder()
        .withMetadata(createStatefulSetMetaData(config))
        .withSpec(createStatefulSetSpec(config, images))
        .build();
  }

  @Override
  public PodTemplateSpec getPodTemplateSpec(ResourceConfig config, List<ImageConfiguration> images) {
    return get(config, images).getSpec().getTemplate();
  }

  @Override
  public void overrideReplicas(KubernetesListBuilder resources, int replicas) {
    resources.accept(new TypedVisitor<StatefulSetBuilder>() {
      @Override
      public void visit(StatefulSetBuilder builder) {
        builder.editOrNewSpec().withReplicas(replicas).endSpec();
      }
    });
  }

  private ObjectMeta createStatefulSetMetaData(ResourceConfig config) {
    return new ObjectMetaBuilder()
        .withName(KubernetesHelper.validateKubernetesId(config.getControllerName(), "controller name"))
        .build();
  }

  private StatefulSetSpec createStatefulSetSpec(ResourceConfig config, List<ImageConfiguration> images) {
    return new StatefulSetSpecBuilder()
        .withReplicas(config.getReplicas())
        .withServiceName(config.getControllerName())
        .withTemplate(podTemplateHandler.getPodTemplate(config, config.getRestartPolicy(), images))
        .build();
  }
}
