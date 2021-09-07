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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author roland
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class ProbeConfig {

    /**
     * Initial delay in seconds before the probe is started.
     */
    private Integer initialDelaySeconds;
    /**
     * Timeout in seconds how long the probe might take.
     */
    private Integer timeoutSeconds;
    /**
     * How often in seconds to perform the probe. Defaults to 10 seconds. Minimum value is 1.
     */
    private Integer periodSeconds;
    /**
     * Command to execute for probing.
     */
    private String exec;
    /**
     * Probe this URL.
     */
    private String getUrl;

    /**
     * Custom headers to set in the request.
     */
    private Map<String, String> httpHeaders;
    /**
     * TCP port to probe.
     */
    private String tcpPort;
    private Integer failureThreshold;
    private Integer successThreshold;

}

