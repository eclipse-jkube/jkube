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
package org.eclipse.jkube.maven.enricher.specific;

import com.google.common.base.Objects;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ExecAction;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.HealthCheckConfiguration;
import org.eclipse.jkube.kit.config.image.build.HealthCheckMode;
import org.eclipse.jkube.maven.enricher.api.JkubeEnricherContext;
import org.eclipse.jkube.maven.enricher.api.util.KubernetesResourceUtil;

import java.util.Collections;
import java.util.List;

import static org.eclipse.jkube.maven.enricher.api.util.GoTimeUtil.durationSeconds;


/**
 * Enrich a container with probes when health checks are defined in the {@code ImageConfiguration} of the docker maven plugin.
 * This enricher could need a change when Dockerfile health checks will be supported natively in Openshift or Kubernetes.
 */
public class DockerHealthCheckEnricher extends AbstractHealthCheckEnricher {

    public DockerHealthCheckEnricher(JkubeEnricherContext buildContext) {
        super(buildContext, "jkube-healthcheck-docker");
    }

    @Override
    protected Probe getReadinessProbe(ContainerBuilder container) {
        return getProbe(container);
    }

    @Override
    protected Probe getLivenessProbe(ContainerBuilder container) {
        return getProbe(container);
    }


    private Probe getProbe(ContainerBuilder container) {
        ImageConfiguration image = getImageWithContainerName(container.getName());
        if (image != null) {
            return getProbe(image);
        }

        return null;
    }

    private Probe getProbe(ImageConfiguration image) {
        if (hasHealthCheck(image)) {
            HealthCheckConfiguration health = image.getBuildConfiguration().getHealthCheck();
            return new ProbeBuilder()
                    .withExec(new ExecAction(health.getCmd().asStrings()))
                    .withTimeoutSeconds(durationSeconds(health.getTimeout()))
                    .withPeriodSeconds(durationSeconds(health.getInterval()))
                    .withFailureThreshold(health.getRetries())
                    .build();
        }

        return null;
    }

    private boolean hasHealthCheck(ImageConfiguration image) {
        return image.getBuildConfiguration() !=null &&
                image.getBuildConfiguration().getHealthCheck() != null &&
                image.getBuildConfiguration().getHealthCheck().getCmd() != null &&
                image.getBuildConfiguration().getHealthCheck().getMode() == HealthCheckMode.cmd;
    }

    private ImageConfiguration getImageWithContainerName(String containerName) {
        if (containerName == null) {
            return null;
        }
        List<ImageConfiguration> images = getImages().orElse(Collections.emptyList());
        for (ImageConfiguration image : images) {
            String imageContainerName = KubernetesResourceUtil.extractContainerName(getContext().getGav(), image);
                if (Objects.equal(containerName, imageContainerName)) {
                    return image;
                }
        }
        return null;
    }

}
