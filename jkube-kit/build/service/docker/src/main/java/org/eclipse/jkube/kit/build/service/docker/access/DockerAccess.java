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
package org.eclipse.jkube.kit.build.service.docker.access;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;

import java.io.File;

/**
 * Access to the <a href="http://docs.docker.io/en/latest/reference/api/docker_remote_api/">Docker API</a> which
 * provides the methods needed by this maven plugin.
 *
 * @author roland
 */
public interface DockerAccess {

    /**
     * Check whether the given name exists as image at the docker daemon
     *
     * @param name image name to check
     * @return true if the image exists
     * @throws DockerAccessException docker access exception
     */
    boolean hasImage(String name) throws DockerAccessException;

    /**
     * Get the image id of a given name or <code>null</code> if no such image exists
     *
     * @param name name to lookup
     * @return the image id or <code>null</code>
     * @throws DockerAccessException docker access exception
     */
    String getImageId(String name) throws DockerAccessException;

    /**
     * Load an image from an archive.
     *
     * @param image the image to pull.
     * @param tarArchive archive file
     * @throws DockerAccessException if the image couldn't be loaded.
     */
    void loadImage(String image, File tarArchive) throws DockerAccessException;

    /**
     * Pull an image from a remote registry and store it locally.
     *
     * @param image the image to pull.
     * @param authConfig authentication configuration used when pulling an image
     * @param registry an optional registry from where to pull the image. Can be null.
     * @param options additional query arguments to add when creating the image. Can be null.
     * @throws DockerAccessException if the image couldn't be pulled.
     */
    void pullImage(String image, AuthConfig authConfig, String registry, CreateImageOptions options) throws DockerAccessException;

    /**
     * Push an image to a registry. An registry can be specified which is used as target
     * if the image name the image does not contain a registry.
     *
     * If an optional registry is used, the image is also tagged with the full name containing the registry as
     * part (if not already existent)
     *
     * @param image image name to push
     * @param authConfig authentication configuration
     * @param registry optional registry to which the image should be pushed.
     * @param retries optional number of times the push should be retried on a 500 error
     * @throws DockerAccessException in case pushing fails
     */
    void pushImage(String image, AuthConfig authConfig, String registry, int retries) throws DockerAccessException;

    /**
     * Create a docker image from a given archive
     *
     * @param image name of the image to build or <code>null</code> if none should be used
     * @param dockerArchive from which the docker image should be build
     * @param options additional query arguments to add when building the image. Can be null.
     * @throws DockerAccessException if docker host reports an error during building of an image
     */
    void buildImage(String image, File dockerArchive, BuildOptions options) throws DockerAccessException;

    /**
     * Alias an image in the repository with a complete new name. (Note that this maps to a Docker Remote API 'tag'
     * operation, which IMO is badly named since it also can generate a complete alias to a given image)
     *
     * @param sourceImage full name (including tag) of the image to alias
     * @param targetImage the alias name
     * @param force forced tagging
     * @throws DockerAccessException if the original image doesn't exist or another error occurs somehow.
     */
    void tag(String sourceImage, String targetImage, boolean force) throws DockerAccessException;

    /**
     * Remove an image from this docker installation
     *
     * @param image image to remove
     * @param force if set to true remove containers as well (only the first vararg is evaluated)
     * @return true if an image was removed, false if none was removed
     * @throws DockerAccessException if an image cannot be removed
     */
    boolean removeImage(String image, boolean ... force) throws DockerAccessException;

    /**
     * Save an image to a tar file
     *
     * @param image image to save
     * @param filename target filename
     * @param compression compression to use for the archive
     * @throws DockerAccessException if an image cannot be removed
     */
    void saveImage(String image, String filename, ArchiveCompression compression) throws DockerAccessException;

    /**
     * Lifecycle method for this access class which must be called before any other method is called.
     *
     * @throws DockerAccessException docker access exception
     */
    void start() throws DockerAccessException;

    /**
     * Lifecycle method which must be called when this object is not needed anymore. This hook might be used for
     * cleaning up things.
     */
    void shutdown();
}

