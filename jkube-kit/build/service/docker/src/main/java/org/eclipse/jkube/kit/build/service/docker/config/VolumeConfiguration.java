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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.jkube.kit.build.service.docker.helper.DeepCopy;

import java.io.Serializable;
import java.lang.String;
import java.util.Map;

/**
 *  Volume Configuration for Volumes to be created prior to container start
 *
 *  @author Tom Burton
 *  @version Dec 15, 2016
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class VolumeConfiguration implements Serializable {

    private static final long serialVersionUID = -310412541839312313L;

    /**
     * Volume Name
     */
    private String name;

    /**
     * Volume driver for mounting the volume
     */
    private String driver;

    /**
     * Driver specific options
     */
    private Map<String, String> opts;

    /**
     * Volume labels
     */
    private Map<String, String> labels;

}
