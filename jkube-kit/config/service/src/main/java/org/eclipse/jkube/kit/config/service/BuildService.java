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

import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;

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
     * Post processing step called after all images has been build
     * @param config build configuration
     */
    void postProcess(BuildServiceConfig config);

}
