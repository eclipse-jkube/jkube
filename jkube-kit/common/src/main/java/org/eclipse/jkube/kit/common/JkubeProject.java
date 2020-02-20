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
import java.util.List;
import java.util.Properties;

public class JkubeProject implements Serializable {
    private String name;
    private String groupId;
    private String artifactId;
    private String version;
    private String outputDirectory;
    private File baseDirectory;
    private String buildDirectory;
    private Properties properties;
    private List<String> compileClassPathElements;
    private List<JkubeProjectDependency> dependencies;
    private List<JkubeProjectDependency> dependenciesWithTransitive;
    private List<JkubeProjectPlugin> plugins;
    private String site;
    private String description;
    private String organizationName;
    private String documentationUrl;
    private String buildFinalName;
    private File artifact;
    private String localRepositoryBaseDirectory;
    private String packaging;
    private String issueManagementSystem;
    private String issueManagementUrl;
    private String scmUrl;
    private String scmTag;

    public String getName() {
        return name;
    }

    public List<String> getCompileClassPathElements() {
        return compileClassPathElements;
    }

    public void setCompileClassPathElements(List<String> compileClassPathElements) {
        this.compileClassPathElements = compileClassPathElements;
    }

    public List<JkubeProjectDependency> getDependencies() {
        return dependencies;
    }

    public List<JkubeProjectDependency> getDependenciesWithTransitive() {
        return dependenciesWithTransitive;
    }

    public void setDependencies(List<JkubeProjectDependency> dependencies) {
        this.dependencies = dependencies;
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

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public List<JkubeProjectPlugin> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<JkubeProjectPlugin> plugins) {
        this.plugins = plugins;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public String getBuildDirectory() { return buildDirectory; }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getSite() {
        return site;
    }

    public String getDescription() {
        return description;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    public String getLocalRepositoryBaseDirectory() {
        return localRepositoryBaseDirectory;
    }

    public File getArtifact() {
        return artifact;
    }

    public String getBuildFinalName() {
        return buildFinalName;
    }

    public String getPackaging() {
        return packaging;
    }

    public String getScmUrl() {
        return scmUrl;
    }

    public String getScmTag() {
        return scmTag;
    }

    public String getIssueManagementSystem() {
        return issueManagementSystem;
    }

    public void setIssueManagementSystem(String issueManagementSystem) {
        this.issueManagementSystem = issueManagementSystem;
    }

    public String getIssueManagementUrl() {
        return issueManagementUrl;
    }

    public void setIssueManagementUrl(String issueManagementUrl) {
        this.issueManagementUrl = issueManagementUrl;
    }

    public void setScmUrl(String scmUrl) {
        this.scmUrl = scmUrl;
    }

    public void setScmTag(String scmTag) {
        this.scmTag = scmTag;
    }

    public static class Builder {
        private JkubeProject jkubeProject = new JkubeProject();

        public Builder() { }

        public Builder(JkubeProject project) {
            this.jkubeProject.plugins = project.getPlugins();
            this.jkubeProject.groupId = project.getGroupId();
            this.jkubeProject.artifactId = project.getArtifactId();
            this.jkubeProject.version = project.getVersion();
            this.jkubeProject.properties = project.getProperties();
            this.jkubeProject.compileClassPathElements = project.getCompileClassPathElements();
            this.jkubeProject.plugins = project.getPlugins();
            this.jkubeProject.dependencies = project.getDependencies();
        }

        public Builder name(String name) {
            jkubeProject.name = name;
            return this;
        }

        public Builder groupId(String groupId) {
            jkubeProject.groupId = groupId;
            return this;
        }

        public Builder artifactId(String artifactId) {
            jkubeProject.artifactId = artifactId;
            return this;
        }

        public Builder version(String version) {
            jkubeProject.version = version;
            return this;
        }

        public Builder properties(Properties properties) {
            jkubeProject.properties = properties;
            return this;
        }

        public Builder plugins(List<JkubeProjectPlugin> plugins) {
            jkubeProject.plugins = plugins;
            return this;
        }

        public Builder dependencies(List<String> dependencies) {
            jkubeProject.dependencies = JkubeProjectDependency.listFromStringDependencies(dependencies);
            return this;
        }

        public Builder dependenciesWithTransitive(List<String> dependencies) {
            jkubeProject.dependenciesWithTransitive = JkubeProjectDependency.listFromStringDependencies(dependencies);
            return this;
        }

        public Builder compileClassPathElements(List<String> compileClassPathElements) {
            jkubeProject.compileClassPathElements = compileClassPathElements;
            return this;
        }

        public Builder outputDirectory(String outputDirectory) {
            jkubeProject.outputDirectory = outputDirectory;
            return this;
        }

        public Builder buildDirectory(String buildDirectory) {
            jkubeProject.buildDirectory = buildDirectory;
            return this;
        }

        public Builder site(String websiteUrl) {
            jkubeProject.site = websiteUrl;
            return this;
        }

        public Builder description(String description) {
            jkubeProject.description = description;
            return this;
        }

        public Builder organization(String organization) {
            jkubeProject.organizationName = organization;
            return this;
        }

        public Builder baseDirectory(File baseDirectory) {
            jkubeProject.baseDirectory = baseDirectory;
            return this;
        }

        public Builder documentationUrl(String documentationUrl) {
            jkubeProject.documentationUrl = documentationUrl;
            return this;
        }

        public Builder artifact(File artifact) {
            jkubeProject.artifact = artifact;
            return this;
        }

        public Builder buildFinalName(String buildFinalName) {
            jkubeProject.buildFinalName = buildFinalName;
            return this;
        }

        public Builder localRepositoryBaseDirectory(String localRepositoryBaseDirectory) {
            jkubeProject.localRepositoryBaseDirectory = localRepositoryBaseDirectory;
            return this;
        }

        public Builder packaging(String packaging) {
            jkubeProject.packaging = packaging;
            return this;
        }

        public Builder issueManagementUrl(String issueManagementUrl) {
            jkubeProject.issueManagementUrl = issueManagementUrl;
            return this;
        }

        public Builder issueManagementSystem(String issueManagementSystem) {
            jkubeProject.issueManagementSystem = issueManagementSystem;
            return this;
        }

        public Builder scmUrl(String scmUrl) {
            jkubeProject.scmUrl = scmUrl;
            return this;
        }

        public Builder scmTag(String scmTag) {
            jkubeProject.scmTag = scmTag;
            return this;
        }

        public JkubeProject build() {
            return jkubeProject;
        }
    }
}
