/*
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFile;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.assertj.ArchiveAssertions;
import org.eclipse.jkube.kit.common.assertj.FileAssertions;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.assertj.core.api.AbstractFileAssert;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.apache.commons.io.FilenameUtils.separatorsToSystem;
import static org.assertj.core.api.Assertions.assertThat;

class AssemblyManagerCreateDockerTarArchiveTest {

  private static final String DOCKERFILE_DEFAULT_FALLBACK_CONTENT = "FROM busybox\nCOPY /jkube-generated-layer-final-artifact/maven /maven/\nVOLUME [\"/maven\"]";

  @TempDir
  Path temporaryFolder;

  private PrefixedLogger prefixedLogger;
  private AssemblyManager assemblyManager;
  private File baseDirectory;
  private File targetDirectory;

  @BeforeEach
  void setUp() throws IOException {
    prefixedLogger = new PrefixedLogger("test-prefix", new KitLogger.SilentLogger());
    assemblyManager = AssemblyManager.getInstance();
    baseDirectory = temporaryFolder.toFile();
    targetDirectory = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
  }

  @Test
  void createChangedFilesArchive() throws IOException {
    // Given
    final JKubeConfiguration jKubeConfiguration = createJKubeConfiguration();
    final List<AssemblyFileEntry> entries = new ArrayList<>();
    final File assemblyDirectory = temporaryFolder.resolve("target").resolve("docker").toFile();
    entries.add(AssemblyFileEntry.builder()
        .source(temporaryFolder.resolve("target").resolve("test-0.1.0.jar").toFile())
        .dest(temporaryFolder.resolve("target").resolve("docker").resolve("test-0.1.0.jar").toFile())
        .fileMode("0655")
        .build());
    // When
    final File result = assemblyManager.createChangedFilesArchive(
        entries, assemblyDirectory, "image-name", jKubeConfiguration);
    // Then
    ArchiveAssertions.assertThat(result)
        .isFile()
        .hasName("changed-files.tar")
        .hasSameContentAsDirectory(getExpectedDirectory("changed-files.tar"));
  }

  @Test
  void withoutDockerfile() throws IOException {
    // Given
    final JKubeConfiguration jKubeConfiguration = createJKubeConfiguration();
    final BuildConfiguration buildConfiguration = BuildConfiguration.builder().build();

    // When
    File dockerArchiveFile = assemblyManager.createDockerTarArchive(
        "test-image", jKubeConfiguration, buildConfiguration, prefixedLogger, null);

    // Then
    assertTargetHasDockerDirectories("test-image");
    ArchiveAssertions.assertThat(dockerArchiveFile)
        .isFile()
        .hasName("docker-build.tar")
        .hasSameContentAsDirectory(getExpectedDirectory("without-dockerfile"));
    assertDockerFile("test-image").hasContent(DOCKERFILE_DEFAULT_FALLBACK_CONTENT);
    assertBuildDirectoryFileTree("test-image").containsExactlyInAnyOrder(
        "Dockerfile",
        "jkube-generated-layer-final-artifact",
        separatorsToSystem("jkube-generated-layer-final-artifact/maven"),
        separatorsToSystem("jkube-generated-layer-final-artifact/maven/test-0.1.0.jar"),
        "maven"
    );
  }

  @Test
  void withoutDockerfileAndFinalCustomizer() throws IOException {
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
    ArchiveAssertions.assertThat(dockerArchiveFile)
        .isFile()
        .hasName("docker-build.tar")
        .hasSameContentAsDirectory(getExpectedDirectory("without-dockerfile-and-final-customizer"));
    assertDockerFile("no-docker-file-and-customizer").hasContent(DOCKERFILE_DEFAULT_FALLBACK_CONTENT);
    assertBuildDirectoryFileTree("no-docker-file-and-customizer").containsExactlyInAnyOrder(
        "Dockerfile",
        "jkube-generated-layer-final-artifact",
        separatorsToSystem("jkube-generated-layer-final-artifact/maven"),
        separatorsToSystem("jkube-generated-layer-final-artifact/maven/test-0.1.0.jar"),
        "maven");
    assertThat(customized).isTrue();
  }

  @Test
  void withoutDockerfileAndAlreadyExistingFileInAssemblyGetsOverwritten() throws IOException {
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
    ArchiveAssertions.assertThat(dockerArchiveFile)
        .isFile()
        .hasName("docker-build.tar")
        .hasSameContentAsDirectory(getExpectedDirectory("without-dockerfile-and-already-existing-file-in-assembly-gets-overwritten"));
    assertDockerFile("modified-image").hasContent(DOCKERFILE_DEFAULT_FALLBACK_CONTENT);
    assertBuildDirectoryFileTree("modified-image").containsExactlyInAnyOrder(
        "Dockerfile",
        "jkube-generated-layer-final-artifact",
        separatorsToSystem("jkube-generated-layer-final-artifact/maven"),
        separatorsToSystem("jkube-generated-layer-final-artifact/maven/test-0.1.0.jar"),
        "maven");
    assertThat(resolveDockerBuild("modified-image")
        .resolve("jkube-generated-layer-final-artifact").resolve("maven").resolve("test-0.1.0.jar"))
        .exists().isRegularFile()
        .hasContent("Modified content");
  }

  @Test
  void withDockerfileInBaseDirectory() throws IOException {
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
    ArchiveAssertions.assertThat(dockerArchiveFile)
        .isFile()
        .hasName("docker-build.tar")
        .hasSameContentAsDirectory(getExpectedDirectory("with-dockerfile-in-base-directory"));
    assertDockerFile("test-image").hasContent("FROM openjdk:jre\n");
    assertBuildDirectoryFileTree("test-image").containsExactlyInAnyOrder(
        "Dockerfile",
        "maven",
        separatorsToSystem("maven/Dockerfile"),
        separatorsToSystem("maven/test-0.1.0.jar"),
        separatorsToSystem("maven/target"),
        separatorsToSystem("maven/target/test-0.1.0.jar"));
  }

  @Test
  void withDockerfileInBaseDirectoryAndAssemblyFile() throws IOException {
    // Given
    final File dockerFile = new File(baseDirectory, "Dockerfile");
    writeLineToFile(dockerFile, "FROM busybox");
    final File assemblyFile = Files.createFile(temporaryFolder.resolve("extra-file-1.txt")).toFile();
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
    ArchiveAssertions.assertThat(dockerArchiveFile)
        .isFile()
        .hasName("docker-build.tar")
        .hasSameContentAsDirectory(getExpectedDirectory("with-dockerfile-in-base-directory-and-assembly-file"));
    assertDockerFile("dockerfile-and-assembly-file").hasContent("FROM busybox\n");
    assertBuildDirectoryFileTree("dockerfile-and-assembly-file").containsExactlyInAnyOrder(
        "Dockerfile",
        "maven",
        separatorsToSystem("maven/Dockerfile"),
        separatorsToSystem("maven/test-0.1.0.jar"),
        separatorsToSystem("maven/extra-file-1.txt"),
        separatorsToSystem("maven/target"),
        separatorsToSystem("maven/target/test-0.1.0.jar"));
  }

  @Test
  void withDockerfileInBaseDirectoryAndDockerinclude() throws IOException {
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
    ArchiveAssertions.assertThat(dockerArchiveFile)
        .isFile()
        .hasName("docker-build.tar")
        .hasSameContentAsDirectory(getExpectedDirectory("with-dockerfile-in-base-directory-and-dockerinclude"));
    assertDockerFile("test-image").hasContent("FROM openjdk:jre\n");
    assertBuildDirectoryFileTree("test-image").containsExactlyInAnyOrder(
        "Dockerfile",
        "maven",
        separatorsToSystem("maven/test-0.1.0.jar"),
        separatorsToSystem("maven/target"),
        separatorsToSystem("maven/target/ill-be-included.txt"));
  }

  @Test
  void withDockerfileInBaseDirectoryAndDockerexclude() throws IOException {
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
    ArchiveAssertions.assertThat(dockerArchiveFile)
        .isFile()
        .hasName("docker-build.tar")
        .hasSameContentAsDirectory(getExpectedDirectory("with-dockerfile-in-base-directory-and-dockerexclude"));
    assertDockerFile("test-image").hasContent("FROM openjdk:jre\n");
    assertBuildDirectoryFileTree("test-image").containsExactlyInAnyOrder(
        "Dockerfile",
        "maven",
        separatorsToSystem("maven/Dockerfile"),
        separatorsToSystem("maven/test-0.1.0.jar"),
        separatorsToSystem("maven/target"),
        separatorsToSystem("maven/target/test-0.1.0.jar"),
        separatorsToSystem("maven/target/ill-be-included")
    );
  }

  @Test
  void withDockerfileInBaseDirectoryAndDockerignore() throws IOException {
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
    ArchiveAssertions.assertThat(dockerArchiveFile)
        .isFile()
        .hasName("docker-build.tar")
        .hasSameContentAsDirectory(getExpectedDirectory("with-dockerfile-in-base-directory-and-dockerignore"));
    assertDockerFile("test-image").hasContent("FROM openjdk:jre\n");
    assertBuildDirectoryFileTree("test-image").containsExactlyInAnyOrder(
        "Dockerfile",
        "maven",
        separatorsToSystem("maven/Dockerfile"),
        separatorsToSystem("maven/test-0.1.0.jar"),
        separatorsToSystem("maven/target"),
        separatorsToSystem("maven/target/test-0.1.0.jar"),
        separatorsToSystem("maven/target/i-wont-be-ignored"));
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
      assertThat(file.createNewFile()).isTrue();
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
    assertThat(emptyArtifact.createNewFile()).isTrue();
    return emptyArtifact;
  }

  private static File getExpectedDirectory(String dir) {
    return new File(AssemblyManagerCreateDockerTarArchiveTest.class.getResource(
        String.format("/assembly/assembly-manager-create-docker-tar-archive/%s", dir)).getFile()
    );
  }
}
