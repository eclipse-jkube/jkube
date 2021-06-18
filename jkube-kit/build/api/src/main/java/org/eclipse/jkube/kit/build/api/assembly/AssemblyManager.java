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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.eclipse.jkube.kit.common.JKubeConfiguration;

import javax.annotation.Nonnull;

import static org.eclipse.jkube.kit.build.api.assembly.AssemblyConfigurationUtils.createDockerFileBuilder;
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
    private static final String DOCKER_IGNORE = ".jkube-dockerignore";
    private static final String DOCKER_EXCLUDE = ".jkube-dockerexclude";
    private static final String DOCKER_INCLUDE = ".jkube-dockerinclude";
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
        final Map<Assembly, List<AssemblyFileEntry>> layers = copyFilesToFinalTarballDirectory(
            configuration, buildDirs, assemblyConfig);

        try {
            if (buildConfig.isDockerFileMode()) {
                createDockerTarArchiveForDockerFile(buildConfig, assemblyConfig, configuration, buildDirs, log, archiveCustomizers);
            } else {
                createAssemblyArchive(assemblyConfig, configuration, buildDirs, buildConfig.getCompression(), layers);
                createDockerTarArchiveForGeneratorMode(buildConfig, buildDirs, archiveCustomizers, assemblyConfig, layers);
            }
            archiveCustomizers.addAll(getDefaultCustomizers(configuration, assemblyConfig, finalCustomizer, layers));
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
        @Nonnull BuildConfiguration buildConfiguration, @Nonnull JKubeConfiguration configuration) throws IOException {

        if (buildConfiguration.isDockerFileMode()) {
            return getAssemblyConfigurationForDockerfileMode(configuration, buildConfiguration, configuration);
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
    private static Optional<String> firstNonOptionArgument(String... lineComponents) {
        return Stream.of(lineComponents)
            .skip(1)
            // Skip command flags like --chown
            .filter(component -> !component.startsWith("--"))
            .findFirst();
    }

    // visible for testing
    static void verifyAssemblyReferencedInDockerfile(
        File dockerFile, BuildConfiguration buildConfig, Properties properties, KitLogger log) throws IOException {
        if (buildConfig.getAssembly() == null) {
            return;
        }
        final List<String[]> keywordLines = new ArrayList<>();
        for (String keyword : new String[] { "ADD", "COPY" }) {
            keywordLines.addAll(
                DockerFileUtil.extractLines(dockerFile, keyword, properties, buildConfig.getFilter()).stream()
                .filter(line -> !line[0].startsWith("#"))
                .collect(Collectors.toList())
            );
        }
        // TODO need to verify layers not assembly. Each layer must correspond to a COPY/ADD statement
        final String name = buildConfig.getAssembly().getName();
        for (String[] line : keywordLines) {
            // contains an ADD/COPY ... targetDir .... All good.
            if (firstNonOptionArgument(line).filter(arg -> arg.contains(name)).isPresent()) {
                return;
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
        final AssemblyConfiguration assemblyConfig = imageConfiguration.getBuildConfiguration().getAssembly()
            .getFlattenedClone(jKubeConfiguration);
        AssemblyFiles assemblyFiles = new AssemblyFiles(buildDirs.getOutputDirectory());
        copyFilesToFinalTarballDirectory(jKubeConfiguration, buildDirs, assemblyConfig)
            .values().stream().flatMap(List::stream)
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
                Files.createDirectories(dest.getParentFile().toPath());
                Files.copy(Paths.get(entry.getSource().getAbsolutePath()), Paths.get(dest.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
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

    private void createAssemblyArchive(
        AssemblyConfiguration assemblyConfig, JKubeConfiguration params, BuildDirs buildDirs, ArchiveCompression compression,
        Map<Assembly, List<AssemblyFileEntry>> layers)
        throws IOException {

        if (layers.isEmpty()) {
            return;
        }
        AssemblyConfigurationSource source = new AssemblyConfigurationSource(params, buildDirs, assemblyConfig);
        JKubeBuildTarArchiver jkubeTarArchiver = new JKubeBuildTarArchiver();

        AssemblyMode buildMode = assemblyConfig.getMode();
        try {
            layers.values().stream().flatMap(List::stream)
                .filter(afe -> StringUtils.isNotBlank(afe.getFileMode()))
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

    public Map<Assembly, List<AssemblyFileEntry>> copyFilesToFinalTarballDirectory(
        JKubeConfiguration configuration, BuildDirs buildDirs, AssemblyConfiguration assemblyConfiguration) throws IOException {
        final Map<Assembly, List<AssemblyFileEntry>> entries = new LinkedHashMap<>();
        FileUtil.createDirectory(new File(buildDirs.getOutputDirectory(), assemblyConfiguration.getTargetDir()));
        final List<Assembly> layers = assemblyConfiguration.getProcessedLayers(configuration);
        if (layers.size() > 1 && layers.stream().anyMatch(l -> StringUtils.isBlank(l.getId()))) {
            throw new IllegalStateException("Assemblies with more than one layer require a proper id for each layer");
        }
        for (Assembly layer : layers) {
            entries.put(layer, copyLayerFilesToFinalTarballDirectory(configuration.getProject(), buildDirs, assemblyConfiguration, layer));
        }
        return entries;
    }

    private List<AssemblyFileEntry> copyLayerFilesToFinalTarballDirectory(JavaProject project, BuildDirs buildDirs,
        AssemblyConfiguration assemblyConfiguration, Assembly layer) throws IOException {

        final List<AssemblyFileEntry> files = new ArrayList<>();
        for (AssemblyFileSet fileSet : getJKubeAssemblyFileSets(layer)) {
            files.addAll(processAssemblyFileSet(
                project.getBaseDirectory(), buildDirs.getOutputDirectory(), fileSet, layer, assemblyConfiguration));
        }
        for (AssemblyFile file : getJKubeAssemblyFiles(layer)) {
            files.add(processJKubeProjectAssemblyFile(project, file, buildDirs, layer, assemblyConfiguration));
        }
        return files;
    }

    private AssemblyFileEntry processJKubeProjectAssemblyFile(
        JavaProject project, AssemblyFile assemblyFile, BuildDirs buildDirs, Assembly layer,
        AssemblyConfiguration assemblyConfiguration) throws IOException {

        final File sourceFile = resolveSourceFile(project.getBaseDirectory(), assemblyFile);

        final File outputDirectory = getAssemblyFileOutputDirectory(
            assemblyFile, buildDirs.getOutputDirectory(), layer, assemblyConfiguration);
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
        return assemblyConfig != null && !assemblyConfig.getLayers().isEmpty();
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

        verifyAssemblyReferencedInDockerfile(dockerFile, buildConfig, params.getProperties(), log);
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

    private void createDockerTarArchiveForGeneratorMode(BuildConfiguration buildConfig, BuildDirs buildDirs,
        List<ArchiverCustomizer> archiveCustomizers, AssemblyConfiguration assemblyConfiguration,
        Map<Assembly, List<AssemblyFileEntry>> layers) throws IOException {
        // Create custom docker file in output dir
        DockerFileBuilder builder = createDockerFileBuilder(buildConfig, assemblyConfiguration, layers);
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
        AssemblyConfiguration assemblyConfiguration, ArchiverCustomizer finalCustomizer,
        Map<Assembly, List<AssemblyFileEntry>> layers) {
        final List<ArchiverCustomizer> archiverCustomizers = new ArrayList<>();
        if (finalCustomizer != null) {
            archiverCustomizers.add(finalCustomizer);
        }
        layers.values().forEach(fileEntries -> archiverCustomizers.add(fileModeCustomizer(fileEntries)));
        return archiverCustomizers;
    }

    @Nonnull
    private static AssemblyConfiguration getAssemblyConfigurationForDockerfileMode(
        JKubeConfiguration configuration, BuildConfiguration buildConfiguration, JKubeConfiguration params) throws IOException {

        AssemblyConfiguration assemblyConfig = getAssemblyConfigurationOrCreateDefault(buildConfiguration);
        final AssemblyConfiguration.AssemblyConfigurationBuilder builder = assemblyConfig.toBuilder();

        File contextDir = buildConfiguration.getAbsoluteContextDirPath(params.getSourceDirectory(), params.getBasedir().getAbsolutePath());
        final AssemblyFileSet assemblyFileSet = AssemblyFileSet.builder()
                .directory(contextDir)
                .outputDirectory(new File("."))
                .directoryMode("0775")
            .excludes(createDockerExcludesList(contextDir, params.getOutputDirectory()))
            .includes(createDockerIncludesList(contextDir))
            .build();
        builder.layer(Assembly.builder().fileSet(assemblyFileSet).build());
        return builder.build().getFlattenedClone(configuration);
    }

    @Nonnull
    private static ArchiverCustomizer fileModeCustomizer(@Nonnull List<AssemblyFileEntry> fileEntries) {
        return a -> {
            fileEntries.stream().filter(afe -> StringUtils.isNotBlank(afe.getFileMode()))
                .forEach(a::setFileMode);
            return a;
        };
    }

    private static List<String> createDockerExcludesList(File directory, String outputDirectory) throws IOException {
        List<String> excludes = new ArrayList<>();
        // Output directory will be always excluded
        excludes.add(String.format("%s{/**,}", outputDirectory));
        for (String dockerConfigFile : new String[] { DOCKER_EXCLUDE, DOCKER_IGNORE } ) {
            File dockerIgnore = new File(directory, dockerConfigFile);
            if (dockerIgnore.exists()) {
                excludes.addAll(Files.readAllLines(dockerIgnore.toPath()));
                excludes.add(dockerConfigFile);
            }
        }
        return excludes;
    }

    private static List<String> createDockerIncludesList(File directory) throws IOException {
        File dockerInclude = new File(directory, DOCKER_INCLUDE);
        List<String> includes = new ArrayList<>();
        if (dockerInclude.exists()) {
            includes.addAll(Files.readAllLines(dockerInclude.toPath()));
        }
        return includes;
    }

}
