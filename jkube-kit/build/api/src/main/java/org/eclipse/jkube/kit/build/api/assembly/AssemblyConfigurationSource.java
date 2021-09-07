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
package org.eclipse.jkube.kit.build.api.assembly;

import java.io.File;
import java.util.List;

import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;

/**
 * @author roland
 * @since 07.05.14
 */
public class AssemblyConfigurationSource {

    private final AssemblyConfiguration assemblyConfig;
    private final JKubeConfiguration context;
    private final BuildDirs buildDirs;

    public AssemblyConfigurationSource(
        JKubeConfiguration context, BuildDirs buildDirs, AssemblyConfiguration assemblyConfig) {

        this.context = context;
        this.assemblyConfig = assemblyConfig;
        this.buildDirs = buildDirs;
    }

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
    public List<JavaProject> getReactorProjects() {
        return context.getReactorProjects();
    }

    // X
    public String getEncoding() {
        return context.getProject().getProperties().getProperty("project.build.sourceEncoding");
    }

    // X
    public JavaProject getProject() {
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
