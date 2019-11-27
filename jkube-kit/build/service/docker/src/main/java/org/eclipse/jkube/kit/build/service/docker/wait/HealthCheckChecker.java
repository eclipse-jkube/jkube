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
package org.eclipse.jkube.kit.build.service.docker.wait;

import org.eclipse.jkube.kit.build.api.model.ContainerDetails;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;
import org.eclipse.jkube.kit.common.KitLogger;

/**
 * @author roland
 * @since 29/03/2017
 */
public class HealthCheckChecker implements WaitChecker {

    private boolean first = true;

    private DockerAccess docker;
    private String containerId;
    private KitLogger log;
    private final String imageConfigDesc;

    public HealthCheckChecker(DockerAccess docker, String containerId, String imageConfigDesc, KitLogger log) {
        this.docker = docker;
        this.containerId = containerId;
        this.imageConfigDesc = imageConfigDesc;
        this.log = log;
    }

    @Override
    public boolean check() {
        try {
            final ContainerDetails container = docker.getContainer(containerId);
            if (container == null) {
                log.debug("HealthWaitChecker: Container %s not found");
                return false;
            }

            if (container.getHealthcheck() == null) {
                throw new IllegalArgumentException("Can not wait for healthstate of " + imageConfigDesc +". No HEALTHCHECK configured.");
            }

            if (first) {
                log.info("%s: Waiting to become healthy", imageConfigDesc);
                log.debug("HealthWaitChecker: Waiting for healthcheck: '%s'", container.getHealthcheck());
                first = false;
            } else if (log.isDebugEnabled()) {
                log.debug("HealthWaitChecker: Waiting on healthcheck '%s'", container.getHealthcheck());
            }

            return container.isHealthy();
        } catch(DockerAccessException e) {
            log.warn("Error while checking health: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public void cleanUp() {}

    @Override
    public String getLogLabel() {
        try {
            final ContainerDetails container = docker.getContainer(containerId);
            return String.format("on healthcheck '%s'",container != null ? container.getHealthcheck() : "[container not found]");
        } catch (DockerAccessException e) {
            return String.format("on healthcheck [error fetching container: %s]", e.getMessage());
        }
    }
}
