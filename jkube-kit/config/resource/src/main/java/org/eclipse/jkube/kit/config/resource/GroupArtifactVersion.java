/*
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

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GroupArtifactVersion {

    private static final String PREFIX = "s";

    private final String groupId;
    private final String artifactId;
    private final String version;

    public boolean isSnapshot() {
        return getVersion() != null && getVersion().endsWith("SNAPSHOT");
    }

    /**
     * ArtifactId is used for setting a resource name (service, pod,...) in Kubernetes resource.
     * The problem is that a Kubernetes resource name must start by a char.
     * This method returns a valid string to be used as Kubernetes name.
     * @return Sanitized Kubernetes name.
     */
    public String getSanitizedArtifactId() {
        return sanitize(getArtifactId());
    }

    public static String sanitize(String coordinate) {
        if (coordinate != null && !coordinate.isEmpty() && Character.isDigit(coordinate.charAt(0))) {
            return PREFIX + coordinate;
        }
        return coordinate;
    }
}

