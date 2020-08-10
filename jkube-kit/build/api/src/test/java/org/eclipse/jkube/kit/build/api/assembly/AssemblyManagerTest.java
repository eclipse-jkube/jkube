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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.eclipse.jkube.kit.config.image.build.DockerFileBuilder;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.eclipse.jkube.kit.config.image.build.JKubeConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AssemblyManagerTest {

    @Tested
    private AssemblyManager assemblyManager;

    @Mocked
    private PrefixedLogger prefixedLogger;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testGetInstanceShouldBeSingleton() {
        // When
        final AssemblyManager dam1 = AssemblyManager.getInstance();
        final AssemblyManager dam2 = AssemblyManager.getInstance();
        // Then
        assertSame(dam1, dam2);
    }

    @Test
    public void testNoAssembly() {
        BuildConfiguration buildConfig = BuildConfiguration.builder().build();
        AssemblyConfiguration assemblyConfig = buildConfig.getAssembly();

        DockerFileBuilder builder = assemblyManager.createDockerFileBuilder(buildConfig, assemblyConfig);
        String content = builder.content();

        assertFalse(content.contains("COPY"));
        assertFalse(content.contains("VOLUME"));
    }

    @Test
    public void assemblyFiles(@Injectable final JKubeConfiguration configuration, @Injectable final JavaProject project)
        throws Exception {

        // Given
        final File baseDirectory = temporaryFolder.newFolder("buildDirs");
        final File targetDir = new File(baseDirectory, "target");
        assertTrue(targetDir.mkdirs());
        new Expectations() {{
            configuration.getProject();
            result = project;

            project.getBaseDirectory();
            result = baseDirectory;
        }};
        ImageConfiguration imageConfiguration = ImageConfiguration.builder()
            .name("testImage").build(createBuildConfig())
            .build();
        // When
        AssemblyFiles assemblyFiles = assemblyManager.getAssemblyFiles(imageConfiguration, configuration);
        // Then
        assertNotNull(assemblyFiles);
        assertEquals(baseDirectory.toPath().resolve("testImage").resolve("build").toFile(), assemblyFiles.getAssemblyDirectory());
        assertTrue(assemblyFiles.getUpdatedEntriesAndRefresh().isEmpty());
    }

    @Test
    public void testCreateChangedFilesArchive() throws IOException {
        // Given
        final List<AssemblyFileEntry> entries = new ArrayList<>();
        final File assemblyDirectory = temporaryFolder.getRoot().toPath().resolve("target").resolve("docker").toFile();
        final JKubeConfiguration jc = createNoDockerfileConfiguration();
        entries.add(AssemblyFileEntry.builder()
            .source(temporaryFolder.getRoot().toPath().resolve("target").resolve("test-0.1.0.jar").toFile())
            .dest(temporaryFolder.getRoot().toPath().resolve("target").resolve("docker").resolve("test-0.1.0.jar").toFile())
            .fileMode("0655")
            .build());
        // When
        final File result = assemblyManager.createChangedFilesArchive(entries, assemblyDirectory, "image-name", jc);
        // Then
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.isFile());
        assertEquals("changed-files.tar", result.getName());
        assertEquals(1536, result.length());
    }

    @Test
    public void testCopyValidVerifyGivenDockerfile(@Injectable final KitLogger logger) throws IOException {
        BuildConfiguration buildConfig = createBuildConfig();

        assemblyManager.verifyGivenDockerfile(
                new File(getClass().getResource("/docker/Dockerfile_assembly_verify_copy_valid.test").getPath()),
                buildConfig, new Properties(),
                logger);

        new Verifications() {{
            logger.warn(anyString, (Object []) any); times = 0;
        }};

    }

    @Test
    public void testCopyInvalidVerifyGivenDockerfile(@Injectable final KitLogger logger) throws IOException {
        BuildConfiguration buildConfig = createBuildConfig();

        assemblyManager.verifyGivenDockerfile(
                new File(getClass().getResource("/docker/Dockerfile_assembly_verify_copy_invalid.test").getPath()),
                buildConfig, new Properties(),
                logger);

        new Verifications() {{
            logger.warn(anyString, (Object []) any); times = 1;
        }};

    }

    @Test
    public void testCopyChownValidVerifyGivenDockerfile(@Injectable final KitLogger logger) throws IOException {
        BuildConfiguration buildConfig = createBuildConfig();

        assemblyManager.verifyGivenDockerfile(
                new File(getClass().getResource("/docker/Dockerfile_assembly_verify_copy_chown_valid.test").getPath()),
                buildConfig,
                new Properties(),
                logger);

        new Verifications() {{
            logger.warn(anyString, (Object []) any); times = 0;
        }};
    }

    private BuildConfiguration createBuildConfig() {
        return BuildConfiguration.builder()
                .assembly(AssemblyConfiguration.builder()
                        .name("maven")
                        .targetDir("/maven")
                        .build())
                .build();
    }

    @Test
    public void testEnsureThatArtifactFileIsSetWithProjectArtifactSet() throws IOException {
        // Given
        JavaProject project = JavaProject.builder()
                .artifact(temporaryFolder.newFile("temp-project-0.0.1.jar"))
                .build();
        // When
        File artifactFile = assemblyManager.ensureThatArtifactFileIsSet(project);
        // Then
        assertNotNull(artifactFile);
        assertEquals("temp-project-0.0.1.jar", artifactFile.getName());
    }

    @Test
    public void testEnsureThatArtifactFileIsSetWithNullProjectArtifact() throws IOException {
        // Given
        File targetDirectory = temporaryFolder.newFolder("target");
        File jarFile = new File(targetDirectory, "foo-project-0.0.1.jar");
        assertTrue(jarFile.createNewFile());
        JavaProject project = JavaProject.builder()
                .buildDirectory(targetDirectory)
                .packaging("jar")
                .buildFinalName("foo-project-0.0.1")
                .build();
        // When
        File artifactFile = assemblyManager.ensureThatArtifactFileIsSet(project);
        // Then
        assertNotNull(artifactFile);
        assertTrue(artifactFile.exists());
        assertEquals("foo-project-0.0.1.jar", artifactFile.getName());
    }

    @Test
    public void testEnsureThatArtifactFileIsSetWithEverythingNull() throws IOException {
        // Given
        JavaProject project = JavaProject.builder().build();
        // When
        File artifactFile = assemblyManager.ensureThatArtifactFileIsSet(project);
        // Then
        assertNull(artifactFile);
    }

    @Test
    public void testCreateDockerTarArchiveWithoutDockerfile() throws IOException {
        // Given
        final JKubeConfiguration jKubeBuildContext = createNoDockerfileConfiguration();
        final BuildConfiguration jKubeBuildConfiguration = BuildConfiguration.builder().build();

        // When
        File dockerArchiveFile = assemblyManager.createDockerTarArchive("test-image", jKubeBuildContext, jKubeBuildConfiguration, prefixedLogger, null);

        // Then
        assertNotNull(dockerArchiveFile);
        assertTrue(dockerArchiveFile.exists());
        assertEquals(3072, dockerArchiveFile.length());
        final File outputDirectory = temporaryFolder.getRoot().toPath().resolve("target").resolve("docker").toFile();
        assertTrue(outputDirectory.isDirectory() && outputDirectory.exists());
        File buildOutputDir = new File(outputDirectory, "test-image");
        assertTrue(buildOutputDir.isDirectory() && buildOutputDir.exists());
        File buildDir = new File(buildOutputDir, "build");
        File workDir = new File(buildOutputDir, "work");
        File tmpDir = new File(buildOutputDir, "tmp");
        assertTrue(buildDir.isDirectory() && buildDir.exists());
        assertTrue(workDir.isDirectory() && workDir.exists());
        assertTrue(tmpDir.isDirectory() && tmpDir.exists());
        assertTrue(new File(buildDir, "Dockerfile").exists());
        File assemblyNameDirInBuild = new File(buildDir, "maven");
        assertTrue(assemblyNameDirInBuild.isDirectory() && assemblyNameDirInBuild.exists());
        assertTrue(new File(assemblyNameDirInBuild, "test-0.1.0.jar").exists());
    }

    @Test
    public void testCreateDockerTarArchiveWithoutDockerfileAndFinalCustomizer() throws IOException {
        // Given
        final JKubeConfiguration jKubeBuildContext = createNoDockerfileConfiguration();
        final BuildConfiguration jKubeBuildConfiguration = BuildConfiguration.builder().build();
        final AtomicBoolean customized = new AtomicBoolean(false);
        final ArchiverCustomizer finalCustomizer = ac -> {
            customized.set(true);
            return ac;
        };

        // When
        File dockerArchiveFile = assemblyManager.createDockerTarArchive(
            "test-image", jKubeBuildContext, jKubeBuildConfiguration, prefixedLogger, finalCustomizer);

        // Then
        assertNotNull(dockerArchiveFile);
        assertTrue(dockerArchiveFile.exists());
        assertEquals(3072, dockerArchiveFile.length());
        final File outputDirectory = temporaryFolder.getRoot().toPath().resolve("target").resolve("docker").toFile();
        assertTrue(outputDirectory.isDirectory() && outputDirectory.exists());
        File buildOutputDir = new File(outputDirectory, "test-image");
        assertTrue(buildOutputDir.isDirectory() && buildOutputDir.exists());
        File buildDir = new File(buildOutputDir, "build");
        File workDir = new File(buildOutputDir, "work");
        File tmpDir = new File(buildOutputDir, "tmp");
        assertTrue(buildDir.isDirectory() && buildDir.exists());
        assertTrue(workDir.isDirectory() && workDir.exists());
        assertTrue(tmpDir.isDirectory() && tmpDir.exists());
        assertTrue(new File(buildDir, "Dockerfile").exists());
        File assemblyNameDirInBuild = new File(buildDir, "maven");
        assertTrue(assemblyNameDirInBuild.isDirectory() && assemblyNameDirInBuild.exists());
        assertTrue(new File(assemblyNameDirInBuild, "test-0.1.0.jar").exists());
        assertTrue(customized.get());
    }

    private JKubeConfiguration createNoDockerfileConfiguration() throws IOException {
        File targetFolder = temporaryFolder.newFolder("target");
        File finalArtifactFile = new File(targetFolder, "test-0.1.0.jar");
        assertTrue(finalArtifactFile.createNewFile());
        File outputDirectory = new File(targetFolder, "docker");
        return JKubeConfiguration.builder()
            .project(JavaProject.builder()
                .groupId("org.eclipse.jkube")
                .artifactId("test")
                .packaging("jar")
                .version("0.1.0")
                .buildDirectory(targetFolder)
                .artifact(finalArtifactFile)
                .build())
            .outputDirectory(outputDirectory.getAbsolutePath())
            .sourceDirectory(temporaryFolder.getRoot().getAbsolutePath() + "/src/main/docker")
            .build();
    }

    @Test
    public void testCreateDockerTarArchiveWithDockerfile() throws IOException {
        // Given
        File baseProjectDir = temporaryFolder.newFolder("test-workspace");
        File dockerFile = new File(baseProjectDir, "Dockerfile");
        assertTrue(dockerFile.createNewFile());
        writeASimpleDockerfile(dockerFile);
        File targetDirectory = new File(baseProjectDir, "target");
        File outputDirectory = new File(targetDirectory, "classes");
        assertTrue(outputDirectory.mkdirs());
        File finalArtifactFile = new File(targetDirectory, "test-0.1.0.jar");
        assertTrue(finalArtifactFile.createNewFile());
        File dockerDirectory = new File(targetDirectory, "docker");

        final JKubeConfiguration configuration = JKubeConfiguration.builder()
                .project(JavaProject.builder()
                        .groupId("org.eclipse.jkube")
                        .artifactId("test")
                        .packaging("jar")
                        .version("0.1.0")
                        .buildDirectory(targetDirectory)
                        .baseDirectory(baseProjectDir)
                        .outputDirectory(outputDirectory)
                        .properties(new Properties())
                        .artifact(finalArtifactFile)
                        .build())
                .outputDirectory("target/docker")
                .sourceDirectory(baseProjectDir.getPath() + "/src/main/docker")
                .build();
        final BuildConfiguration jKubeBuildConfiguration = BuildConfiguration.builder()
                .dockerFile(dockerFile.getPath())
                .dockerFileFile(dockerFile)
                .build();


        // When
        File dockerArchiveFile = assemblyManager.createDockerTarArchive("test-image", configuration, jKubeBuildConfiguration, prefixedLogger, null);

        // Then
        assertNotNull(dockerArchiveFile);
        assertTrue(dockerArchiveFile.exists());
        assertTrue(dockerDirectory.isDirectory() && dockerDirectory.exists());
        File buildOutputDir = new File(dockerDirectory, "test-image");
        assertTrue(buildOutputDir.isDirectory() && buildOutputDir.exists());
        File buildDir = new File(buildOutputDir, "build");
        File workDir = new File(buildOutputDir, "work");
        File tmpDir = new File(buildOutputDir, "tmp");
        assertTrue(buildDir.isDirectory() && buildDir.exists());
        assertTrue(workDir.isDirectory() && workDir.exists());
        assertTrue(tmpDir.isDirectory() && tmpDir.exists());
        assertTrue(new File(buildDir, "Dockerfile").exists());
        File jarCopiedInBuildDirs = new File(buildDir, "maven/target/test-0.1.0.jar");
        assertTrue(jarCopiedInBuildDirs.exists());
    }

    private void writeASimpleDockerfile(File dockerFile) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter(dockerFile, "UTF-8");
        writer.println("FROM openjdk:jre");
        writer.close();
    }

    @Test
    public void testCreateDockerTarArchiveWithDockerfileAndAssembly() throws IOException {
        // Given
        File baseProjectDir = temporaryFolder.newFolder("test-workspace");
        File dockerFile = new File(baseProjectDir, "Dockerfile");
        assertTrue(dockerFile.createNewFile());
        writeASimpleDockerfile(dockerFile);

        File assemblyFolder = new File(baseProjectDir, "additional");
        assertTrue(assemblyFolder.mkdirs());
        File extraFile = new File(assemblyFolder, "extraFile.txt");
        assertTrue(extraFile.createNewFile());

        File targetDirectory = new File(baseProjectDir, "target");
        File outputDirectory = new File(targetDirectory, "classes");
        assertTrue(outputDirectory.mkdirs());
        File finalArtifactFile = new File(targetDirectory, "test-0.1.0.jar");
        assertTrue(finalArtifactFile.createNewFile());
        File dockerDirectory = new File(targetDirectory, "docker");
        File buildOutputDir = new File(dockerDirectory, "test-image");
        File buildDir = new File(buildOutputDir, "build");
        File workDir = new File(buildOutputDir, "work");
        File tmpDir = new File(buildOutputDir, "tmp");
        File mavenDir = new File(buildDir, "maven");
        File jarCopiedInBuildDirs = new File(mavenDir, "target/test-0.1.0.jar");

        final JKubeConfiguration configuration = JKubeConfiguration.builder()
                .project(JavaProject.builder()
                        .groupId("org.eclipse.jkube")
                        .artifactId("test")
                        .packaging("jar")
                        .version("0.1.0")
                        .buildDirectory(targetDirectory)
                        .baseDirectory(baseProjectDir)
                        .outputDirectory(outputDirectory)
                        .properties(new Properties())
                        .artifact(finalArtifactFile)
                        .build())
                .outputDirectory("target/docker")
                .sourceDirectory(baseProjectDir.getPath() + "/src/main/docker")
                .build();

        AssemblyConfiguration assemblyConfig = AssemblyConfiguration.builder()
                .inline(Assembly.builder()
                        .file(AssemblyFile.builder()
                              .source(extraFile)
                              .outputDirectory(mavenDir)
                              .build())
                        .build())
                .build();

        final BuildConfiguration jKubeBuildConfiguration = BuildConfiguration.builder()
                .assembly(assemblyConfig)
                .dockerFile(dockerFile.getPath())
                .dockerFileFile(dockerFile)
                .build();

        // When
        File dockerArchiveFile = assemblyManager.createDockerTarArchive("test-image", configuration, jKubeBuildConfiguration, prefixedLogger, null);

        // Then
        assertNotNull(dockerArchiveFile);
        assertTrue(dockerArchiveFile.exists());
        assertTrue(dockerDirectory.isDirectory() && dockerDirectory.exists());
        assertTrue(buildOutputDir.isDirectory() && buildOutputDir.exists());
        assertTrue(buildDir.isDirectory() && buildDir.exists());
        assertTrue(workDir.isDirectory() && workDir.exists());
        assertTrue(tmpDir.isDirectory() && tmpDir.exists());
        assertTrue(new File(buildDir, "Dockerfile").exists());
        assertTrue(mavenDir.isDirectory() && mavenDir.exists());
        assertTrue(new File(mavenDir, extraFile.getName()).exists());
        assertTrue(jarCopiedInBuildDirs.exists());
    }
}

