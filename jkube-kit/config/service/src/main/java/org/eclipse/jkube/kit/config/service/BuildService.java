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
package org.eclipse.jkube.kit.config.service;

import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.RegistryConfig;

import java.util.Collection;

/**
 * @author nicola
 */
public interface BuildService {

    /**
     * Check whether provided Build Service implementation is applicable in current context or not.
     *
     * @return boolean value specifying whether provided BuildService implementation should be used.
     */
    boolean isApplicable();

    /**
     * Build the given images using specified configuration
     *
     * @param imageConfiguration {@link ImageConfiguration}(s) to build
     * @throws JKubeServiceException in case of any error while building image
     */
    void build(ImageConfiguration... imageConfiguration) throws JKubeServiceException;

    /**
     * Pushes to given image to specified Registry
     *
     * @param imageConfigs image configurations to process
     * @param retries number of retries
     * @param registryConfig registry configuration
     * @param skipTag boolean value whether skip tagging or not
     * @throws JKubeServiceException in case of any error while building image
     */
    void push(Collection<ImageConfiguration> imageConfigs, int retries, RegistryConfig registryConfig, boolean skipTag) throws JKubeServiceException;

    /**
     * Post processing step called after all images has been build
     */
    void postProcess();
}
