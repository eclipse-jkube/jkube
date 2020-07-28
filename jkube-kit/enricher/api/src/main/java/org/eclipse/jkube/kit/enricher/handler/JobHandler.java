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
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.JobSpec;
import io.fabric8.kubernetes.api.model.batch.JobSpecBuilder;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;

import java.util.List;

/**
 * Handler for Jobs
 *
 * @author matthew on 11/02/17.
 */
public class JobHandler {
    private final PodTemplateHandler podTemplateHandler;

    JobHandler(PodTemplateHandler podTemplateHandler) {
        this.podTemplateHandler = podTemplateHandler;
    }

    public Job getJob(ResourceConfig config,
                      List<ImageConfiguration> images) {
        return new JobBuilder()
                .withMetadata(createJobSpecMetaData(config))
                .withSpec(createJobSpec(config, images))
                .build();
    }

    // ===========================================================

    private ObjectMeta createJobSpecMetaData(ResourceConfig config) {
        return new ObjectMetaBuilder()
                .withName(KubernetesHelper.validateKubernetesId(config.getControllerName(), "controller name"))
                .build();
    }

    private JobSpec createJobSpec(ResourceConfig config, List<ImageConfiguration> images) {
        return new JobSpecBuilder()
                .withTemplate(podTemplateHandler.getPodTemplate(config, images))
                .build();
    }
}
