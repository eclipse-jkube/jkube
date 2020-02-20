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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JkubeProjectDependency implements Serializable {
    private String groupId;
    private String artifactId;
    private String version;
    private String type;
    private String scope;
    private File file;

    public JkubeProjectDependency(String groupId, String artifactId, String version, String type, String scope, File file) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.scope = scope;
        this.file = file;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public static JkubeProjectDependency fromString(String jkubeDependencyAsString) {
        String[] parts = jkubeDependencyAsString.split(",");
        if (parts.length == 5) { // Case without artifact file object
            return new JkubeProjectDependency(parts[0], parts[1], parts[2], parts[3], parts[4], null);
        } else if (parts.length == 6) { // Case with artifact file object
            return new JkubeProjectDependency(parts[0], parts[1], parts[2], parts[3], parts[4], new File(parts[5]));
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
