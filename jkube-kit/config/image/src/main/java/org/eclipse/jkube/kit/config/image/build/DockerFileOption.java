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

/**
 * List of options for Docker keywords
 */
public enum DockerFileOption
{
    HEALTHCHECK_INTERVAL("interval"),
    HEALTHCHECK_TIMEOUT("timeout"),
    HEALTHCHECK_START_PERIOD("start-period"),
    HEALTHCHECK_RETRIES("retries");

    private String key;

    DockerFileOption(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**\
     * Appends the option with the giv
     *
     * @param sb string builder
     * @param value object as value
     */
    public void addTo(StringBuilder sb, Object value) {
        sb.append("--");
        sb.append(getKey());
        sb.append("=");
        sb.append(value);
        sb.append(" ");
    }
}
