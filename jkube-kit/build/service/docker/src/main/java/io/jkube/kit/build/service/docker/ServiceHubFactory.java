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
package io.jkube.kit.build.service.docker;

import io.jkube.kit.build.maven.assembly.DockerAssemblyManager;
import io.jkube.kit.build.service.docker.access.DockerAccess;
import io.jkube.kit.build.service.docker.access.log.LogOutputSpecFactory;
import io.jkube.kit.common.KitLogger;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.project.MavenProject;
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
    protected BuildPluginManager pluginManager;

    @Requirement
    protected DockerAssemblyManager dockerAssemblyManager;

    private LogOutputSpecFactory logOutputSpecFactory;

    public ServiceHub createServiceHub(MavenProject project, MavenSession session, DockerAccess access, KitLogger log, LogOutputSpecFactory logSpecFactory) {
        this.logOutputSpecFactory = logSpecFactory;
        return new ServiceHub(access, containerTracker, pluginManager, dockerAssemblyManager, project, session,
                              log, logSpecFactory);
    }

    public LogOutputSpecFactory getLogOutputSpecFactory() {
        return logOutputSpecFactory;
    }

}
