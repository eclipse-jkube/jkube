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
package org.eclipse.jkube.kit.build.service.docker;

import org.eclipse.jkube.kit.build.api.assembly.AssemblyManager;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.common.KitLogger;

/**
 * A service hub responsible for creating and managing services which are used by
 * Mojos for calling to the docker backend. The docker backend (DAO) is injected from the outside.
 *
 * @author roland
 */
public class DockerServiceHub {

    private final DockerAccess dockerAccess;
    private final RegistryService registryService;
    private final BuildService buildService;
    private final ArchiveService archiveService;
    private final WatchService watchService;

    public static DockerServiceHub newInstance(KitLogger kitLogger) {
        return newInstance(kitLogger,
            new DockerAccessFactory().createDockerAccess(DockerAccessFactory.DockerAccessContext.getDefault(kitLogger))
        );
    }

    public static DockerServiceHub newInstance(KitLogger kitLogger, DockerAccess dockerAccess) {
      return new DockerServiceHub(dockerAccess,  AssemblyManager.getInstance(), kitLogger);
    }

    DockerServiceHub(DockerAccess dockerAccess,
                     AssemblyManager assemblyManager,
                     KitLogger logger) {

        this.dockerAccess = dockerAccess;

        archiveService = new ArchiveService(assemblyManager, logger);

        if (dockerAccess != null) {
            final QueryService queryService = new QueryService(dockerAccess);
            registryService = new RegistryService(dockerAccess, queryService, logger);
            buildService = new BuildService(dockerAccess, queryService, registryService, archiveService, logger);
            watchService = new WatchService(archiveService, buildService, logger);
        } else {
            registryService = null;
            buildService = null;
            watchService = null;
        }
    }

    /**
     * Get access object for contacting the docker daemon
     *
     * @return docker access object
     */
    public DockerAccess getDockerAccess() {
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
     * Get the registry service to push/pull images
     *
     * @return registry service
     */
    public RegistryService getRegistryService() {
        checkDockerAccessInitialization();
        return registryService;
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
     * Service for creating archives
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
