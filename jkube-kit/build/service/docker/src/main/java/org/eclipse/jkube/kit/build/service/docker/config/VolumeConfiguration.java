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
package org.eclipse.jkube.kit.build.service.docker.config;

import java.io.Serializable;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *  Volume Configuration for Volumes to be created prior to container start
 *
 *  @author Tom Burton
 *  @version Dec 15, 2016
 */
@SuppressWarnings("JavaDoc")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class VolumeConfiguration implements Serializable {

    private static final long serialVersionUID = -310412541839312313L;

    /**
     * Volume Name
     * @param name name of volume configuration
     * @return name of specified volume configuration
     */
    private String name;

    /**
     * Volume driver for mounting the volume
     * @param driver volume driver
     * @return string indicating volume driver
     */
    private String driver;

    /**
     * Driver specific options
     * @param opts driver specific options
     * @return map containing driver specific options
     */
    private Map<String, String> opts;

    /**
     * Volume labels
     * @param labels volume labels
     * @return map containing volume labels
     */
    private Map<String, String> labels;

}
