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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jkube.kit.build.core.JKubeBuildContext;
import org.eclipse.jkube.kit.build.service.docker.helper.DockerFileUtil;
import org.eclipse.jkube.kit.common.JKubeAssemblyFile;
import org.eclipse.jkube.kit.common.JKubeAssemblyFileSet;
import org.eclipse.jkube.kit.common.JKubeProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.eclipse.jkube.kit.common.archive.JKubeTarArchiver;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.build.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.AssemblyMode;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.DockerFileBuilder;

import static org.eclipse.jkube.kit.build.core.assembly.JKubeAssemblyConfigurationUtils.getAssemblyConfigurationOrCreateDefault;
import static org.eclipse.jkube.kit.build.core.assembly.JKubeAssemblyConfigurationUtils.getJKubeAssemblyFileSets;
import static org.eclipse.jkube.kit.build.core.assembly.JKubeAssemblyConfigurationUtils.getJKubeAssemblyFileSetsExcludes;
import static org.eclipse.jkube.kit.build.core.assembly.JKubeAssemblyConfigurationUtils.getJKubeAssemblyFiles;

/**
 * Tool for creating a docker image tar ball including a Dockerfile for building
 * a docker image.
 *
 * @author roland
 * @since 08.05.14
 */
public class DockerAssemblyManager {

    private static DockerAssemblyManager dockerAssemblyManager = null;
    public static final String DEFAULT_DATA_BASE_IMAGE = "busybox:latest";
    public static final String SCRATCH_IMAGE = "scratch";

    // Assembly name used also as build directory within outputBuildDir
    public static final String DOCKER_IGNORE = ".maven-dockerignore";
    public static final String DOCKER_EXCLUDE = ".maven-dockerexclude";
    public static final String DOCKER_INCLUDE = ".maven-dockerinclude";
    private static final String DOCKERFILE_NAME = "Dockerfile";

    private DockerAssemblyManager() { }

    public static DockerAssemblyManager getInstance() {
        if (dockerAssemblyManager == null) {
            dockerAssemblyManager = new DockerAssemblyManager();
        }
        return dockerAssemblyManager;
    }

    /**
     * Create an docker tar archive from the given configuration which can be send to the Docker host for
     * creating the image.
     *
     * @param imageName Name of the image to create (used for creating build directories)
     * @param params Mojos parameters (used for finding the directories)
     * @param buildConfig configuration for how to build the image
     * @param log KitLogger used to display warning if permissions are to be normalized
     * @return file holding the path to the created assembly tar file
     * @throws IOException IO exception
     */
    public File createDockerTarArchive(
            String imageName, JKubeBuildContext params, BuildConfiguration buildConfig, KitLogger log)
            throws IOException {

        return createDockerTarArchive(imageName, params, buildConfig, log, null);
    }

    /**
     * Create an docker tar archive from the given configuration which can be send to the Docker host for
     * creating the image.
     *
     * @param imageName Name of the image to create (used for creating build directories)
     * @param params Mojos parameters (used for finding the directories)
     * @param buildConfig configuration for how to build the image
     * @param log KitLogger used to display warning if permissions are to be normalized
     * @param finalCustomizer finalCustomizer to be applied to the tar archive
     * @return file holding the path to the created assembly tar file
     * @throws IOException IO exception
     */
    public File createDockerTarArchive(
            String imageName, final JKubeBuildContext params, final BuildConfiguration buildConfig, KitLogger log,
            ArchiverCustomizer finalCustomizer) throws IOException {

        final BuildDirs buildDirs = createBuildDirs(imageName, params);

        final AssemblyConfiguration assemblyConfig = getAssemblyConfigurationOrCreateDefault(buildConfig);
        Map<File, String> fileToPermissionsMap = copyFilesToFinalTarballDirectory(params.getProject(), buildDirs, assemblyConfig);

        final List<ArchiverCustomizer> archiveCustomizers = new ArrayList<>();

        // Build up assembly. In dockerfile mode this must be added explicitly in the Dockerfile with an ADD
        if (hasAssemblyConfiguration(assemblyConfig)) {
            createAssemblyArchive(assemblyConfig, params, buildDirs, buildConfig.getCompression());
        }
        try {
            if (buildConfig.isDockerFileMode()) {
                // Use specified docker directory which must include a Dockerfile.
                final File dockerFile = buildConfig.getAbsoluteDockerFilePath(params.getSourceDirectory(), params.getProject().getBaseDirectory().toString());
                if (!dockerFile.exists()) {
                    throw new IOException("Configured Dockerfile \"" +
                                                     buildConfig.getDockerFile() + "\" (resolved to \"" + dockerFile + "\") doesn't exist");
                }

                verifyGivenDockerfile(dockerFile, buildConfig, params.getProperties(), log);
                interpolateDockerfile(dockerFile, buildDirs, params.getProperties());
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
            } else {
                // Create custom docker file in output dir
                DockerFileBuilder builder = createDockerFileBuilder(buildConfig, assemblyConfig);
                builder.write(buildDirs.getOutputDirectory());
                // Add own Dockerfile
                final File dockerFile = new File(buildDirs.getOutputDirectory(), DOCKERFILE_NAME);
                archiveCustomizers.add((archiver) -> {
                        archiver.includeFile(dockerFile, DOCKERFILE_NAME);
                        return archiver;
                    });
            }

            if (finalCustomizer != null) {
                archiveCustomizers.add(finalCustomizer);
            }
            archiveCustomizers.add((archiver) -> {
                File finalArtifactFile = JKubeProjectUtil.getFinalOutputArtifact(params.getProject());
                if (finalArtifactFile != null) {
                    archiver.includeFile(finalArtifactFile, (assemblyConfig != null ? assemblyConfig.getName() : "maven") + File.separator + finalArtifactFile.getName());
                }
                return archiver;
            });

            List<String> filesToExclude = getJKubeAssemblyFileSetsExcludes(buildConfig.getAssemblyConfiguration());
            archiveCustomizers.add(archiver -> {
                filesToExclude.forEach(archiver::excludeFile);
                fileToPermissionsMap.forEach(archiver::setFilePermissions);
                return archiver;
            });

            return createBuildTarBall(params, buildDirs, archiveCustomizers, assemblyConfig, buildConfig.getCompression());

        } catch (IOException e) {
            throw new IOException(String.format("Cannot create %s in %s", DOCKERFILE_NAME, buildDirs.getOutputDirectory()), e);
        }
    }

    private void interpolateDockerfile(File dockerFile, BuildDirs params, Properties properties) throws IOException {
        File targetDockerfile = new File(params.getOutputDirectory(), dockerFile.getName());
        String dockerFileInterpolated = DockerFileUtil.interpolate(dockerFile, properties);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetDockerfile))) {
            writer.write(dockerFileInterpolated);
        }
    }

    // visible for testing
    void verifyGivenDockerfile(File dockerFile, BuildConfiguration buildConfig, Properties properties, KitLogger log) throws IOException {
        AssemblyConfiguration assemblyConfig = buildConfig.getAssemblyConfiguration();
        if (assemblyConfig == null) {
            return;
        }

        String name = assemblyConfig.getName();
            for (String keyword : new String[] { "ADD", "COPY" }) {
                List<String[]> lines = DockerFileUtil.extractLines(dockerFile, keyword, properties);
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
     * @param name name of assembly
     * @param buildConfig build configuration
     * @param mojoParams maven build context
     * @return assembly files
     */
    public AssemblyFiles getAssemblyFiles(String name, BuildConfiguration buildConfig, JKubeBuildContext mojoParams) {

        BuildDirs buildDirs = createBuildDirs(name, mojoParams);

        AssemblyConfiguration assemblyConfig = buildConfig.getAssemblyConfiguration();
        String assemblyName = assemblyConfig.getName();

        AssemblyFiles assemblyFiles = new AssemblyFiles(buildDirs.getOutputDirectory());
        File finalOutputArtifact = JKubeProjectUtil.getFinalOutputArtifact(mojoParams.getProject());
        if (finalOutputArtifact != null) {
            assemblyFiles.addEntry(finalOutputArtifact, new File(buildDirs.getOutputDirectory().getAbsolutePath() + File.separator + assemblyName, finalOutputArtifact.getName()));
        }

        return assemblyFiles;
    }


    public File createChangedFilesArchive(List<AssemblyFiles.Entry> entries, File assemblyDirectory,
                                          String imageName, JKubeBuildContext mojoParameters)
            throws IOException {
        BuildDirs dirs = createBuildDirs(imageName, mojoParameters);
        try {
            File archive = new File(dirs.getTemporaryRootDirectory(), "changed-files.tar");
            File archiveDir = createArchiveDir(dirs);
            for (AssemblyFiles.Entry entry : entries) {
                File dest = prepareChangedFilesArchivePath(archiveDir,entry.getDestFile(),assemblyDirectory);
                Files.copy(Paths.get(entry.getSrcFile().getAbsolutePath()), Paths.get(dest.getAbsolutePath()));
            }
            return JKubeTarArchiver.createTarBallOfDirectory(archive, archiveDir, ArchiveCompression.none);
        } catch (IOException exp) {
            throw new IOException("Error while creating " + dirs.getTemporaryRootDirectory() +
                                             "/changed-files.tar: " + exp);
        }
    }

    private File prepareChangedFilesArchivePath(File archiveDir, File destFile, File assemblyDir) throws IOException {
        // Replace build target dir from destfile and add changed-files build dir instead
        String relativePath = FileUtil.getRelativeFilePath(assemblyDir.getCanonicalPath(),destFile.getCanonicalPath());
        return new File(archiveDir,relativePath);
    }

    // Create final tar-ball to be used for building the archive to send to the Docker daemon
    private File createBuildTarBall(JKubeBuildContext params, BuildDirs buildDirs, List<ArchiverCustomizer> archiverCustomizers,
                                    AssemblyConfiguration assemblyConfig, ArchiveCompression compression) throws IOException {
        DockerAssemblyConfigurationSource source = new DockerAssemblyConfigurationSource(params, buildDirs, assemblyConfig);

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
    @SuppressWarnings("deprecation")
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
            builder.add(assemblyConfig.getName(), "")
                   .basedir(assemblyConfig.getTargetDir())
                   .assemblyUser(assemblyConfig.getUser())
                   .exportTargetDir(assemblyConfig.exportTargetDir());
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

    private void createAssemblyArchive(AssemblyConfiguration assemblyConfig, JKubeBuildContext params, BuildDirs buildDirs, ArchiveCompression compression)
            throws IOException {
        DockerAssemblyConfigurationSource source = new DockerAssemblyConfigurationSource(params, buildDirs, assemblyConfig);
        JKubeBuildTarArchiver jkubeTarArchiver = new JKubeBuildTarArchiver();

        Map<File, String> fileToPermissionsMap = copyFilesToFinalTarballDirectory(params.getProject(), buildDirs, assemblyConfig);
        AssemblyMode buildMode = assemblyConfig.getMode();
        File originalArtifactFile = null;
        try {
            originalArtifactFile = ensureThatArtifactFileIsSet(params.getProject());
            fileToPermissionsMap.forEach(jkubeTarArchiver::setFilePermissions);
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
        } finally {
            setArtifactFile(params.getProject(), originalArtifactFile);
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
    File ensureThatArtifactFileIsSet(JKubeProject project) throws IOException {
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

    private void setArtifactFile(JKubeProject project, File artifactFile) throws IOException {
        if (artifactFile != null) {
            File artifact = new File(project.getBuildDirectory(), artifactFile.getName());
            Files.copy(Paths.get(artifactFile.getAbsolutePath()), Paths.get(artifact.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Map<File, String> copyFilesToFinalTarballDirectory(
      JKubeProject project, BuildDirs buildDirs, AssemblyConfiguration assemblyConfiguration) throws IOException {

        final Map<File, String> filesToPermissionsMap = new HashMap<>();
        FileUtil.createDirectory(new File(buildDirs.getOutputDirectory(), assemblyConfiguration.getName()));
        for (JKubeAssemblyFileSet fileSet : getJKubeAssemblyFileSets(assemblyConfiguration)) {
            filesToPermissionsMap.putAll(processJKubeProjectAssemblyFileSet(project, fileSet, buildDirs, assemblyConfiguration));
        }
        for (JKubeAssemblyFile file : getJKubeAssemblyFiles(assemblyConfiguration)) {
            processJKubeProjectAssemblyFile(project, file, buildDirs, assemblyConfiguration);
        }
        return filesToPermissionsMap;
    }

  private Map<File, String> processJKubeProjectAssemblyFileSet(JKubeProject project,
      JKubeAssemblyFileSet jkubeProjectAssemblyFileSet, BuildDirs buildDirs,
      AssemblyConfiguration jkubeProjectAssemblyConfiguration) throws IOException {

    final Map<File, String> fileToPermissionsMap = new HashMap<>();

    for (String relativePathInclude : jkubeProjectAssemblyFileSet.getIncludes()) {
      final File sourceDirectory = project.getBaseDirectory().toPath()
          .resolve(jkubeProjectAssemblyFileSet.getDirectory().toPath())
          .toFile();
      final File sourceFile = new File(sourceDirectory, FileUtil.trimWildcardCharactersFromPath(relativePathInclude));
      File destParentFile = new File(buildDirs.getOutputDirectory(), jkubeProjectAssemblyConfiguration.getName());
      if (jkubeProjectAssemblyFileSet.getOutputDirectory() != null
          && !jkubeProjectAssemblyFileSet.getOutputDirectory().getName().equals(".")) {
        destParentFile = buildDirs.getOutputDirectory().toPath()
            .resolve(jkubeProjectAssemblyConfiguration.getName())
            .resolve(jkubeProjectAssemblyFileSet.getOutputDirectory().toPath())
            .toFile();
      }
      FileUtil.createDirectory(destParentFile);
      File destFile = new File(destParentFile, sourceFile.getName());

      if (sourceFile.exists()) {
        if (sourceFile.isDirectory()) {
          FileUtil.copyDirectoryIfNotExists(sourceFile, destFile);
        } else {
          FileUtil.copy(sourceFile, destFile);
        }
        fileToPermissionsMap.put(destFile, jkubeProjectAssemblyFileSet.getFileMode());
      }
    }
    return fileToPermissionsMap;
  }

    private void processJKubeProjectAssemblyFile(
      JKubeProject project, JKubeAssemblyFile assemblyFile, BuildDirs buildDirs, AssemblyConfiguration assemblyConfiguration)
      throws IOException {

        final File outputDirectory;
        if (assemblyFile.getOutputDirectory().isAbsolute()) {
            outputDirectory = assemblyFile.getOutputDirectory();
        } else {
            outputDirectory = buildDirs.getOutputDirectory().toPath()
              .resolve(assemblyConfiguration.getName())
              .resolve(assemblyFile.getOutputDirectory().toPath())
              .toFile();
        }
        final File sourceFile = project.getBaseDirectory().toPath()
            .resolve(assemblyFile.getSource().toPath()).toFile();

        FileUtil.createDirectory(outputDirectory);
        final File destinationFile = new File(outputDirectory, sourceFile.getName());
        FileUtil.copy(sourceFile, destinationFile);
    }

    private static BuildDirs createBuildDirs(String imageName, JKubeBuildContext params) {
        BuildDirs buildDirs = new BuildDirs(imageName, params);
        buildDirs.createDirs();
        return buildDirs;
    }

    private static boolean hasAssemblyConfiguration(AssemblyConfiguration assemblyConfig) {
        return assemblyConfig != null &&
          (assemblyConfig.getInline() != null ||
            assemblyConfig.getDescriptor() != null ||
            assemblyConfig.getDescriptorRef() != null);
    }

    private static boolean isArchive(AssemblyConfiguration assemblyConfig) {
        return hasAssemblyConfiguration(assemblyConfig) &&
          assemblyConfig.getMode() != null &&
          assemblyConfig.getMode().isArchive();
    }

}
