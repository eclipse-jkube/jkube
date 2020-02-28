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
package org.eclipse.jkube.kit.build.core.assembly;

import java.io.File;
import java.util.List;

import org.eclipse.jkube.kit.build.core.JKubeBuildContext;
import org.eclipse.jkube.kit.build.core.config.JKubeAssemblyConfiguration;
import org.eclipse.jkube.kit.common.JKubeProject;
import org.eclipse.jkube.kit.config.image.build.AssemblyConfiguration;

/**
 * @author roland
 * @since 07.05.14
 */
public class DockerAssemblyConfigurationSource {

    private final JKubeAssemblyConfiguration assemblyConfig;
    private final JKubeBuildContext context;
    private final BuildDirs buildDirs;

    public DockerAssemblyConfigurationSource(
            JKubeBuildContext context, BuildDirs buildDirs, JKubeAssemblyConfiguration assemblyConfig) {

        this.context = context;
        this.assemblyConfig = assemblyConfig;
        this.buildDirs = buildDirs;
    }

    public String[] getDescriptors() {
        if (assemblyConfig != null) {
          String descriptor = assemblyConfig.getDescriptor();

          if (descriptor != null) {
            return new String[] {
                context.inSourceDir(descriptor).getAbsolutePath() };
          }
        }
        return new String[0];
    }

    public String[] getDescriptorReferences() {
        if (assemblyConfig != null) {
            String descriptorRef = assemblyConfig.getDescriptorRef();
            if (descriptorRef != null) {
                return new String[]{descriptorRef};
            }
        }
        return new String[0];
    }

    // ============================================================================================

    public File getOutputDirectory() {
        return buildDirs.getOutputDirectory();
    }

    public File getWorkingDirectory() {
        return buildDirs.getWorkingDirectory();
    }

    // X
    public File getTemporaryRootDirectory() {
        return buildDirs.getTemporaryRootDirectory();
    }

    // Maybe use injection
    public List<JKubeProject> getReactorProjects() {
        return context.getReactorProjects();
    }

    // X
    public String getEncoding() {
        return context.getProject().getProperties().getProperty("project.build.sourceEncoding");
    }

    // X
    public JKubeProject getProject() {
        return context.getProject();
    }

    // X
    public File getBasedir() {
        return context.getProject().getBaseDirectory();
    }

    public boolean isIgnorePermissions() {
        return
            assemblyConfig != null &&
            assemblyConfig.getPermissions() != null &&
            assemblyConfig.getPermissions() == AssemblyConfiguration.PermissionMode.ignore;
    }
}
