package io.jshift.kit.build.service.docker;/*
 * 
 * Copyright 2015 Roland Huss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.jshift.kit.build.maven.MavenBuildContext;
import io.jshift.kit.build.maven.assembly.ArchiverCustomizer;
import io.jshift.kit.build.maven.assembly.AssemblyFiles;
import io.jshift.kit.build.maven.assembly.DockerAssemblyManager;
import io.jshift.kit.common.KitLogger;
import io.jshift.kit.config.image.build.BuildConfiguration;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author roland
 * @since 30/11/15
 */
public class ArchiveService {


    private final KitLogger log;
    private DockerAssemblyManager dockerAssemblyManager;


    public ArchiveService(DockerAssemblyManager dockerAssemblyManager, KitLogger log) {
        this.log = log;
        this.dockerAssemblyManager = dockerAssemblyManager;
    }

    /**
     * Create the tar file container the source for building an image. This tar can be used directly for
     * uploading to a Docker daemon for creating the image
     *
     * @param imageConfig the image configuration
     * @param params mojo params for the project
     * @return file for holding the sources
     * @throws IOException if during creation of the tar an error occurs.
     */
    public File createDockerBuildArchive(ImageConfiguration imageConfig, MavenBuildContext params)
            throws IOException {
        return createDockerBuildArchive(imageConfig, params, null);
    }

    /**
     * Create the tar file container the source for building an image. This tar can be used directly for
     * uploading to a Docker daemon for creating the image
     *
     * @param imageConfig the image configuration
     * @param params mojo params for the project
     * @param customizer final customizer to be applied to the tar before being generated
     * @return file for holding the sources
     * @throws IOException if during creation of the tar an error occurs.
     */
    public File createDockerBuildArchive(ImageConfiguration imageConfig, MavenBuildContext params, ArchiverCustomizer customizer)
            throws IOException {
        File ret = createArchive(imageConfig.getName(), imageConfig.getBuildConfiguration(), params, log, customizer);
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
     * @param mojoParameters needed for tracking the assembly
     * @return mapping of assembly files
     * @throws IOException IO Exception
     */
    public AssemblyFiles getAssemblyFiles(ImageConfiguration imageConfig, MavenBuildContext mojoParameters)
        throws IOException {

        String name = imageConfig.getName();
        try {
            return dockerAssemblyManager.getAssemblyFiles(name, imageConfig.getBuildConfiguration(), mojoParameters, log);
        } catch (InvalidAssemblerConfigurationException | ArchiveCreationException | AssemblyFormattingException e) {
            throw new IOException("Cannot extract assembly files for image " + name + ": " + e, e);
        }
    }

    /**
     * Create an tar archive from a set of assembly files. Only files which changed since the last call are included.
     *
     * @param entries changed files. List must not be empty or null
     * @param assemblyDir assembly directory
     * @param imageName image's name
     * @param mojoParameters maven build context
     * @return created archive
     * @throws IOException in case of any I/O exception
     */
    public File createChangedFilesArchive(List<AssemblyFiles.Entry> entries, File assemblyDir,
                                          String imageName, MavenBuildContext mojoParameters) throws IOException {
        return dockerAssemblyManager.createChangedFilesArchive(entries, assemblyDir, imageName, mojoParameters);
    }

    // =============================================

    File createArchive(String imageName, BuildConfiguration buildConfig, MavenBuildContext params, KitLogger log)
            throws IOException {
        return createArchive(imageName, buildConfig, params, log, null);
    }

    File createArchive(String imageName, BuildConfiguration buildConfig, MavenBuildContext params, KitLogger log, ArchiverCustomizer customizer)
            throws IOException {
        return dockerAssemblyManager.createDockerTarArchive(imageName, params, buildConfig, log, customizer);
    }
}
