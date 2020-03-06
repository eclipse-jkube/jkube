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
package org.eclipse.jkube.kit.config.resource;

import java.util.Map;

public class OpenshiftBuildConfig {
    private Map<String, String> limits;
    private Map<String, String> requests;

    public Map<String, String> getRequests() {
        return requests;
    }

    public void setRequests(Map<String, String> requests) {
        this.requests = requests;
    }

    public Map<String, String> getLimits() {
        return limits;
    }

    public void setLimits(Map<String, String> resourceLimits) {
        this.limits = resourceLimits;
    }

    public static class Builder {
        private OpenshiftBuildConfig openshiftBuildConfig;

        public Builder() {
            this.openshiftBuildConfig = new OpenshiftBuildConfig();
        }

        public Builder(OpenshiftBuildConfig openshiftBuildConfig) {
            if (openshiftBuildConfig != null) {
                this.openshiftBuildConfig.limits = openshiftBuildConfig.limits;
                this.openshiftBuildConfig.requests = openshiftBuildConfig.requests;
            }
        }

        public Builder limits(Map<String, String> limits) {
            this.openshiftBuildConfig.limits = limits;
            return this;
        }

        public Builder requests(Map<String, String> requests) {
            this.openshiftBuildConfig.requests = requests;
            return this;
        }

        public OpenshiftBuildConfig build() {
            return this.openshiftBuildConfig;
        }
    }
}
