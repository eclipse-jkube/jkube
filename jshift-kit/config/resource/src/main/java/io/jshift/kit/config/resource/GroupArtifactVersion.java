package io.jshift.kit.config.resource;

public class GroupArtifactVersion {

    private static final String PREFIX = "s";

    private String groupId;
    private String artifactId;
    private String version;

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

