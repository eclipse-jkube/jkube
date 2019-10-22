/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.jkube.kit.config.resource;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author roland
 * @since 22/03/16
 */
public class ProbeConfig {

    // Initial delay in seconds before the probe is started.
    @Parameter
    Integer initialDelaySeconds;

    // Timeout in seconds how long the probe might take
    @Parameter
    Integer timeoutSeconds;

    // Command to execute for probing
    @Parameter
    String exec;

    // Probe this URL
    @Parameter
    String getUrl;

    // TCP port to probe
    @Parameter
    String tcpPort;

    @Parameter
    Integer failureThreshold;

    @Parameter
    Integer successThreshold;

    public Integer getInitialDelaySeconds() {
        return initialDelaySeconds;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getExec() {
        return exec;
    }

    public String getGetUrl() {
        return getUrl;
    }

    public String getTcpPort() {
        return tcpPort;
    }

    public Integer getFailureThreshold() {
        return failureThreshold;
    }

    public Integer getSuccessThreshold() {
        return successThreshold;
    }

    // =============================================================

    public static class Builder {
        private ProbeConfig config = new ProbeConfig();

        public Builder initialDelaySeconds(Integer initialDelaySeconds) {
            config.initialDelaySeconds = initialDelaySeconds;
            return this;
        }

        public Builder timeoutSeconds(Integer timeoutSeconds) {
            config.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder exec(String exec) {
            config.exec = exec;
            return this;
        }

        public Builder tcpPort(String tcpPort) {
            config.tcpPort = tcpPort;
            return this;
        }

        public Builder getUrl(String getUrl) {
            config.getUrl = getUrl;
            return this;
        }

        public Builder failureThreshold(Integer failureThreshold) {
            config.failureThreshold = failureThreshold;
            return this;
        }

        public Builder successThreshold(Integer successThreshold) {
            config.successThreshold = successThreshold;
            return this;
        }

        public ProbeConfig build() {
            return config;
        }
    }
}

