/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jshift.kit.config.service;

import io.jshift.kit.common.service.ArtifactResolverService;
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
