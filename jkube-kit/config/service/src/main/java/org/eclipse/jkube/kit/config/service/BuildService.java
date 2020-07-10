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
package org.eclipse.jkube.kit.config.service;

import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.RegistryConfig;

import java.util.Collection;

/**
 * @author nicola
 * @since 17/02/2017
 */
public interface BuildService {

    /**
     * Builds the given image using the specified configuration.
     *
     * @param imageConfig the image to build
     */
    void build(ImageConfiguration imageConfig) throws JKubeServiceException;


    /**
     * Pushes to given image to specified Registry
     *
     * @param imageConfigs image configurations to process
     * @param retries number of retries
     * @param registryConfig registry configuration
     * @param skipTag boolean value whether skip tagging or not
     */
    void push(Collection<ImageConfiguration> imageConfigs, int retries, RegistryConfig registryConfig, boolean skipTag) throws JKubeServiceException;

    /**
     * Post processing step called after all images has been build
     * @param config build configuration
     */
    void postProcess(BuildServiceConfig config);

}
