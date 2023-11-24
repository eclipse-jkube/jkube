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

import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;

/**
 * Query service for getting image and container information from the docker dameon
 *
 */
public class QueryService {

    // Access to docker daemon & logger
    private final DockerAccess docker;

    /**
     * Constructor which gets its dependencies as args)
     *  @param docker remote access to docker daemon
     * */
    public QueryService(DockerAccess docker) {
        this.docker = docker;
    }

    /**
     * Finds the id of an image.
     *
     * @param imageName name of the image.
     * @return the id of the image
     * @throws DockerAccessException if the request fails
     */
    public String getImageId(String imageName) throws DockerAccessException {
        return docker.getImageId(imageName);
    }

    /**
     * Check whether the given Image is locally available.
     *
     * @param name name of image to check, the image can contain a tag, otherwise 'latest' will be appended
     * @return true if the image is locally available or false if it must be pulled.
     * @throws DockerAccessException if the request fails
     */
    public boolean hasImage(String name) throws DockerAccessException {
        return docker.hasImage(name);
    }

}
