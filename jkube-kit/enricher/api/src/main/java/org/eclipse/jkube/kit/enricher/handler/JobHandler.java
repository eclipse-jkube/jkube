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

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpecBuilder;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import java.util.List;
import java.util.Optional;

/**
 * Handler for Jobs
 *
 * @author matthew on 11/02/17.
 */
public class JobHandler implements ControllerHandler<Job> {

  private final PodTemplateHandler podTemplateHandler;
  private static final String DEFAULT_JOB_RESTART_POLICY = "OnFailure";

  JobHandler(PodTemplateHandler podTemplateHandler) {
    this.podTemplateHandler = podTemplateHandler;
  }

  @Override
  public Job get(ControllerResourceConfig config, List<ImageConfiguration> images) {
    return new JobBuilder()
        .withMetadata(createJobSpecMetaData(config))
        .withSpec(createJobSpec(config, images))
        .build();
  }

  @Override
  public PodTemplateSpec getPodTemplateSpec(ControllerResourceConfig config, List<ImageConfiguration> images) {
    return get(config, images).getSpec().getTemplate();
  }

  @Override
  public PodTemplateSpec getPodTemplate(Job controller) {
    return controller.getSpec().getTemplate();
  }

  @Override
  public void overrideReplicas(KubernetesListBuilder resources, int replicas) {
    // NOOP
  }

  private ObjectMeta createJobSpecMetaData(ControllerResourceConfig config) {
    return new ObjectMetaBuilder()
        .withName(KubernetesHelper.validateKubernetesId(config.getControllerName(), "controller name"))
        .build();
  }

  private JobSpec createJobSpec(ControllerResourceConfig config, List<ImageConfiguration> images) {
    return new JobSpecBuilder()
        .withTemplate(podTemplateHandler.getPodTemplate(config, Optional.ofNullable(config.getRestartPolicy()).orElse(DEFAULT_JOB_RESTART_POLICY), images))
        .build();
  }
}
