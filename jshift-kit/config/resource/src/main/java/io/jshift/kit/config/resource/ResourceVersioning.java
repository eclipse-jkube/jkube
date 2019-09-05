package io.jshift.kit.config.resource;

/**
 * @author nicola
 * @since 24/05/2017
 */
public class ResourceVersioning {

    private String coreVersion;

    private String extensionsVersion;

    private String appsVersion;

    private String jobVersion;

    private String openshiftV1version;

    public ResourceVersioning() {
    }

    public ResourceVersioning(String coreVersion, String extensionsVersion, String appsVersion, String jobVersion, String openshiftV1version) {
        this.coreVersion = coreVersion;
        this.extensionsVersion = extensionsVersion;
        this.appsVersion = appsVersion;
        this.jobVersion = jobVersion;
        this.openshiftV1version = openshiftV1version;
    }

    public String getCoreVersion() {
        return coreVersion;
    }

    public void setCoreVersion(String coreVersion) {
        this.coreVersion = coreVersion;
    }

    public String getExtensionsVersion() {
        return extensionsVersion;
    }

    public void setExtensionsVersion(String extensionsVersion) {
        this.extensionsVersion = extensionsVersion;
    }

    public String getAppsVersion() {
        return appsVersion;
    }

    public void setAppsVersion(String appsVersion) {
        this.appsVersion = appsVersion;
    }

    public void setOpenshiftV1Version(String openshiftV1Version) {
        this.openshiftV1version = openshiftV1Version;
    }

    public String getOpenshiftV1version() {
        return openshiftV1version;
    }

    public String getJobVersion() {
        return jobVersion;
    }

    public void setJobVersion(String jobVersion) {
        this.jobVersion = jobVersion;
    }

    public ResourceVersioning withCoreVersion(String coreVersion) {
        ResourceVersioning c = copy();
        c.setCoreVersion(coreVersion);
        return c;
    }

    public ResourceVersioning withExtensionsVersion(String extensionsVersion) {
        ResourceVersioning c = copy();
        c.setExtensionsVersion(extensionsVersion);
        return c;
    }

    public ResourceVersioning withAppsVersion(String appsVersion) {
        ResourceVersioning c = copy();
        c.setAppsVersion(appsVersion);
        return c;
    }

    public ResourceVersioning withOpenshiftV1Version(String version) {
        ResourceVersioning c = copy();
        c.setOpenshiftV1Version(version);
        return c;
    }

    public ResourceVersioning withJobVersion(String jobVersion) {
        ResourceVersioning c = copy();
        c.setJobVersion(jobVersion);
        return c;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResourceVersioning{");
        sb.append("coreVersion='").append(coreVersion).append('\'');
        sb.append(", extensionsVersion='").append(extensionsVersion).append('\'');
        sb.append(", appsVersion='").append(appsVersion).append('\'');
        sb.append(", jobVersion='").append(jobVersion).append('\'');
        sb.append(", openshiftV1Version='").append(openshiftV1version).append('\'');
        sb.append('}');
        return sb.toString();
    }

    protected ResourceVersioning copy() {
        return new ResourceVersioning(coreVersion, extensionsVersion, appsVersion, jobVersion, openshiftV1version);
    }
}

