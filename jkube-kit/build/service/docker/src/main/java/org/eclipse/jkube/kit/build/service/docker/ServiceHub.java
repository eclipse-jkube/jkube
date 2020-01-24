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

/**
 * A service hub responsible for creating and managing services which are used by
 * Mojos for calling to the docker backend. The docker backend (DAO) is injected from the outside.
 *
 * @author roland
 * @since 01/12/15
 */
public class ServiceHub {

    private final DockerAccess dockerAccess;

    private final QueryService queryService;
    private final RunService runService;
    private final RegistryService registryService;
    private final BuildService buildService;
    private final ArchiveService archiveService;
    private final VolumeService volumeService;
    private final WatchService watchService;
    private final WaitService waitService;

    ServiceHub(DockerAccess dockerAccess, ContainerTracker containerTracker,
               DockerAssemblyManager dockerAssemblyManager,
               KitLogger logger, LogOutputSpecFactory logSpecFactory) {

        this.dockerAccess = dockerAccess;

        archiveService = new ArchiveService(dockerAssemblyManager, logger);

        if (dockerAccess != null) {
            queryService = new QueryService(dockerAccess);
            registryService = new RegistryService(dockerAccess, logger);
            runService = new RunService(dockerAccess, queryService, containerTracker, logSpecFactory, logger);
            buildService = new BuildService(dockerAccess, queryService, registryService, archiveService, logger);
            volumeService = new VolumeService(dockerAccess);
            watchService = new WatchService(archiveService, buildService, dockerAccess, queryService, runService, logger);
            waitService = new WaitService(dockerAccess, queryService, logger);
        } else {
            queryService = null;
            registryService = null;
            runService = null;
            buildService = null;
            volumeService = null;
            watchService = null;
            waitService = null;
        }
    }

    /**
     * Get access object for contacting the docker daemon
     *
     * @return docker access object
     */
    public DockerAccess getDockerAccess() {
        checkDockerAccessInitialization();
        return dockerAccess;
    }

    /**
     * Service for doing the build against a Docker daemon
     *
     * @return get the build service
     */
    public BuildService getBuildService() {
        checkDockerAccessInitialization();
        return buildService;
    }

    /**
     * Get the query service for obtaining information about containers and images
     *
     * @return query service
     */
    public QueryService getQueryService() {
        checkDockerAccessInitialization();
        return queryService;
    }

    /**
     * Get the registry service to push/pull images
     *
     * @return query service
     */
    public RegistryService getRegistryService() {
        checkDockerAccessInitialization();
        return registryService;
    }


    /**
     * The run service is responsible for creating and starting up containers
     *
     * @return the run service
     */
    public RunService getRunService() {
        checkDockerAccessInitialization();
        return runService;
    }

    /**
     * The volume service is responsible for creating volumes
     *
     * @return the run service
     */
    public VolumeService getVolumeService() {
        checkDockerAccessInitialization();
        return volumeService;
    }

    /**
     * The watch service is responsible for watching container status and rebuilding
     *
     * @return the watch service
     */
    public WatchService getWatchService() {
        checkDockerAccessInitialization();
        return watchService;
    }

    /**
     * The wait service is responsible on waiting on container based on several
     * conditions
     *
     * @return the wait service
     */
    public WaitService getWaitService() {
        checkDockerAccessInitialization();
        return waitService;
    }

    /**
     * Serivce for creating archives
     *
     * @return the archive service
     */
    public ArchiveService getArchiveService() {
        return archiveService;
    }

    private synchronized void checkDockerAccessInitialization() {
        if (dockerAccess == null) {
            throw new IllegalStateException("Service hub created without a docker access to a docker daemon");
        }
    }


}
