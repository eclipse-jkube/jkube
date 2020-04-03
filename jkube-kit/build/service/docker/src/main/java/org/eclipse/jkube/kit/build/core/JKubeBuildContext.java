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
package org.eclipse.jkube.kit.build.core;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.kit.build.api.BuildContext;
import org.eclipse.jkube.kit.build.core.config.JKubeBuildConfiguration;
import org.eclipse.jkube.kit.common.JKubeProject;
import org.eclipse.jkube.kit.common.KitLogger;

/**
 * @author roland
 * @since 16.10.18
 */
public class JKubeBuildContext implements BuildContext<JKubeBuildConfiguration>, Serializable {

    private static final long serialVersionUID = 7459084747241070651L;

    private String sourceDirectory;
    private String outputDirectory;
    private JKubeProject project;
    private List<JKubeProject> reactorProjects;
    private transient JKubeArchiveService archiveService;

    private JKubeBuildContext() { }

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }


    public File getBasedir() {
        return project.getBaseDirectory();
    }

    @Override
    public Properties getProperties() {
        return project.getProperties();
    }

    @Override
    public File createImageContentArchive(String imageName, JKubeBuildConfiguration buildConfig, KitLogger log)
            throws IOException {

        try {
            return archiveService.createArchive(imageName, buildConfig, this, log);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public File inOutputDir(String path) {
        return inDir(getOutputDirectory(), path);
    }

    @Override
    public File inSourceDir(String path) {
        return inDir(getSourceDirectory(), path);
    }

    @Override
    public File inDir(String dir, String path) {
        File file = new File(path);
        if (file.isAbsolute()) {
            return file;
        }
        File absoluteSourceDir = new File(getBasedir(), dir);
        return new File(absoluteSourceDir, path);
    }

    // =======================================================================================
    // Maven specific method not available via interface

    public JKubeProject getProject() {
        return project;
    }

  public List<JKubeProject> getReactorProjects() {
    return reactorProjects;
  }

    // =======================================================================================

    public static class Builder {

        private JKubeBuildContext context;

        public Builder() {
            this.context = new JKubeBuildContext();
        }

        public Builder(JKubeBuildContext context) {
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

        // ===============================================================================
        // Maven specific calls

        public Builder project(JKubeProject project) {
            context.project = project;
            return this;
        }

        public Builder reactorProjects(List<JKubeProject> reactorProjects) {
            context.reactorProjects = reactorProjects;
            return this;
        }

        public Builder archiveService(JKubeArchiveService archiveService) {
            context.archiveService = archiveService;
            return this;
        }

        // ================================================================================
        public JKubeBuildContext build() {
            return context;
        }

    }
}
