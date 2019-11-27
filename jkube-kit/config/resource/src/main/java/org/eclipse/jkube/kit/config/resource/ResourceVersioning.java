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
package org.eclipse.jkube.kit.config.resource;

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

    private String rbacVersion;

    private String cronJobVersion;

    public ResourceVersioning() {
    }

    public ResourceVersioning(String coreVersion, String extensionsVersion, String appsVersion, String jobVersion, String openshiftV1version, String cronJobVersion, String rbacVersion) {
        this.coreVersion = coreVersion;
        this.extensionsVersion = extensionsVersion;
        this.appsVersion = appsVersion;
        this.jobVersion = jobVersion;
        this.openshiftV1version = openshiftV1version;
        this.cronJobVersion = cronJobVersion;
        this.rbacVersion = rbacVersion;
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

    public String getCronJobVersion() { return cronJobVersion; }

    public void setCronJobVersion(String cronJobVersion) { this.cronJobVersion = cronJobVersion; }

    public String getRbacVersion() { return rbacVersion; }

    public void setRbacVersion(String rbacVersion) { this.rbacVersion = rbacVersion; }

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

    public ResourceVersioning withCronJobVersion(String cronjobVersion) {
        ResourceVersioning c = copy();
        c.setCronJobVersion(cronjobVersion);
        return c;
    }

    public ResourceVersioning withRbacVersioning(String rbacVersion) {
        ResourceVersioning c = copy();
        c.setRbacVersion(rbacVersion);
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
        sb.append(", cronjobVersion='").append(cronJobVersion).append('\'');
        sb.append(", rbacVersion='").append(rbacVersion).append('\'');
        sb.append('}');
        return sb.toString();
    }

    protected ResourceVersioning copy() {
        return new ResourceVersioning(coreVersion, extensionsVersion, appsVersion, jobVersion, openshiftV1version, cronJobVersion, rbacVersion);
    }
}

