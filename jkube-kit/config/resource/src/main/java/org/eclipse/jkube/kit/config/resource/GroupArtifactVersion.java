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

public class GroupArtifactVersion {

    private static final String PREFIX = "s";

    private final String groupId;
    private final String artifactId;
    private final String version;

    public GroupArtifactVersion(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

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
        if (this.artifactId != null && !this.artifactId.isEmpty() && Character.isDigit(this.artifactId.charAt(0))) {
            return PREFIX + this.artifactId;
        }

        return this.artifactId;
    }
}

