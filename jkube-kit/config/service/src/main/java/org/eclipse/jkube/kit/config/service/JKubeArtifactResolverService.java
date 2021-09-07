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
package org.eclipse.jkube.kit.config.service;

import java.io.File;
import java.util.Objects;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.service.ArtifactResolverService;

/**
 * Allows retrieving artifacts using Maven.
 */
class JKubeArtifactResolverService implements ArtifactResolverService {

    private JavaProject project;

    JKubeArtifactResolverService(JavaProject project) {
        this.project = Objects.requireNonNull(project, "project");
    }

    @Override
    public File resolveArtifact(String groupId, String artifactId, String version, String type) {
        String canonicalString = String.join(File.separator, groupId.split("\\.")) + File.separator + artifactId + File.separator +
                version + File.separator + artifactId + "-" + version + "." + type;
        if (project.getLocalRepositoryBaseDirectory() != null) {
            return new File(project.getLocalRepositoryBaseDirectory(), canonicalString);
        }

        throw new IllegalStateException("Cannot find artifact " + canonicalString + " within the resolved resources");
    }

}
