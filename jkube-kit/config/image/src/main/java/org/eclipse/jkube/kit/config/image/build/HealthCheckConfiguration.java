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
package org.eclipse.jkube.kit.config.image.build;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Optional;

/**
 * Build configuration for health checks.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class HealthCheckConfiguration implements Serializable {

    private static final long serialVersionUID = 3756852848945587001L;

    private HealthCheckMode mode;
    private String interval;
    private String timeout;
    private String startPeriod;
    private Integer retries;
    private Arguments cmd;

    public HealthCheckMode getMode() {
        return Optional.ofNullable(mode).orElse(HealthCheckMode.cmd);
    }

    public String getInterval() {
        return prepareTimeValue(interval);
    }

    public String getTimeout() {
        return prepareTimeValue(timeout);
    }

    public String getStartPeriod() {
        return prepareTimeValue(startPeriod);
    }

    private static String prepareTimeValue(String timeout) {
        // Seconds as default
        if (timeout == null) {
            return null;
        }
        return timeout.matches("^\\d+$") ? timeout + "s" : timeout;
    }


    public void validate() {
        if (getMode() == null) {
            throw new IllegalArgumentException("HealthCheck: mode must not be null");
        }

        if (getMode() == HealthCheckMode.none) {
            if (interval != null || timeout != null || startPeriod != null || retries != null || cmd != null) {
                throw new IllegalArgumentException("HealthCheck: no parameters are allowed when the health check mode is set to 'none'");
            }
        } else if (getMode() == HealthCheckMode.cmd && cmd == null) {
            throw new IllegalArgumentException("HealthCheck: the parameter 'cmd' is mandatory when the health check mode is set to 'cmd' (default)");
        }
    }

    public static class HealthCheckConfigurationBuilder {
        public HealthCheckConfigurationBuilder modeString(String modeString) {
            mode = Optional.ofNullable(modeString).map(HealthCheckMode::valueOf).orElse(null);
            return this;
        }
    }
}
