package io.jshift.kit.common.service;

import java.io.File;

/**
 * Allows retrieving artifacts from a Maven repo.
 */
public interface ArtifactResolverService {

    public File resolveArtifact(String groupId, String artifactId, String version, String type);

}
