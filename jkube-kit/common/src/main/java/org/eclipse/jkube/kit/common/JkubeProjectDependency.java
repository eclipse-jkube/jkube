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
package org.eclipse.jkube.kit.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JkubeProjectDependency implements Serializable {
    private String groupId;
    private String artifactId;
    private String version;

    public JkubeProjectDependency(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public static JkubeProjectDependency fromString(String jkubeDependencyAsString) {
        String[] parts = jkubeDependencyAsString.split(",");
        if (parts.length == 3) {
            return new JkubeProjectDependency(parts[0], parts[1], parts[2]);
        }
        return null;
    }

    public static List<JkubeProjectDependency> listFromStringDependencies(List<String> jkubeDependenciesAsStr) {
        List<JkubeProjectDependency> dependencies = new ArrayList<>();
        for (String commaSeparatedDependencies : jkubeDependenciesAsStr) {
            JkubeProjectDependency jkubeProjectDependency = JkubeProjectDependency.fromString(commaSeparatedDependencies);
            if (jkubeProjectDependency != null) {
                dependencies.add(jkubeProjectDependency);
            }
        }
        return dependencies;
    }
}
