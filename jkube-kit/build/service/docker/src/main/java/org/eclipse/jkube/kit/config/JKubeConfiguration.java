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
package org.eclipse.jkube.kit.config;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jkube.kit.build.service.docker.RegistryConfig;
import org.eclipse.jkube.kit.common.JavaProject;

/**
 * @author roland
 * @since 16.10.18
 */
public class JKubeConfiguration implements Serializable {

    private static final long serialVersionUID = 7459084747241070651L;

    private JavaProject project;
    private String sourceDirectory;
    private String outputDirectory;
    private Map<String, String> buildArgs;
    private RegistryConfig registryConfig;
    private List<JavaProject> reactorProjects;

    public JavaProject getProject() {
        return project;
    }

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public Map<String, String> getBuildArgs() {
        return buildArgs;
    }

    public RegistryConfig getRegistryConfig() {
        return registryConfig;
    }

    public File getBasedir() {
        return project.getBaseDirectory();
    }

    public Properties getProperties() {
        return project.getProperties();
    }

    public File inOutputDir(String path) {
        return inDir(getOutputDirectory(), path);
    }

    public File inSourceDir(String path) {
        return inDir(getSourceDirectory(), path);
    }

    public File inDir(String dir, String path) {
        File file = new File(path);
        if (file.isAbsolute()) {
            return file;
        }
        File absoluteSourceDir = new File(getBasedir(), dir);
        return new File(absoluteSourceDir, path);
    }


    public List<JavaProject> getReactorProjects() {
    return reactorProjects;
  }

    public static class Builder {

        private JKubeConfiguration context;

        public Builder() {
            this.context = new JKubeConfiguration();
        }

        public Builder(JKubeConfiguration context) {
            this.context = context;
        }

        public Builder sourceDirectory(String sourceDirectory) {
            context.sourceDirectory = sourceDirectory;
            return this;
        }

        public Builder outputDirectory(String outputDirectory) {
            context.outputDirectory = outputDirectory;
            return this;
        }

        public Builder project(JavaProject project) {
            context.project = project;
            return this;
        }

        public Builder buildArgs(Map<String, String> buildArgs) {
            context.buildArgs = buildArgs;
            return this;
        }

        public Builder registryConfig(RegistryConfig registryConfig) {
            context.registryConfig = registryConfig;
            return this;
        }
        public Builder reactorProjects(List<JavaProject> reactorProjects) {
            context.reactorProjects = reactorProjects;
            return this;
        }

        public JKubeConfiguration build() {
            return context;
        }

    }
}
