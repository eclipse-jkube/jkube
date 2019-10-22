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
package io.jkube.kit.config.service;

import io.jkube.kit.common.service.ArtifactResolverService;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import java.io.File;
import java.util.Objects;

/**
 * Allows retrieving artifacts using Maven.
 */
class ArtifactResolverServiceMavenImpl implements ArtifactResolverService {

    private MavenProject project;

    private RepositorySystem repositorySystem;

    ArtifactResolverServiceMavenImpl(RepositorySystem repositorySystem, MavenProject project) {
        this.repositorySystem = Objects.requireNonNull(repositorySystem, "repositorySystem");
        this.project = Objects.requireNonNull(project, "project");
    }

    @Override
    public File resolveArtifact(String groupId, String artifactId, String version, String type) {
        String canonicalString = groupId + ":" + artifactId + ":" + type + ":" + version;
        Artifact art = repositorySystem.createArtifact(groupId, artifactId, version, type);
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                .setArtifact(art)
                .setResolveRoot(true)
                .setOffline(false)
                .setRemoteRepositories(project.getRemoteArtifactRepositories())
                .setResolveTransitively(false);

        ArtifactResolutionResult res = repositorySystem.resolve(request);

        if (!res.isSuccess()) {
            throw new IllegalStateException("Cannot resolve artifact " + canonicalString);
        }

        for (Artifact artifact : res.getArtifacts()) {
            if (artifact.getGroupId().equals(groupId) && artifact.getArtifactId().equals(artifactId) && artifact.getVersion().equals(version) && artifact.getType().equals(type)) {
                return artifact.getFile();
            }
        }

        throw new IllegalStateException("Cannot find artifact " + canonicalString + " within the resolved resources");
    }

}
