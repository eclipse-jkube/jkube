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
package io.jkube.maven.enricher.api.model;


import io.jkube.kit.config.resource.GroupArtifactVersion;

import java.io.File;

public class Dependency {

    // GAV coordinates of dependency
    private GroupArtifactVersion gav;

    // Dependency type ("jar", "war", ...)
    private String type;

    // Scope of the dependency ("compile", "runtime", ...)
    private String scope;

    // Location where the dependent jar is located
    private File location;

    public Dependency(GroupArtifactVersion gav, String type, String scope, File location) {
        this.gav = gav;
        this.type = type;
        this.scope = scope;
        this.location = location;
    }

    public GroupArtifactVersion getGav() {
        return gav;
    }

    public String getType() {
        return type;
    }

    public String getScope() {
        return scope;
    }

    public File getLocation() {
        return location;
    }
}

