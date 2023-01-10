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
import java.util.Optional;

import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobSpecBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpecBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobTemplateSpecBuilder;

public class CronJobHandler implements ControllerHandler<CronJob> {
  private static final String DEFAULT_JOB_RESTART_POLICY = "OnFailure";
  private final PodTemplateHandler podTemplateHandler;

  public CronJobHandler(PodTemplateHandler podTemplateHandler) {
    this.podTemplateHandler = podTemplateHandler;
  }

  @Override
  public CronJob get(ControllerResourceConfig config, List<ImageConfiguration> images) {
    return new CronJobBuilder()
        .withMetadata(createCronJobMetadata(config))
        .withSpec(createCronJobSpec(config, images))
        .build();
  }

  @Override
  public PodTemplateSpec getPodTemplateSpec(ControllerResourceConfig config, List<ImageConfiguration> images) {
    return get(config, images).getSpec().getJobTemplate().getSpec().getTemplate();
  }

  @Override
  public PodTemplateSpec getPodTemplate(CronJob controller) {
    return controller.getSpec().getJobTemplate().getSpec().getTemplate();
  }

  @Override
  public void overrideReplicas(KubernetesListBuilder resources, int replicas) {
    // NOOP
  }

  private ObjectMeta createCronJobMetadata(ControllerResourceConfig config) {
    return new ObjectMetaBuilder()
        .withName(KubernetesHelper.validateKubernetesId(config.getControllerName(), "controller name"))
        .build();
  }

  private CronJobSpec createCronJobSpec(ControllerResourceConfig config, List<ImageConfiguration> images) {
    return new CronJobSpecBuilder()
        .withSchedule(KubernetesHelper.validateCronJobSchedule(config.getSchedule()))
        .withJobTemplate(createJobTemplateSpec(config, images))
        .build();
  }

  private JobTemplateSpec createJobTemplateSpec(ControllerResourceConfig config, List<ImageConfiguration> images) {
    return new JobTemplateSpecBuilder()
        .withSpec(createJobSpec(config, images))
        .build();
  }

  private JobSpec createJobSpec(ControllerResourceConfig config, List<ImageConfiguration> images) {
    return new JobSpecBuilder()
        .withTemplate(podTemplateHandler.getPodTemplate(config,
            Optional.ofNullable(config.getRestartPolicy()).orElse(DEFAULT_JOB_RESTART_POLICY), images))
        .build();
  }

}