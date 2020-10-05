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
package org.eclipse.jkube.kit.build.core;

import java.io.Serializable;

/**
 * Label used to mark a container belonging to a certain build.
 *
 * @author roland
 * @since 31/03/15
 */
public class GavLabel implements Serializable {

    private static final long serialVersionUID = -5056897124015874063L;

    private final String mavenCoordinates;
    public static final String KEY = "jkube.coordinates";

    /**
     * Construct from a given label
     *
     * @param label label as stored with the container
     */
    public GavLabel(String label) {
        String[] parts = label.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Label '" + label +
                    "' has not the format <group>:<artifact>:<version>");
        }
        mavenCoordinates = parts[0] + ":" + parts[1] + ":" + parts[2];
    }

    /**
     * Construct from maven coordinates and run ID. If the runId is <code>null</code> this label
     * will.
     *
     * @param groupId Maven group
     * @param artifactId Maven artifact
     * @param version version
     */
    public GavLabel(String groupId, String artifactId, String version) {
        mavenCoordinates = groupId + ":" + artifactId + ":" + version;
    }

    /**
     * Get this label in string representation
     * @return this label as string
     */
    public String getValue() {
        return mavenCoordinates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return mavenCoordinates.equals(((GavLabel) o).mavenCoordinates);
    }

    @Override
    public int hashCode() {
        return mavenCoordinates.hashCode();
    }
}

