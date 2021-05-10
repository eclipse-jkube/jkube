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
package org.eclipse.jkube.kit.common.archive;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.assertj.FileAssertions;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.common.archive.AssemblyFileSetUtils.resolveSourceDirectory;

public class AssemblyFileSetUtilsProcessAssemblyFileSetTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File baseDirectory;
  private File sourceDirectory;
  private File outputDirectory;

  @Before
  public void setUp() throws Exception {
    baseDirectory = temp.newFolder("base");
    sourceDirectory = new File(baseDirectory, "source-directory");
    final List<File> sourceSubdirectories = Stream.of("one", "two", "three")
        .map(s -> new File(sourceDirectory, s)).collect(Collectors.toList());
    for (File directory : Stream.concat(Stream.of(baseDirectory, sourceDirectory), sourceSubdirectories.stream()).collect(Collectors.toList())) {
      FileUtils.forceMkdir(directory);
      populateSampleFiles(directory);
    }
    outputDirectory = temp.newFolder("output");
  }

  @After
  public void tearDown() {
    outputDirectory = null;
    sourceDirectory = null;
    baseDirectory = null;
  }

  private static void populateSampleFiles(File baseDirectory) throws IOException {
    for (String fileName : new String[]{"1.txt", "3.other", "37"}) {
      FileUtils.touch(new File(baseDirectory, fileName));
    }
  }

  @Test
  public void resolveSourceDirectoryIsAbsoluteShouldReturnAbsolute() throws Exception {
    // Given
    final File directory = temp.newFolder("absolute-path-out-of-base");
    final AssemblyFileSet afs = AssemblyFileSet.builder().directory(directory).build();
    // When
    final File result = resolveSourceDirectory(baseDirectory, afs);
    // Then
    assertThat(result)
        .isEqualTo(directory)
        .isAbsolute()
        .satisfies(f -> assertThat(baseDirectory.toPath().relativize(result.toPath()).toString()).startsWith(".."));
  }

  @Test
  public void resolveSourceDirectoryIsRelativeShouldReturnRelative() {
    // Given
    final File relativeDirectory = new File("source-directory");
    final AssemblyFileSet afs = AssemblyFileSet.builder().directory(relativeDirectory).build();
    // When
    final File result = resolveSourceDirectory(baseDirectory, afs);
    // Then
    assertThat(result)
        .isEqualTo(new File(baseDirectory, "source-directory"))
        .isAbsolute()
        .satisfies(f -> assertThat(baseDirectory.toPath().relativize(result.toPath())).hasToString("source-directory"));
  }

  @Test
  public void assemblyFileSetHasNoDirectoryShouldThrowException() {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .name("deployments")
        .build();
    // When
    final NullPointerException result = Assert.assertThrows(NullPointerException.class, () ->
      AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac)
    );
    // Then
    assertThat(result)
        .hasMessage("Assembly FileSet directory is required");
  }

  @Test
  public void assemblyConfigurationHasNoTargetDirShouldThrowException() {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .directory(sourceDirectory)
        .build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder().build();
    // When
    final NullPointerException result = Assert.assertThrows(NullPointerException.class, () ->
        AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac)
    );
    // Then
    assertThat(result)
        .hasMessage("Assembly Configuration target dir is required");
  }

  /**
   * Has AssemblyFileSet#directory and AssemblyConfiguration#targetDir options.
   *
   * Should copy the AssemblyFileSet#directory to the outputDirectory in a subdirectory named as the AssemblyConfiguration#targetDir.
   *
   * n.b. this is the only case where the source directory and not its contents is copied.
   */
  @Test
  public void minimumRequiredFields() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .directory(sourceDirectory)
        .build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .targetDir("deployments")
        .build();
    // When
    final List<AssemblyFileEntry> result = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(result).hasSize(16);
    FileAssertions.assertThat(new File(outputDirectory, "deployments"))
        .exists()
        .fileTree()
        .containsExactlyInAnyOrder(
            "source-directory", "source-directory/1.txt", "source-directory/3.other", "source-directory/37",
            "source-directory/one", "source-directory/one/1.txt", "source-directory/one/3.other", "source-directory/one/37",
            "source-directory/two", "source-directory/two/1.txt", "source-directory/two/3.other", "source-directory/two/37",
            "source-directory/three", "source-directory/three/1.txt", "source-directory/three/3.other", "source-directory/three/37"
        );
  }

  /**
   * Has AssemblyFileSet#directory and AssemblyConfiguration#targetDir options.
   *
   * Source directory doesn't exist
   *
   * Should do nothing.
   */
  @Test
  public void sourceDoesNotExist() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .directory(new File(sourceDirectory, "non-existent"))
        .build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .targetDir("deployments")
        .build();
    // When
    final List<AssemblyFileEntry> result = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(result).isEmpty();
    FileAssertions.assertThat(new File(outputDirectory, "deployments")).doesNotExist();
  }

  /**
   * Has AssemblyFileSet with directory and outputDirectory relative path resolving to self.
   * Has AssemblyConfiguration targetDir.
   *
   * Should copy <b>contents</b> of AssemblyFileSet#directory to the outputDirectory in a subdirectory named as the
   * AssemblyConfiguration#targetDir.
   */
  @Test
  public void fileSetDirectoryAndOutputDirectoryResolvingToSelf() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .directory(sourceDirectory)
        .outputDirectory(new File("."))
        .build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .name("NotImportant")
        .targetDir("deployments")
        .build();
    // When
    final List<AssemblyFileEntry> result = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(result).hasSize(16);
    FileAssertions.assertThat(new File(outputDirectory, "deployments"))
        .exists()
        .fileTree()
        .containsExactlyInAnyOrder(
            "1.txt", "3.other", "37",
            "one", "one/1.txt", "one/3.other", "one/37",
            "two", "two/1.txt", "two/3.other", "two/37",
            "three", "three/1.txt", "three/3.other", "three/37"
        )
        .doesNotContainSequence("source-directory");
  }

  /**
   * Has AssemblyFileSet directory and absolute outputDirectory.
   * Has AssemblyConfiguration targetDir.
   *
   * Should copy contents of AssemblyFileSet#directory to the absoluteOutputDirectory.
   */
  @Test
  public void fileSetDirectoryAndAbsoluteOutputDirectory() throws Exception {
    // Given
    final File absoluteOutputDirectory = temp.newFolder("absolute-output");
    assertThat(absoluteOutputDirectory).isEmptyDirectory();
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .directory(sourceDirectory)
        .outputDirectory(absoluteOutputDirectory)
        .build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .targetDir("/deployments")
        .build();
    // When
    final List<AssemblyFileEntry> result = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(result).hasSize(16);
    FileAssertions.assertThat(new File(outputDirectory, "deployments")).doesNotExist();
    FileAssertions.assertThat(absoluteOutputDirectory)
        .exists()
        .fileTree()
        .containsExactlyInAnyOrder(
            "1.txt", "3.other", "37",
            "one", "one/1.txt", "one/3.other", "one/37",
            "two", "two/1.txt", "two/3.other", "two/37",
            "three", "three/1.txt", "three/3.other", "three/37"
        )
        .doesNotContainSequence("source-directory");
  }

  /**
   * No options provided except of the AssemblyFileSet#directory, relative outputDirectory and AssemblyConfiguration#targetDir.
   *
   * Should copy contents of AssemblyFileSet#directory to the outputDirectory in a relative subdirectory with path
   * composed of AssemblyConfiguration#targetDir and the relative outputDirectory.
   */
  @Test
  public void fileSetDirectoryAndRelativeOutputDirectory() throws Exception {
    // Given
    final File relativeOutputDirectory = new File("relative-output");
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .directory(sourceDirectory)
        .outputDirectory(relativeOutputDirectory)
        .build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .name("MyNameIsAl")
        .targetDir("/deployments/")
        .build();
    // When
    final List<AssemblyFileEntry> result = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(result).hasSize(16);
    FileAssertions.assertThat(new File(outputDirectory, "deployments"))
        .exists()
        .fileTree()
        .containsExactlyInAnyOrder(
            "relative-output", "relative-output/1.txt", "relative-output/3.other", "relative-output/37",
            "relative-output/one", "relative-output/one/1.txt", "relative-output/one/3.other", "relative-output/one/37",
            "relative-output/two", "relative-output/two/1.txt", "relative-output/two/3.other", "relative-output/two/37",
            "relative-output/three", "relative-output/three/1.txt", "relative-output/three/3.other", "relative-output/three/37"
        )
        .doesNotContainSequence("source-directory");
  }

  /**
   * Has AssemblyFileSet#directory and includes for files in several hierarchic levels.
   * Has AssemblyConfiguration targetDir.
   *
   * Should copy contents of AssemblyFileSet#directory to the outputDirectory in a subdirectory named as the AssemblyConfiguration#targetDir.
   */
  @Test
  public void hierarchicalInclude() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .directory(sourceDirectory)
        .include("1.txt")
        .include("one/1.txt")
        .include("two")
        .build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .name("deployments")
        .targetDir("/deployments")
        .build();
    // When
    final List<AssemblyFileEntry> result = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(result).hasSize(6);
    FileAssertions.assertThat(new File(outputDirectory, "deployments"))
        .exists()
        .fileTree()
        .containsExactlyInAnyOrder(
            "source-directory", "source-directory/1.txt",
            "source-directory/one", "source-directory/one/1.txt",
            "source-directory/two", "source-directory/two/1.txt", "source-directory/two/3.other", "source-directory/two/37"
        );
  }

  /**
   * AssemblyFileSet#directory, relative outputDirectory and includes for files in several hierarchic levels.
   * Has AssemblyConfiguration targetDir.
   *
   * Should copy contents of AssemblyFileSet#directory to the outputDirectory in a subdirectory named as the AssemblyConfiguration#targetDir.
   */
  @Test
  public void hierarchicalIncludeInRelativeDirectory() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .directory(sourceDirectory)
        .outputDirectory(new File("relative"))
        .include("37")
        .include("one/1.txt")
        .include("three")
        .build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .targetDir("maven")
        .build();
    // When
    final List<AssemblyFileEntry> result = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(result).hasSize(6);
    FileAssertions.assertThat(new File(outputDirectory, "maven"))
        .exists()
        .fileTree()
        .containsExactlyInAnyOrder(
            "relative", "relative/37",
            "relative/one", "relative/one/1.txt",
            "relative/three", "relative/three/1.txt", "relative/three/3.other", "relative/three/37"
        );
  }

  @Test
  public void wildcardInclude() throws IOException {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .directory(sourceDirectory)
        .include("*.txt")
        .fileMode("0766")
        .build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .name("deployments")
        .targetDir("/deployments")
        .build();
    // When
    final List<AssemblyFileEntry> result = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(result).hasSize(1);
    FileAssertions.assertThat(new File(outputDirectory, "deployments"))
        .exists()
        .fileTree()
        .containsExactlyInAnyOrder(
            "source-directory", "source-directory/1.txt"
        );
  }

  @Test
  public void wildcardIncludeAcrossDirectories() throws IOException {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .directory(sourceDirectory)
        .include("**.txt")
        .fileMode("0766")
        .build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .name("deployments")
        .targetDir("/deployments")
        .build();
    // When
    final List<AssemblyFileEntry> result = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(result).hasSize(4);
    FileAssertions.assertThat(new File(outputDirectory, "deployments"))
        .exists()
        .fileTree()
        .containsExactlyInAnyOrder(
            "source-directory", "source-directory/1.txt",
            "source-directory/one", "source-directory/one/1.txt",
            "source-directory/two", "source-directory/two/1.txt",
            "source-directory/three", "source-directory/three/1.txt"
        );
  }

  @Test
  public void wildcardHierarchicalInclude() throws IOException {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .directory(sourceDirectory)
        .include("**/*.txt")
        .fileMode("0766")
        .build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .name("deployments")
        .targetDir("/deployments")
        .build();
    // When
    final List<AssemblyFileEntry> result = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(result).hasSize(3);
    FileAssertions.assertThat(new File(outputDirectory, "deployments"))
        .exists()
        .fileTree()
        .containsExactlyInAnyOrder(
            "source-directory",
            "source-directory/one", "source-directory/one/1.txt",
            "source-directory/two", "source-directory/two/1.txt",
            "source-directory/three", "source-directory/three/1.txt"
        );
  }

  @Test
  public void wildcardIncludeAndExcludeAcrossDirectories() throws IOException {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .directory(sourceDirectory)
        .include("**.*")
        .exclude("**.txt")
        .fileMode("0766")
        .build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .name("deployments")
        .targetDir("/deployments")
        .build();
    // When
    final List<AssemblyFileEntry> result = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(result).hasSize(4);
    FileAssertions.assertThat(new File(outputDirectory, "deployments"))
        .exists()
        .fileTree()
        .containsExactlyInAnyOrder(
            "source-directory", "source-directory/3.other",
            "source-directory/one", "source-directory/one/3.other",
            "source-directory/two", "source-directory/two/3.other",
            "source-directory/three", "source-directory/three/3.other"
        );
  }

  @Test
  public void directoryExcludes() throws Exception {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .directory(sourceDirectory)
        .fileMode("0764")
        .exclude("one/**")
        .exclude("one")
        .exclude("two{/**,}")
        .build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .name("deployments")
        .targetDir("/deployments")
        .build();
    // When
    final List<AssemblyFileEntry> result = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(result).hasSize(8);
    FileAssertions.assertThat(new File(outputDirectory, "deployments"))
        .exists()
        .fileTree()
        .containsExactlyInAnyOrder(
            "source-directory", "source-directory/1.txt", "source-directory/3.other", "source-directory/37",
            "source-directory/three", "source-directory/three/1.txt", "source-directory/three/3.other", "source-directory/three/37"
        );
  }

  @Test
  public void withRelativeSourceAndDirectoryExcludes() throws Exception {
    // Given
    final File quickstartDirectory = new File(sourceDirectory, "quickstarts/directory");
    FileUtils.forceMkdir(quickstartDirectory);
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .directory(new File(quickstartDirectory, "../../"))
        .outputDirectory(new File("."))
        .fileMode("0764")
        .exclude("quickstarts{/**,}")
        .exclude("three/**")
        .build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .name("deployments")
        .targetDir("/deployments")
        .build();
    // When
    final List<AssemblyFileEntry> result = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(result).hasSize(13);
    FileAssertions.assertThat(new File(outputDirectory, "deployments"))
        .exists()
        .fileTree()
        .containsExactlyInAnyOrder(
            "1.txt", "3.other", "37",
            "one", "one/1.txt", "one/3.other", "one/37",
            "two", "two/1.txt", "two/3.other", "two/37",
            "three"
        );
  }

}