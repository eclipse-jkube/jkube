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
public class VolumeConfiguration implements Serializable
{
    /** Volume Name */
    private String name;

    /** Volume driver for mounting the volume */
    private String driver;

    /** Driver specific options */
    private Map<String, String> opts;

    /** Volume labels */
    private Map<String, String> labels;

    public String getName() {
        return name;
    }

    public String getDriver() {
        return driver;
    }

    public Map<String, String> getOpts() {
        return opts;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    // =============================================================================

    public static class Builder {
        private final VolumeConfiguration config;

        public Builder()  {
            this(null);
        }

        public Builder(VolumeConfiguration that) {
            this.config = that == null ? new VolumeConfiguration() : DeepCopy.copy(that);
        }

        public Builder name(String name) {
            config.name = name;
            return this;
        }

        public Builder driver(String driver) {
            config.driver = driver;
            return this;
        }

        public Builder opts(Map<String, String> opts) {
            config.opts = opts;
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            config.labels = labels;
            return this;
        }

        public VolumeConfiguration build() {
            return config;
        }
    }

}
