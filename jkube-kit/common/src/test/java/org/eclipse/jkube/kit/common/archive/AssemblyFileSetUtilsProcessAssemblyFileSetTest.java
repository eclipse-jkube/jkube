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
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.eclipse.jkube.kit.common.archive.AssemblyFileSetUtils.resolveSourceDirectory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;

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
      assertThat(new File(baseDirectory, fileName).createNewFile(), equalTo(true));
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
    assertThat(result, equalTo(directory));
    final Path relativeToBase = baseDirectory.toPath().relativize(result.toPath());
    assertThat(relativeToBase.toString(), startsWith(".."));
  }

  @Test
  public void resolveSourceDirectoryIsRelativeShouldReturnRelative() {
    // Given
    final File relativeDirectory = new File("source-directory");
    final AssemblyFileSet afs = AssemblyFileSet.builder().directory(relativeDirectory).build();
    // When
    final File result = resolveSourceDirectory(baseDirectory, afs);
    // Then
    assertThat(result, equalTo(new File(baseDirectory, "source-directory")));
    final Path relativeToBase = baseDirectory.toPath().relativize(result.toPath());
    assertThat(relativeToBase.toString(), equalTo("source-directory"));
  }

  @Test
  public void assemblyFileSetHasNoDirectoryShouldThrowException() {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder().build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .name("deployments")
        .build();
    // When
    final Exception result = Assert.assertThrows(NullPointerException.class, () ->
      AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac)
    );
    // Then
    assertThat(result.getMessage(), equalTo("Assembly FileSet directory is required"));
  }

  @Test
  public void assemblyConfigurationHasNoTargetDirShouldThrowException() {
    // Given
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .directory(sourceDirectory)
        .build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder().build();
    // When
    final Exception result = Assert.assertThrows(NullPointerException.class, () ->
        AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac)
    );
    // Then
    assertThat(result.getMessage(), equalTo("Assembly Configuration target dir is required"));
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
    final Map<File, String> permissions = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(permissions.entrySet(), hasSize(4));
    final File deployments = new File(outputDirectory, "deployments");
    assertThat(deployments.exists(), equalTo(true));
    assertThat(deployments.listFiles(), arrayWithSize(1));
    final File outputSourceDir = Objects.requireNonNull(deployments.listFiles())[0];
    assertThat(outputSourceDir.getName(), equalTo("source-directory"));
    assertThat(outputSourceDir.exists(), equalTo(true));
    assertThat(outputSourceDir.listFiles(), arrayWithSize(6));
    assertThat(outputSourceDir.list(), arrayContainingInAnyOrder("one", "two", "three", "1.txt", "3.other", "37"));
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
    final Map<File, String> permissions = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(permissions.entrySet(), empty());
    final File deployments = new File(outputDirectory, "deployments");
    assertThat(deployments.exists(), equalTo(false));
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
    final Map<File, String> permissions = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(permissions.entrySet(), hasSize(4));
    final File deployments = new File(outputDirectory, "deployments");
    assertThat(deployments.exists(), equalTo(true));
    assertThat(deployments.listFiles(), arrayWithSize(6));
    assertThat(deployments.list(), arrayContainingInAnyOrder("one", "two", "three", "1.txt", "3.other", "37"));
    assertThat(new File(deployments, "source-directory").exists(), equalTo(false));
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
    assertThat(absoluteOutputDirectory.listFiles(), emptyArray());
    final AssemblyFileSet afs = AssemblyFileSet.builder()
        .directory(sourceDirectory)
        .outputDirectory(absoluteOutputDirectory)
        .build();
    final AssemblyConfiguration ac = AssemblyConfiguration.builder()
        .targetDir("/deployments")
        .build();
    // When
    final Map<File, String> permissions = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(permissions.entrySet(), hasSize(4));
    assertThat(new File(outputDirectory, "deployments").exists(), equalTo(false));
    assertThat(absoluteOutputDirectory.listFiles(), arrayWithSize(6));
    assertThat(absoluteOutputDirectory.list(), arrayContainingInAnyOrder("one", "two", "three", "1.txt", "3.other", "37"));
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
    final Map<File, String> permissions = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(permissions.entrySet(), hasSize(4));
    final File deployments = new File(outputDirectory, "deployments");
    assertThat(deployments.exists(), equalTo(true));
    assertThat(deployments.listFiles(), arrayWithSize(1));
    final File resultingDirectory = outputDirectory.toPath().resolve("deployments").resolve("relative-output").toFile();
    assertThat(Objects.requireNonNull(deployments.listFiles())[0], equalTo(resultingDirectory));
    assertThat(resultingDirectory.exists(), equalTo(true));
    assertThat(resultingDirectory.listFiles(), arrayWithSize(6));
    assertThat(resultingDirectory.list(), arrayContainingInAnyOrder("one", "two", "three", "1.txt", "3.other", "37"));
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
    final Map<File, String> permissions = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(permissions.entrySet(), hasSize(1));
    final File deployments = new File(outputDirectory, "deployments");
    assertThat(deployments.exists(), equalTo(true));
    assertThat(deployments.listFiles(), arrayWithSize(1));
    final File outputSourceDir = Objects.requireNonNull(deployments.listFiles())[0];
    assertThat(outputSourceDir.getName(), equalTo("source-directory"));
    assertThat(outputSourceDir.exists(), equalTo(true));
    assertThat(outputSourceDir.listFiles(), arrayWithSize(3));
    assertThat(outputSourceDir.list(), arrayContainingInAnyOrder("one", "two", "1.txt"));
    assertThat(new File(outputSourceDir, "one").listFiles(), arrayWithSize(1));
    assertThat(new File(outputSourceDir, "two").listFiles(), arrayWithSize(3));
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
    final Map<File, String> permissions = AssemblyFileSetUtils.processAssemblyFileSet(baseDirectory, outputDirectory, afs, ac);
    // Then
    assertThat(permissions.entrySet(), hasSize(1));
    final File maven = new File(outputDirectory, "maven");
    assertThat(maven.exists(), equalTo(true));
    assertThat(maven.listFiles(), arrayWithSize(1));
    final File outputSourceDir = Objects.requireNonNull(maven.listFiles())[0];
    assertThat(outputSourceDir.getName(), equalTo("relative"));
    assertThat(outputSourceDir.exists(), equalTo(true));
    assertThat(outputSourceDir.listFiles(), arrayWithSize(3));
    assertThat(outputSourceDir.list(), arrayContainingInAnyOrder("one", "three", "37"));
    assertThat(new File(outputSourceDir, "one").listFiles(), arrayWithSize(1));
    assertThat(new File(outputSourceDir, "three").listFiles(), arrayWithSize(3));
  }
}