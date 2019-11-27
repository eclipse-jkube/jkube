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
import java.util.*;

/**
 * Run configuration for volumes.
 *
 * @author roland
 * @since 08/12/14
 */
public class RunVolumeConfiguration implements Serializable {

    /**
     * List of images names from where volumes are mounted
     */
    private List<String> from;

    /**
     * List of bind parameters for binding/mounting host directories
     * into the container
     */
    private List<String> bind;

    /**
     * List of images to mount from
     *
     * @return images
     */
    public List<String> getFrom() {
        return from;
    }

    /**
     * List of docker bind specification for mounting local directories
     * @return list of bind specs
     */
    public List<String> getBind() {
        return bind;
    }

    // ===========================================

    public static class Builder {

        private RunVolumeConfiguration config = new RunVolumeConfiguration();

        public Builder() {
            this.config = new RunVolumeConfiguration();
        }

        public Builder from(List<String> args) {
            if (args != null) {
                if (config.from == null) {
                    config.from = new ArrayList<>();
                }
                config.from.addAll(args);
            }
            return this;
        }

        public Builder bind(List<String> args) {
            if (args != null) {
                if (config.bind == null) {
                    config.bind = new ArrayList<>();
                }
                config.bind.addAll(args);
            }
            return this;
        }

        public RunVolumeConfiguration build() {
            return config;
        }
    }
}

