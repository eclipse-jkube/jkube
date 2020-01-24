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
package org.eclipse.jkube.kit.build.service.docker;

import org.eclipse.jkube.kit.build.core.assembly.DockerAssemblyManager;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogOutputSpecFactory;
import org.eclipse.jkube.kit.common.KitLogger;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Factory for creating the ServiceHub (i.e. the overall context for performing all services)
 */
@Component(role = ServiceHubFactory.class, instantiationStrategy = "singleton")
public class ServiceHubFactory {

    // Track started containers
    private final ContainerTracker containerTracker = new ContainerTracker();

    @Requirement
    protected DockerAssemblyManager dockerAssemblyManager;

    private LogOutputSpecFactory logOutputSpecFactory;

    public ServiceHub createServiceHub(DockerAccess access, KitLogger log, LogOutputSpecFactory logSpecFactory) {
        this.logOutputSpecFactory = logSpecFactory;
        return new ServiceHub(access, containerTracker, dockerAssemblyManager,
                              log, logSpecFactory);
    }

    public LogOutputSpecFactory getLogOutputSpecFactory() {
        return logOutputSpecFactory;
    }

}
