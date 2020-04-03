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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.eclipse.jkube.kit.build.core.JKubeBuildContext;
import org.eclipse.jkube.kit.build.core.config.JKubeAssemblyConfiguration;
import org.eclipse.jkube.kit.build.core.config.JKubeBuildConfiguration;
import org.eclipse.jkube.kit.common.JKubeProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.eclipse.jkube.kit.config.image.build.DockerFileBuilder;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DockerAssemblyManagerTest {

    @Mocked
    private PrefixedLogger prefixedLogger;

    @Tested
    private DockerAssemblyManager assemblyManager;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testNoAssembly() {
        JKubeBuildConfiguration buildConfig = new JKubeBuildConfiguration.Builder().build();
        JKubeAssemblyConfiguration assemblyConfig = buildConfig.getAssemblyConfiguration();

        DockerFileBuilder builder = assemblyManager.createDockerFileBuilder(buildConfig, assemblyConfig);
        String content = builder.content();

        assertFalse(content.contains("COPY"));
        assertFalse(content.contains("VOLUME"));
    }

    @Test
    public void assemblyFiles(@Injectable final JKubeBuildContext mojoParams,
                              @Injectable final JKubeProject project,
                              @Injectable final File assembly) {


        new Expectations() {{
            mojoParams.getProject();
            project.getBaseDirectory();

            project.getBuildDirectory();
            result = "target/";
            result = ".";

        }};

        JKubeBuildConfiguration buildConfig = createBuildConfig();

        assertTrue(new File("./target/testImage/build/maven").mkdirs());
        AssemblyFiles assemblyFiles = assemblyManager.getAssemblyFiles("testImage", buildConfig, mojoParams, prefixedLogger);
        assertNotNull(assemblyFiles);
    }

    @Test
    public void testCopyValidVerifyGivenDockerfile(@Injectable final KitLogger logger) throws IOException {
        JKubeBuildConfiguration buildConfig = createBuildConfig();

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
        JKubeBuildConfiguration buildConfig = createBuildConfig();

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
        JKubeBuildConfiguration buildConfig = createBuildConfig();

        assemblyManager.verifyGivenDockerfile(
                new File(getClass().getResource("/docker/Dockerfile_assembly_verify_copy_chown_valid.test").getPath()),
                buildConfig,
                new Properties(),
                logger);

        new Verifications() {{
            logger.warn(anyString, (Object []) any); times = 0;
        }};
    }

    private JKubeBuildConfiguration createBuildConfig() {
        return new JKubeBuildConfiguration.Builder()
                .assembly(new JKubeAssemblyConfiguration.Builder()
                        .descriptorRef("artifact")
                        .build())
                .build();
    }

    @Test
    public void testEnsureThatArtifactFileIsSetWithProjectArtifactSet() throws IOException {
        // Given
        JKubeProject project = JKubeProject.builder()
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
        JKubeProject project = JKubeProject.builder()
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
        JKubeProject project = JKubeProject.builder().build();

        // When
        File artifactFile = assemblyManager.ensureThatArtifactFileIsSet(project);

        // Then
        assertNull(artifactFile);
    }

    @Test
    public void testCreateDockerTarArchiveWithoutDockerfile() throws IOException {
        // Given
        File targetFolder = temporaryFolder.newFolder("target");
        File finalArtifactFile = new File(targetFolder, "test-0.1.0.jar");
        assertTrue(finalArtifactFile.createNewFile());
        File outputDirectory = new File(targetFolder, "docker");

        final JKubeBuildContext jKubeBuildContext = new JKubeBuildContext.Builder()
                .project(JKubeProject.builder()
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
        final JKubeBuildConfiguration jKubeBuildConfiguration = new JKubeBuildConfiguration.Builder()
                .build();

        // When
        File dockerArchiveFile = assemblyManager.createDockerTarArchive("test-image", jKubeBuildContext, jKubeBuildConfiguration, prefixedLogger, null);

        // Then
        assertNotNull(dockerArchiveFile);
        assertTrue(dockerArchiveFile.exists());
        assertEquals(2560, dockerArchiveFile.length());
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


        final JKubeBuildContext jKubeBuildContext = new JKubeBuildContext.Builder()
                .project(JKubeProject.builder()
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
        final JKubeBuildConfiguration jKubeBuildConfiguration = new JKubeBuildConfiguration.Builder()
                .dockerFileDir(baseProjectDir.getPath())
                .dockerFile(dockerFile.getPath())
                .dockerFileFile(dockerFile)
                .build();


        // When
        File dockerArchiveFile = assemblyManager.createDockerTarArchive("test-image", jKubeBuildContext, jKubeBuildConfiguration, prefixedLogger, null);

        // Then
        assertNotNull(dockerArchiveFile);
        assertTrue(dockerArchiveFile.exists());
        assertEquals(2560, dockerArchiveFile.length());
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
        File assemblyNameDirInBuild = new File(buildDir, "maven");
        assertTrue(assemblyNameDirInBuild.isDirectory() && assemblyNameDirInBuild.exists());
    }

    private void writeASimpleDockerfile(File dockerFile) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter(dockerFile, "UTF-8");
        writer.println("FROM openjdk:jre");
        writer.close();
    }

}

