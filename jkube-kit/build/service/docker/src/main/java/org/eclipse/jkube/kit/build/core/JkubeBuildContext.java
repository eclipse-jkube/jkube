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
import org.eclipse.jkube.kit.build.core.config.JkubeBuildConfiguration;
import org.eclipse.jkube.kit.common.JkubeProject;
import org.eclipse.jkube.kit.common.KitLogger;

/**
 * @author roland
 * @since 16.10.18
 */
public class JkubeBuildContext implements BuildContext<JkubeBuildConfiguration>, Serializable {

    private String sourceDirectory;
    private String outputDirectory;
    private JkubeProject project;
    private List<JkubeProject> reactorProjects;
    private transient JkubeArchiveService archiveService;

    private JkubeBuildContext() { }

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
    public File createImageContentArchive(String imageName, JkubeBuildConfiguration buildConfig, KitLogger log)
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

    public JkubeProject getProject() {
        return project;
    }

	public List<JkubeProject> getReactorProjects() {
		return reactorProjects;
	}

    // =======================================================================================

    public static class Builder {

        private JkubeBuildContext context;

        public Builder() {
            this.context = new JkubeBuildContext();
        }

        public Builder(JkubeBuildContext context) {
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

        public Builder project(JkubeProject project) {
            context.project = project;
            return this;
        }

        public Builder reactorProjects(List<JkubeProject> reactorProjects) {
            context.reactorProjects = reactorProjects;
            return this;
        }

        public Builder archiveService(JkubeArchiveService archiveService) {
            context.archiveService = archiveService;
            return this;
        }

        // ================================================================================
        public JkubeBuildContext build() {
            return context;
        }

    }
}
