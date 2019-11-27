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

/**
 * Configuration for watching on image changes
 */
public class WatchImageConfiguration implements Serializable {

    private Integer interval;

    private WatchMode mode;

    private String postGoal;

    private String postExec;

    public WatchImageConfiguration() {};

    public int getInterval() {
        return interval != null ? interval : 5000;
    }

    public Integer getIntervalRaw() {
        return interval;
    }

    public WatchMode getMode() {
        return mode;
    }

    public String getPostGoal() {
        return postGoal;
    }

    public String getPostExec() {
        return postExec;
    }

    public static class Builder {

        private final WatchImageConfiguration c;

        public Builder() {
            this(null);
        }

        public Builder(WatchImageConfiguration that) {
            if (that == null) {
                this.c = new WatchImageConfiguration();
            } else {
                this.c = DeepCopy.copy(that);
            }
        }

        public Builder interval(Integer interval) {
            c.interval = interval;
            return this;
        }

        public Builder mode(String mode) {
            if (mode != null) {
                c.mode = WatchMode.valueOf(mode.toLowerCase());
            }
            return this;
        }

        public Builder postGoal(String goal) {
            c.postGoal = goal;
            return this;
        }

        public Builder postExec(String exec) {
            c.postExec = exec;
            return this;
        }

        public WatchImageConfiguration build() {
            return c;
        }
    }


}
