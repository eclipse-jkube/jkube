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

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetSpec;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetSpecBuilder;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import java.util.List;

/**
 * @author roland
 */
public class ReplicaSetHandler implements ControllerHandler<ReplicaSet> {

  private final PodTemplateHandler podTemplateHandler;

  ReplicaSetHandler(PodTemplateHandler podTemplateHandler) {
    this.podTemplateHandler = podTemplateHandler;
  }

  @Override
  public ReplicaSet get(ResourceConfig config, List<ImageConfiguration> images) {
    return new ReplicaSetBuilder()
        .withMetadata(createRsMetaData(config))
        .withSpec(createRsSpec(config, images))
        .build();
  }

  @Override
  public PodTemplateSpec getPodTemplateSpec(ResourceConfig config, List<ImageConfiguration> images) {
    return get(config, images).getSpec().getTemplate();
  }

  private ObjectMeta createRsMetaData(ResourceConfig config) {
    return new ObjectMetaBuilder()
        .withName(KubernetesHelper.validateKubernetesId(config.getControllerName(), "controller name"))
        .build();
  }

  private ReplicaSetSpec createRsSpec(ResourceConfig config, List<ImageConfiguration> images) {
    return new ReplicaSetSpecBuilder()
        .withReplicas(config.getReplicas())
        .withTemplate(podTemplateHandler.getPodTemplate(config, images))
        .build();
  }
}
