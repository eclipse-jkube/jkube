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
import java.util.List;

import org.eclipse.jkube.kit.build.core.assembly.ArchiverCustomizer;
import org.eclipse.jkube.kit.build.core.assembly.AssemblyFiles;
import org.eclipse.jkube.kit.build.core.assembly.DockerAssemblyManager;
import org.eclipse.jkube.kit.build.core.config.MavenBuildConfiguration;
import org.eclipse.jkube.kit.build.core.config.MavenImageConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;

/**
 * @author roland
 * @since 30/11/15
 */
public class MavenArchiveService {


    private final KitLogger log;
    private DockerAssemblyManager dockerAssemblyManager;


    public MavenArchiveService(DockerAssemblyManager dockerAssemblyManager, KitLogger log) {
        this.log = log;
        this.dockerAssemblyManager = dockerAssemblyManager;
    }

    /**
     * Create the tar file container the source for building an image. This tar can be used directly for
     * uploading to a Docker daemon for creating the image
     *
     * @param imageConfig the image configuration
     * @param context mojo params for the project
     * @param customizer final customizer to be applied to the tar before being generated
     * @return file for holding the sources
     * @throws IOException if during creation of the tar an error occurs.
     */
    public File createDockerBuildArchive(MavenImageConfiguration imageConfig, MavenBuildContext context, ArchiverCustomizer customizer)
        throws IOException {
        File ret = createArchive(imageConfig.getName(), imageConfig.getBuildConfiguration(), context, log, customizer);
        log.info("%s: Created docker source tar %s",imageConfig.getDescription(), ret);
        return ret;
    }

    /**
     * Get a mapping of original to destination files which a covered by an assembly. This can be used
     * to watch the source files for changes in order to update the target (either by recreating a docker image
     * or by copying it into a running container)
     *
     * @param imageConfig image config for which to get files. The build- and assembly configuration in this image
     *                    config must not be null.
     * @param context needed for tracking the assembly
     * @return mapping of assembly files
     * @throws IOException IOException in case of any input/output error
     */
    public AssemblyFiles getAssemblyFiles(MavenImageConfiguration imageConfig, MavenBuildContext context)
        throws IOException, MojoExecutionException {

        String name = imageConfig.getName();
        try {
            return dockerAssemblyManager.getAssemblyFiles(name, imageConfig.getBuildConfiguration(), context, log);
        } catch (InvalidAssemblerConfigurationException | ArchiveCreationException | AssemblyFormattingException e) {
            throw new IOException("Cannot extract assembly files for image " + name + ": " + e, e);
        }
    }

    /**
     * Create an tar archive from a set of assembly files. Only files which changed since the last call are included.
     * @param entries changed files. List must not be empty or null
     * @param imageName image's name
     * @param mojoParameters maven build context
     * @return created archive
     */
    public File createChangedFilesArchive(List<AssemblyFiles.Entry> entries, File assemblyDir,
                                          String imageName, MavenBuildContext mojoParameters) throws IOException {
        return dockerAssemblyManager.createChangedFilesArchive(entries, assemblyDir, imageName, mojoParameters);
    }

    // =============================================

    public File createArchive(String imageName, MavenBuildConfiguration buildConfig, MavenBuildContext ctx, KitLogger log)
        throws IOException {
        return createArchive(imageName, buildConfig, ctx, log, null);
    }

    File createArchive(String imageName, MavenBuildConfiguration buildConfig, MavenBuildContext ctx, KitLogger log, ArchiverCustomizer customizer)
        throws IOException {
        return dockerAssemblyManager.createDockerTarArchive(imageName, ctx, buildConfig, log, customizer);
    }
}
