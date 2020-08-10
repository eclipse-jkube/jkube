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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.build.api.helper.DockerFileUtil;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.AssemblyMode;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.eclipse.jkube.kit.common.archive.JKubeTarArchiver;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.DockerFileBuilder;
import org.eclipse.jkube.kit.config.image.build.JKubeConfiguration;

import javax.annotation.Nonnull;

import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getAssemblyConfigurationOrCreateDefault;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getJKubeAssemblyFileSets;
import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.getJKubeAssemblyFiles;
import static org.eclipse.jkube.kit.common.archive.AssemblyFileSetUtils.processAssemblyFileSet;
import static org.eclipse.jkube.kit.common.archive.AssemblyFileUtils.getAssemblyFileOutputDirectory;
import static org.eclipse.jkube.kit.common.archive.AssemblyFileUtils.resolveSourceFile;

/**
 * Tool for creating a docker image tar ball including a Dockerfile for building
 * a docker image.
 *
 * @author roland
 */
public class AssemblyManager {

    private static AssemblyManager dockerAssemblyManager = null;
    public static final String DEFAULT_DATA_BASE_IMAGE = "busybox:latest";
    public static final String SCRATCH_IMAGE = "scratch";

    // Assembly name used also as build directory within outputBuildDir
    private static final String DOCKERFILE_NAME = "Dockerfile";

    private AssemblyManager() { }

    public static AssemblyManager getInstance() {
        if (dockerAssemblyManager == null) {
            dockerAssemblyManager = new AssemblyManager();
        }
        return dockerAssemblyManager;
    }

    /**
     * Create an docker tar archive from the given configuration which can be send to the Docker host for
     * creating the image.
     *
     * @param imageName Name of the image to create (used for creating build directories)
     * @param configuration Mojos parameters (used for finding the directories)
     * @param buildConfig configuration for how to build the image
     * @param log KitLogger used to display warning if permissions are to be normalized
     * @param finalCustomizer finalCustomizer to be applied to the tar archive
     * @return file holding the path to the created assembly tar file
     * @throws IOException IO exception
     */
    public File createDockerTarArchive(
        String imageName, final JKubeConfiguration configuration, final BuildConfiguration buildConfig, KitLogger log,
        ArchiverCustomizer finalCustomizer) throws IOException {

        final BuildDirs buildDirs = createBuildDirs(imageName, configuration);
        final List<ArchiverCustomizer> archiveCustomizers = new ArrayList<>();
        final AssemblyConfiguration assemblyConfig = getAssemblyConfiguration(buildConfig, configuration);
        final List<AssemblyFileEntry> assemblyFileEntries = copyFilesToFinalTarballDirectory(configuration.getProject(), buildDirs, assemblyConfig);

        try {
            if (buildConfig.isDockerFileMode()) {
                createDockerTarArchiveForDockerFile(buildConfig, assemblyConfig, configuration, buildDirs, log, archiveCustomizers);
            } else {
                createAssemblyArchive(assemblyConfig, configuration, buildDirs, buildConfig.getCompression(), assemblyFileEntries);
                createDockerTarArchiveForGeneratorMode(buildConfig, buildDirs, archiveCustomizers, assemblyConfig);
            }
            archiveCustomizers.addAll(
                getDefaultCustomizers(configuration, assemblyConfig, finalCustomizer, assemblyFileEntries));
            return createBuildTarBall(configuration, buildDirs, archiveCustomizers, assemblyConfig, buildConfig.getCompression());
        } catch (IOException e) {
            throw new IOException(String.format("Cannot create %s in %s", DOCKERFILE_NAME, buildDirs.getOutputDirectory()), e);
        }
    }

    /**
     * Returns the complete {@link AssemblyConfiguration} with required options for the provided {@link BuildConfiguration}
     * and {@link JKubeConfiguration}.
     *
     * @param buildConfiguration BuildConfiguration from which to compute the AssemblyConfiguration
     * @param configuration global JKubeConfiguration
     * @return the computed AssemblyConfiguration
     */
    @Nonnull
    public static AssemblyConfiguration getAssemblyConfiguration(
        @Nonnull BuildConfiguration buildConfiguration, @Nonnull JKubeConfiguration configuration) {

        if (buildConfiguration.isDockerFileMode()) {
            return getAssemblyConfigurationForDockerfileMode(buildConfiguration, configuration);
        } else {
            return getAssemblyConfigurationOrCreateDefault(buildConfiguration);
        }
    }

    private void interpolateDockerfile(File dockerFile, BuildDirs params, Properties properties, String filter) throws IOException {
        File targetDockerfile = new File(params.getOutputDirectory(), dockerFile.getName());
        String dockerFileInterpolated = DockerFileUtil.interpolate(dockerFile, properties, filter);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetDockerfile))) {
            writer.write(dockerFileInterpolated);
        }
    }

    // visible for testing
    void verifyGivenDockerfile(File dockerFile, BuildConfiguration buildConfig, Properties properties, KitLogger log) throws IOException {
        AssemblyConfiguration assemblyConfig = buildConfig.getAssembly();
        if (assemblyConfig == null) {
            return;
        }

        String name = assemblyConfig.getName();
            for (String keyword : new String[] { "ADD", "COPY" }) {
                List<String[]> lines = DockerFileUtil.extractLines(dockerFile, keyword, properties, buildConfig.getFilter());
                for (String[] line : lines) {
                    if (!line[0].startsWith("#")) {
                        // Skip command flags like --chown
                        int i;
                        for (i = 1; i < line.length; i++) {
                            String component = line[i];
                            if (!component.startsWith("--")) {
                                break;
                            }
                        }

                        // contains an ADD/COPY ... targetDir .... All good.
                        if (i < line.length && line[i].contains(name)) {
                            return;
                        }
                    }
                }
            }
        log.warn("Dockerfile %s does not contain an ADD or COPY directive to include assembly created at %s. Ignoring assembly.",
                 dockerFile.getPath(), name);
    }

    /**
     * Extract all files with a tracking archiver. These can be used to track changes in the filesystem and triggering
     * a rebuild of the image if needed ('docker:watch')
     *
     * @param imageConfiguration the image configuration
     * @param jKubeConfiguration JKube kit configuration
     * @return assembly files
     */
    public AssemblyFiles getAssemblyFiles(ImageConfiguration imageConfiguration, JKubeConfiguration jKubeConfiguration)
        throws IOException {

        BuildDirs buildDirs = createBuildDirs(imageConfiguration.getName(), jKubeConfiguration);
        AssemblyConfiguration assemblyConfig = imageConfiguration.getBuildConfiguration().getAssembly();
        AssemblyFiles assemblyFiles = new AssemblyFiles(buildDirs.getOutputDirectory());
        copyFilesToFinalTarballDirectory(jKubeConfiguration.getProject(), buildDirs, assemblyConfig)
            .forEach(assemblyFiles::addEntry);
        return assemblyFiles;
    }

    public File createChangedFilesArchive(
        List<AssemblyFileEntry> entries, File assemblyDirectory, String imageName,
        JKubeConfiguration jKubeConfiguration) throws IOException {

        BuildDirs dirs = createBuildDirs(imageName, jKubeConfiguration);
        try {
            File archive = new File(dirs.getTemporaryRootDirectory(), "changed-files.tar");
            File archiveDir = createArchiveDir(dirs);
            for (AssemblyFileEntry entry : entries) {
                File dest = prepareChangedFilesArchivePath(archiveDir, entry.getDest(), assemblyDirectory);
                Files.copy(Paths.get(entry.getSource().getAbsolutePath()), Paths.get(dest.getAbsolutePath()));
            }
            return JKubeTarArchiver.createTarBallOfDirectory(archive, archiveDir, ArchiveCompression.none);
        } catch (IOException exp) {
            throw new IOException("Error while creating " + dirs.getTemporaryRootDirectory() +
                    "/changed-files.tar: " + exp);
        }
    }

    private File prepareChangedFilesArchivePath(File archiveDir, File destFile, File assemblyDir) throws IOException {
        // Replace build target dir from destfile and add changed-files build dir instead
        String relativePath = FileUtil.getRelativeFilePath(assemblyDir.getCanonicalPath(), destFile.getCanonicalPath());
        return new File(archiveDir, relativePath);
    }

    // Create final tar-ball to be used for building the archive to send to the Docker daemon
    private File createBuildTarBall(JKubeConfiguration params, BuildDirs buildDirs, List<ArchiverCustomizer> archiverCustomizers,
                                    AssemblyConfiguration assemblyConfig, ArchiveCompression compression) throws IOException {
        AssemblyConfigurationSource source = new AssemblyConfigurationSource(params, buildDirs, assemblyConfig);

        JKubeBuildTarArchiver jkubeTarArchiver = new JKubeBuildTarArchiver();
        for (ArchiverCustomizer customizer : archiverCustomizers) {
            if (customizer != null) {
                jkubeTarArchiver = customizer.customize(jkubeTarArchiver);
            }
        }
        return jkubeTarArchiver.createArchive(source.getOutputDirectory(), buildDirs, compression);
    }

    private File createArchiveDir(BuildDirs dirs) throws IOException{
        File archiveDir = new File(dirs.getTemporaryRootDirectory(), "changed-files");
        if (archiveDir.exists()) {
            // Remove old stuff to
            FileUtil.cleanDirectory(archiveDir);
        } else {
            if (!archiveDir.mkdir()) {
                throw new IOException("Cannot create " + archiveDir);
            }
        }
        return archiveDir;
    }

    // visible for testing
    DockerFileBuilder createDockerFileBuilder(BuildConfiguration buildConfig, AssemblyConfiguration assemblyConfig) {
        DockerFileBuilder builder =
                new DockerFileBuilder()
                        .env(buildConfig.getEnv())
                        .labels(buildConfig.getLabels())
                        .expose(buildConfig.getPorts())
                        .run(buildConfig.getRunCmds())
                        .volumes(buildConfig.getVolumes())
                        .user(buildConfig.getUser());
        if (buildConfig.getMaintainer() != null) {
            builder.maintainer(buildConfig.getMaintainer());
        }
        if (buildConfig.getWorkdir() != null) {
            builder.workdir(buildConfig.getWorkdir());
        }
        if (assemblyConfig != null) {
            builder.add(assemblyConfig.getTargetDir(), "")
                   .basedir(assemblyConfig.getTargetDir())
                   .assemblyUser(assemblyConfig.getUser())
                   .exportTargetDir(assemblyConfig.getExportTargetDir());
        } else {
            builder.exportTargetDir(false);
        }

        builder.baseImage(buildConfig.getFrom());

        if (buildConfig.getHealthCheck() != null) {
            builder.healthCheck(buildConfig.getHealthCheck());
        }

        if (buildConfig.getCmd() != null){
            builder.cmd(buildConfig.getCmd());
        }

        if (buildConfig.getEntryPoint() != null){
            builder.entryPoint(buildConfig.getEntryPoint());
        }

        if (buildConfig.optimise()) {
            builder.optimise();
        }

        return builder;
    }

    private void createAssemblyArchive(
        AssemblyConfiguration assemblyConfig, JKubeConfiguration params, BuildDirs buildDirs, ArchiveCompression compression,
        List<AssemblyFileEntry> assemblyFileEntries)
        throws IOException {

        if (!hasAssemblyConfiguration(assemblyConfig)) {
            return;
        }
        AssemblyConfigurationSource source = new AssemblyConfigurationSource(params, buildDirs, assemblyConfig);
        JKubeBuildTarArchiver jkubeTarArchiver = new JKubeBuildTarArchiver();

        AssemblyMode buildMode = assemblyConfig.getMode();
        try {
            assemblyFileEntries.stream().filter(afe -> StringUtils.isNotBlank(afe.getFileMode()))
                .forEach(jkubeTarArchiver::setFileMode);
            jkubeTarArchiver.createArchive(source.getOutputDirectory(), buildDirs, compression);
        } catch (IOException e) {
            String error = "Failed to create assembly for docker image " +
                           " (with mode '" + buildMode + "'): " + e.getMessage() + ".";
            if (params.getProject().getArtifact() == null) {
                error += " If you include the build artifact please ensure that you have " +
                         "built the artifact before with 'mvn package' (should be available in the target/ dir). " +
                         "Please see the documentation (section \"Assembly\") for more information.";
            }
            throw new IOException(error, e);
        }
    }

    // Set an artifact file if it is missing. This workaround the issues
    // mentioned first in https://issues.apache.org/jira/browse/MASSEMBLY-94 which requires the package
    // phase to run so set the ArtifactFile. There is no good solution, so we are trying
    // to be very defensive and add a workaround for some situation which won't work for every occasion.
    // Unfortunately a plain forking of the Maven lifecycle is not good enough, since the MavenProject
    // gets cloned before the fork, and the 'package' plugin (e.g. JarPlugin) sets the file on the cloned
    // object which is then not available for the BuildMojo (there the file is still null leading to the
    // the "Cannot include project artifact: ... The following patterns were never triggered in this artifact inclusion filter: <artifact>"
    // warning with an error following.
    File ensureThatArtifactFileIsSet(JavaProject project) throws IOException {
        File oldFile = project.getArtifact();
        if (oldFile != null) {
            return oldFile;
        }
        final File artifactFile = JKubeProjectUtil.getFinalOutputArtifact(project);
        if (artifactFile != null && artifactFile.exists() && artifactFile.isFile()) {
            setArtifactFile(project, artifactFile);
            return artifactFile;
        }
        return null;
    }

    private void setArtifactFile(JavaProject project, File artifactFile) throws IOException {
        if (artifactFile != null) {
            File artifact = new File(project.getBuildDirectory(), artifactFile.getName());
            Files.copy(Paths.get(artifactFile.getAbsolutePath()), Paths.get(artifact.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public List<AssemblyFileEntry> copyFilesToFinalTarballDirectory(
        JavaProject project, BuildDirs buildDirs, AssemblyConfiguration assemblyConfiguration) throws IOException {

        final List<AssemblyFileEntry> files = new ArrayList<>();
        FileUtil.createDirectory(new File(buildDirs.getOutputDirectory(), assemblyConfiguration.getTargetDir()));
        for (AssemblyFileSet fileSet : getJKubeAssemblyFileSets(assemblyConfiguration)) {
            files.addAll(processAssemblyFileSet(project.getBaseDirectory(), buildDirs.getOutputDirectory(), fileSet, assemblyConfiguration));
        }
        for (AssemblyFile file : getJKubeAssemblyFiles(assemblyConfiguration)) {
            files.add(processJKubeProjectAssemblyFile(project, file, buildDirs, assemblyConfiguration));
        }
        return files;
    }

    private AssemblyFileEntry processJKubeProjectAssemblyFile(
        JavaProject project, AssemblyFile assemblyFile, BuildDirs buildDirs, AssemblyConfiguration assemblyConfiguration)
      throws IOException {

        final File sourceFile = resolveSourceFile(project.getBaseDirectory(), assemblyFile);

        final File outputDirectory = getAssemblyFileOutputDirectory(assemblyFile, buildDirs.getOutputDirectory(), assemblyConfiguration);
        FileUtil.createDirectory(outputDirectory);

        final String destinationFilename = Optional.ofNullable(assemblyFile.getDestName()).orElse(sourceFile.getName());
        final File destinationFile = new File(outputDirectory, destinationFilename);
        FileUtil.copy(sourceFile, destinationFile);
        return new AssemblyFileEntry(sourceFile, destinationFile, assemblyFile.getFileMode());
    }

    private static BuildDirs createBuildDirs(String imageName, JKubeConfiguration params) {
        BuildDirs buildDirs = new BuildDirs(imageName, params);
        buildDirs.createDirs();
        return buildDirs;
    }

    private static boolean hasAssemblyConfiguration(AssemblyConfiguration assemblyConfig) {
        return assemblyConfig != null && assemblyConfig.getInline() != null;
    }

    private static boolean isArchive(AssemblyConfiguration assemblyConfig) {
        return hasAssemblyConfiguration(assemblyConfig) &&
          assemblyConfig.getMode() != null &&
          assemblyConfig.getMode().isArchive();
    }

    private void createDockerTarArchiveForDockerFile(BuildConfiguration buildConfig, AssemblyConfiguration assemblyConfig, JKubeConfiguration params, BuildDirs buildDirs, KitLogger log, List<ArchiverCustomizer> archiveCustomizers) throws IOException {
        // Use specified docker directory which must include a Dockerfile.
        final File dockerFile = buildConfig.getAbsoluteDockerFilePath(params.getSourceDirectory(), params.getProject().getBaseDirectory().toString());
        if (!dockerFile.exists()) {
            throw new IOException("Configured Dockerfile \"" +
                    buildConfig.getDockerFile() + "\" (resolved to \"" + dockerFile + "\") doesn't exist");
        }

        verifyGivenDockerfile(dockerFile, buildConfig, params.getProperties(), log);
        interpolateDockerfile(dockerFile, buildDirs, params.getProperties(), buildConfig.getFilter());
        // User dedicated Dockerfile from extra directory
        archiveCustomizers.add(archiver -> {
            // If the content is added as archive, then we need to add the Dockerfile from the builddir
            // directly to docker.tar (as the output builddir is not picked up in archive mode)
            if (isArchive(assemblyConfig)) {
                String name = dockerFile.getName();
                archiver.includeFile(new File(buildDirs.getOutputDirectory(), name), name);
            }

            return archiver;
        });
    }

    private void createDockerTarArchiveForGeneratorMode(BuildConfiguration buildConfig, BuildDirs buildDirs, List<ArchiverCustomizer> archiveCustomizers, final AssemblyConfiguration assemblyConfig) throws IOException {
        // Create custom docker file in output dir
        DockerFileBuilder builder = createDockerFileBuilder(buildConfig, assemblyConfig);
        builder.write(buildDirs.getOutputDirectory());
        // Add own Dockerfile
        final File dockerFile = new File(buildDirs.getOutputDirectory(), DOCKERFILE_NAME);
        archiveCustomizers.add(archiver -> {
            archiver.includeFile(dockerFile, DOCKERFILE_NAME);
            return archiver;
        });

    }

    @Nonnull
    private static List<ArchiverCustomizer> getDefaultCustomizers(JKubeConfiguration configuration,
        AssemblyConfiguration assemblyConfiguration, ArchiverCustomizer finalCustomizer, List<AssemblyFileEntry> fileEntries) {
        final List<ArchiverCustomizer> archiverCustomizers = new ArrayList<>();
        if (!assemblyConfiguration.isExcludeFinalOutputArtifact()) {
            archiverCustomizers.add(finalOutputArtifactCustomizer(configuration, assemblyConfiguration));
        }
        if (finalCustomizer != null) {
            archiverCustomizers.add(finalCustomizer);
        }
        archiverCustomizers.add(fileModeCustomizer(fileEntries));
        return archiverCustomizers;
    }

    @Nonnull
    private static AssemblyConfiguration getAssemblyConfigurationForDockerfileMode(
        BuildConfiguration buildConfiguration, JKubeConfiguration params) {

        AssemblyConfiguration assemblyConfig = getAssemblyConfigurationOrCreateDefault(buildConfiguration);
        final AssemblyConfiguration.AssemblyConfigurationBuilder builder = assemblyConfig.toBuilder();
        final Assembly.AssemblyBuilder inlineBuilder = assemblyConfig.getInline() == null ? Assembly.builder() : assemblyConfig.getInline().toBuilder();

        File contextDir = buildConfiguration.getAbsoluteContextDirPath(params.getSourceDirectory(), params.getBasedir().getAbsolutePath());
        builder.inline(inlineBuilder
                       .fileSet(AssemblyFileSet.builder()
                           .directory(contextDir)
                           .outputDirectory(new File("."))
                           .directoryMode("0775")
                           .build()).build());
        return builder.build();
    }

    @Nonnull
    private static ArchiverCustomizer finalOutputArtifactCustomizer(
        @Nonnull JKubeConfiguration configuration, @Nonnull AssemblyConfiguration assemblyConfiguration) {

        return a -> {
            File finalArtifactFile = JKubeProjectUtil.getFinalOutputArtifact(configuration.getProject());
            if (finalArtifactFile != null) {
                a.includeFile(finalArtifactFile, assemblyConfiguration.getTargetDir() + File.separator + finalArtifactFile.getName());
            }
            return a;
        };
    }

    @Nonnull
    private static ArchiverCustomizer fileModeCustomizer(@Nonnull List<AssemblyFileEntry> fileEntries) {
        return a -> {
            fileEntries.stream().filter(afe -> StringUtils.isNotBlank(afe.getFileMode()))
                .forEach(a::setFileMode);
            return a;
        };
    }

}
