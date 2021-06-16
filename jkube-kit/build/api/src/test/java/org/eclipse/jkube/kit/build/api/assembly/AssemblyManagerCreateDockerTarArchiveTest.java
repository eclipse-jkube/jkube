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
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.eclipse.jkube.kit.common.assertj.FileAssertions;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.common.JKubeConfiguration;

import mockit.Mocked;
import org.assertj.core.api.AbstractFileAssert;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class AssemblyManagerCreateDockerTarArchiveTest {

  private static final String DOCKERFILE_DEFAULT_FALLBACK_CONTENT = "FROM busybox\nCOPY /maven /maven/\nVOLUME [\"/maven\"]";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mocked
  private PrefixedLogger prefixedLogger;

  private AssemblyManager assemblyManager;
  private File baseDirectory;
  private File targetDirectory;

  @Before
  public void setUp() throws Exception {
    assemblyManager = AssemblyManager.getInstance();
    baseDirectory = temporaryFolder.getRoot();
    targetDirectory = temporaryFolder.newFolder("target");
  }

  @Test
  public void createChangedFilesArchive() throws IOException {
    // Given
    final JKubeConfiguration jKubeConfiguration = createJKubeConfiguration();
    final List<AssemblyFileEntry> entries = new ArrayList<>();
    final File assemblyDirectory = temporaryFolder.getRoot().toPath().resolve("target").resolve("docker").toFile();
    entries.add(AssemblyFileEntry.builder()
        .source(temporaryFolder.getRoot().toPath().resolve("target").resolve("test-0.1.0.jar").toFile())
        .dest(temporaryFolder.getRoot().toPath().resolve("target").resolve("docker").resolve("test-0.1.0.jar").toFile())
        .fileMode("0655")
        .build());
    // When
    final File result = assemblyManager.createChangedFilesArchive(
        entries, assemblyDirectory, "image-name", jKubeConfiguration);
    // Then
    assertThat(result)
        .exists()
        .isFile()
        .hasName("changed-files.tar")
        .hasSize(1536);
  }

  @Test
  public void withoutDockerfile() throws IOException {
    // Given
    final JKubeConfiguration jKubeConfiguration = createJKubeConfiguration();
    final BuildConfiguration buildConfiguration = BuildConfiguration.builder().build();

    // When
    File dockerArchiveFile = assemblyManager.createDockerTarArchive(
        "test-image", jKubeConfiguration, buildConfiguration, prefixedLogger, null);

    // Then
    assertTargetHasDockerDirectories("test-image");
    assertThat(dockerArchiveFile).isFile().hasName("docker-build.tar").hasSize(3072);
    assertDockerFile("test-image").hasContent(DOCKERFILE_DEFAULT_FALLBACK_CONTENT);
    assertBuildDirectoryFileTree("test-image").containsExactlyInAnyOrder(
        "Dockerfile",
        "maven",
        "maven/test-0.1.0.jar");
  }

  @Test
  public void withoutDockerfileAndFinalCustomizer() throws IOException {
    // Given
    final JKubeConfiguration jKubeConfiguration = createJKubeConfiguration();
    final BuildConfiguration buildConfiguration = BuildConfiguration.builder().build();
    final AtomicBoolean customized = new AtomicBoolean(false);
    final ArchiverCustomizer finalCustomizer = ac -> {
      customized.set(true);
      return ac;
    };

    // When
    File dockerArchiveFile = assemblyManager.createDockerTarArchive(
        "no-docker-file-and-customizer", jKubeConfiguration, buildConfiguration, prefixedLogger, finalCustomizer);

    // Then
    assertTargetHasDockerDirectories("no-docker-file-and-customizer");
    assertThat(dockerArchiveFile).isFile().hasName("docker-build.tar").hasSize(3072);
    assertDockerFile("no-docker-file-and-customizer").hasContent(DOCKERFILE_DEFAULT_FALLBACK_CONTENT);
    assertBuildDirectoryFileTree("no-docker-file-and-customizer").containsExactlyInAnyOrder(
        "Dockerfile",
        "maven",
        "maven/test-0.1.0.jar");
    assertThat(customized).isTrue();
  }

  @Test
  public void withoutDockerfileAndAlreadyExistingFileInAssemblyGetsOverwritten() throws IOException {
    final JKubeConfiguration jKubeConfiguration = createJKubeConfiguration();
    final BuildConfiguration buildConfiguration = BuildConfiguration.builder().build();
    File dockerArchiveFile;

    // When
    assemblyManager.createDockerTarArchive(
        "modified-image", jKubeConfiguration, buildConfiguration, prefixedLogger, null);
    // Modify file contents
    writeLineToFile(jKubeConfiguration.getProject().getArtifact(), "Modified content");
    dockerArchiveFile = assemblyManager.createDockerTarArchive(
        "modified-image", jKubeConfiguration, buildConfiguration, prefixedLogger, null);

    // Then
    assertTargetHasDockerDirectories("modified-image");
    assertThat(dockerArchiveFile).isFile().hasName("docker-build.tar").hasSize(4608);
    assertDockerFile("modified-image").hasContent(DOCKERFILE_DEFAULT_FALLBACK_CONTENT);
    assertBuildDirectoryFileTree("modified-image").containsExactlyInAnyOrder(
        "Dockerfile",
        "maven",
        "maven/test-0.1.0.jar");
    assertThat(resolveDockerBuild("modified-image").resolve("maven").resolve("test-0.1.0.jar"))
        .exists().isRegularFile()
        .hasContent("Modified content");
  }

  @Test
  public void withDockerfileInBaseDirectory() throws IOException {
    // Given
    final File dockerFile = new File(baseDirectory, "Dockerfile");
    writeLineToFile(dockerFile, "FROM openjdk:jre");
    final JKubeConfiguration configuration = createJKubeConfiguration();
    final BuildConfiguration jKubeBuildConfiguration = BuildConfiguration.builder()
        .dockerFileFile(dockerFile).dockerFile(dockerFile.getPath()).build();

    // When
    File dockerArchiveFile = assemblyManager.createDockerTarArchive("test-image", configuration, jKubeBuildConfiguration, prefixedLogger, null);

    // Then
    assertTargetHasDockerDirectories("test-image");
    assertThat(dockerArchiveFile).isFile().hasName("docker-build.tar").hasSize(5120);
    assertDockerFile("test-image").hasContent("FROM openjdk:jre\n");
    assertBuildDirectoryFileTree("test-image").containsExactlyInAnyOrder(
        "Dockerfile",
        "maven",
        "maven/Dockerfile",
        "maven/test-0.1.0.jar",
        "maven/target",
        "maven/target/test-0.1.0.jar");
  }

  @Test
  public void withDockerfileInBaseDirectoryAndAssemblyFile() throws IOException {
    // Given
    final File dockerFile = new File(baseDirectory, "Dockerfile");
    writeLineToFile(dockerFile, "FROM busybox");
    final File assemblyFile = temporaryFolder.newFile("extra-file-1.txt");
    writeLineToFile(assemblyFile, "HELLO");
    AssemblyConfiguration assemblyConfig = AssemblyConfiguration.builder()
        .inline(Assembly.builder()
            .file(AssemblyFile.builder()
                .source(assemblyFile)
                .outputDirectory(new File("."))
                .build())
            .build())
        .build();
    final JKubeConfiguration configuration = createJKubeConfiguration();
    final BuildConfiguration jKubeBuildConfiguration = BuildConfiguration.builder()
        .dockerFileFile(dockerFile).dockerFile(dockerFile.getPath())
        .assembly(assemblyConfig)
        .build();

    // When
    final File dockerArchiveFile = assemblyManager.createDockerTarArchive(
        "dockerfile-and-assembly-file", configuration, jKubeBuildConfiguration, prefixedLogger, null);

    // Then
    assertTargetHasDockerDirectories("dockerfile-and-assembly-file");
    assertThat(dockerArchiveFile).isFile().hasName("docker-build.tar").hasSize(6144);
    assertDockerFile("dockerfile-and-assembly-file").hasContent("FROM busybox\n");
    assertBuildDirectoryFileTree("dockerfile-and-assembly-file").containsExactlyInAnyOrder(
        "Dockerfile",
        "maven",
        "maven/Dockerfile",
        "maven/test-0.1.0.jar",
        "maven/extra-file-1.txt",
        "maven/target",
        "maven/target/test-0.1.0.jar");
  }

  @Test
  public void withDockerfileInBaseDirectoryAndDockerInclude() throws IOException {
    // Given
    final File dockerFile = new File(baseDirectory, "Dockerfile");
    writeLineToFile(dockerFile, "FROM openjdk:jre");
    writeLineToFile(new File(baseDirectory, ".jkube-dockerinclude"), "**/*.txt");
    writeLineToFile(new File(targetDirectory, "ill-be-included.txt"), "Hello");
    writeLineToFile(new File(targetDirectory, "ill-wont-be-included"), "Hello");
    final JKubeConfiguration configuration = createJKubeConfiguration();
    final BuildConfiguration jKubeBuildConfiguration = BuildConfiguration.builder()
        .dockerFileFile(dockerFile).dockerFile(dockerFile.getPath()).build();

    // When
    final File dockerArchiveFile = assemblyManager.createDockerTarArchive(
        "test-image", configuration, jKubeBuildConfiguration, prefixedLogger, null);

    // Then
    assertTargetHasDockerDirectories("test-image");
    assertThat(dockerArchiveFile).isFile().hasName("docker-build.tar").hasSize(4608);
    assertDockerFile("test-image").hasContent("FROM openjdk:jre\n");
    assertBuildDirectoryFileTree("test-image").containsExactlyInAnyOrder(
        "Dockerfile",
        "maven",
        "maven/test-0.1.0.jar",
        "maven/target",
        "maven/target/ill-be-included.txt");
  }

  @Test
  public void withDockerfileInBaseDirectoryAndDockerExclude() throws IOException {
    // Given
    final File dockerFile = new File(baseDirectory, "Dockerfile");
    writeLineToFile(dockerFile, "FROM openjdk:jre");
    writeLineToFile(new File(baseDirectory, ".jkube-dockerexclude"), "**/*.txt");
    writeLineToFile(new File(targetDirectory, "ill-wont-be-included.txt"), "Hello");
    writeLineToFile(new File(targetDirectory, "ill-be-included"), "Hello");
    final JKubeConfiguration configuration = createJKubeConfiguration();
    final BuildConfiguration jKubeBuildConfiguration = BuildConfiguration.builder()
        .dockerFileFile(dockerFile).dockerFile(dockerFile.getPath()).build();

    // When
    final File dockerArchiveFile = assemblyManager.createDockerTarArchive(
        "test-image", configuration, jKubeBuildConfiguration, prefixedLogger, null);

    // Then
    assertTargetHasDockerDirectories("test-image");
    assertThat(dockerArchiveFile).isFile().hasName("docker-build.tar").hasSize(6144);
    assertDockerFile("test-image").hasContent("FROM openjdk:jre\n");
    assertBuildDirectoryFileTree("test-image").containsExactlyInAnyOrder(
        "Dockerfile",
        "maven",
        "maven/Dockerfile",
        "maven/test-0.1.0.jar",
        "maven/target",
        "maven/target/test-0.1.0.jar",
        "maven/target/ill-be-included");
  }

  @Test
  public void withDockerfileInBaseDirectoryAndDockerIgnore() throws IOException {
    // Given
    final File dockerFile = new File(baseDirectory, "Dockerfile");
    writeLineToFile(dockerFile, "FROM openjdk:jre");
    writeLineToFile(new File(baseDirectory, ".jkube-dockerignore"), "**/*.txt\ntarget/ill-be-ignored-too");
    writeLineToFile(new File(targetDirectory, "ill-be-ignored.txt"), "Hello");
    writeLineToFile(new File(targetDirectory, "ill-be-ignored-too"), "Hello");
    writeLineToFile(new File(targetDirectory, "i-wont-be-ignored"), "Hello");
    final JKubeConfiguration configuration = createJKubeConfiguration();
    final BuildConfiguration jKubeBuildConfiguration = BuildConfiguration.builder()
        .dockerFileFile(dockerFile).dockerFile(dockerFile.getPath()).build();

    // When
    File dockerArchiveFile = assemblyManager.createDockerTarArchive("test-image", configuration, jKubeBuildConfiguration, prefixedLogger, null);

    // Then
    assertTargetHasDockerDirectories("test-image");
    assertThat(dockerArchiveFile).isFile().hasName("docker-build.tar").hasSize(6144);
    assertDockerFile("test-image").hasContent("FROM openjdk:jre\n");
    assertBuildDirectoryFileTree("test-image").containsExactlyInAnyOrder(
        "Dockerfile",
        "maven",
        "maven/Dockerfile",
        "maven/test-0.1.0.jar",
        "maven/target",
        "maven/target/test-0.1.0.jar",
        "maven/target/i-wont-be-ignored");
  }

  private void assertTargetHasDockerDirectories(String imageDirName) {
    assertThat(targetDirectory.toPath().resolve("docker").resolve(imageDirName))
        .isDirectory().exists()
        .isDirectoryContaining(p -> p.toFile().isDirectory() && p.toFile().getName().equals("build"))
        .isDirectoryContaining(p -> p.toFile().isDirectory() && p.toFile().getName().equals("work"))
        .isDirectoryContaining(p -> p.toFile().isDirectory() && p.toFile().getName().equals("tmp"))
        .extracting(p -> p.resolve("build").resolve("Dockerfile").toFile())
        .satisfies(p -> assertThat(p).exists().isFile());
  }

  private Path resolveDockerBuild(String imageDirName) {
    return targetDirectory.toPath().resolve("docker").resolve(imageDirName).resolve("build");
  }

  private AbstractFileAssert<?> assertDockerFile(String imageDirName) {
    return assertThat(resolveDockerBuild(imageDirName).resolve("Dockerfile").toFile()).isFile().exists();
  }

  private AbstractListAssert<ListAssert<String>, List<? extends String>, String, ObjectAssert<String>> assertBuildDirectoryFileTree(
      String imageDirName) throws IOException {
    return FileAssertions.assertThat(resolveDockerBuild(imageDirName).toFile()).fileTree();
  }

  private static void writeLineToFile(File file, String line) throws IOException {
    if (!file.exists()) {
      assertTrue(file.createNewFile());
    }
    PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8.name());
    writer.println(line);
    writer.close();
  }

  private JKubeConfiguration createJKubeConfiguration() throws IOException {
    return JKubeConfiguration.builder()
        .project(JavaProject.builder()
            .groupId("org.eclipse.jkube")
            .artifactId("test")
            .packaging("jar")
            .version("0.1.0")
            .baseDirectory(baseDirectory)
            .buildDirectory(targetDirectory)
            .artifact(createEmptyArtifact())
            .build())
        .outputDirectory("target/docker")
        .sourceDirectory("src/main/docker")
        .build();
  }

  private File createEmptyArtifact() throws IOException {
    File emptyArtifact = new File(targetDirectory, "test-0.1.0.jar");
    assertTrue(emptyArtifact.createNewFile());
    return emptyArtifact;
  }
}
