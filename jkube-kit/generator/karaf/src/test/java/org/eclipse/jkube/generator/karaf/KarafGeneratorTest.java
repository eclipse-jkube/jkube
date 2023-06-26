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
package org.eclipse.jkube.generator.karaf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class KarafGeneratorTest {
  @TempDir
  File temporaryFolder;
  private GeneratorContext generatorContext;

  @BeforeEach
  public void setUp() {
    generatorContext = GeneratorContext.builder()
      .logger(new KitLogger.SilentLogger())
      .project(JavaProject.builder().build())
      .build();
  }

  @Test
  @DisplayName("Warn if final output artifact is missing")
  void warnIfMissingFinalOutputArtifact() throws IOException {
    // @Todo: move these initializations to setup method when replacing mocks with real objects
    KitLogger testLogger = spy(new KitLogger.SilentLogger());
    File targetDir = Files.createDirectory(temporaryFolder.toPath().resolve("target")).toFile();
    JavaProject project = JavaProject.builder()
            .baseDirectory(targetDir)
            .buildDirectory(targetDir.getAbsoluteFile())
            .buildPackageDirectory(targetDir.getAbsoluteFile())
            .outputDirectory(targetDir)
            .buildFinalName("sample")
            .version("1.0.0-SNAPSHOT")
            .packaging("jar")
            .build();
    generatorContext = GeneratorContext.builder()
            .project(project)
            .logger(testLogger)
            .build();

    // Given, When
    new KarafGenerator(generatorContext).customize(new ArrayList<>(), false);

    // Then
    verify(generatorContext.getLogger())
            .error("karaf: Final output artifact file was not detected. The project may have not been built." +
                   " HINT: try to compile and package your application prior to running the container image build task.");
  }

  @Test
  @DisplayName("No warnings or errors if the final output artifact is present")
  void noWarnOrErrorIfFinalOutPutArtifactPresent() throws IOException {
    // @Todo: move these initializations to setup method when replacing mocks with real objects
    KitLogger testLogger = spy(new KitLogger.SilentLogger());
    File targetDir = Files.createDirectory(temporaryFolder.toPath().resolve("target")).toFile();
    JavaProject project = JavaProject.builder()
            .baseDirectory(targetDir)
            .buildDirectory(targetDir.getAbsoluteFile())
            .buildPackageDirectory(targetDir.getAbsoluteFile())
            .outputDirectory(targetDir)
            .buildFinalName("sample")
            .version("1.0.0-SNAPSHOT")
            .packaging("jar")
            .build();
    generatorContext = GeneratorContext.builder()
            .project(project)
            .logger(testLogger)
            .build();

    // Given
    Files.createFile(targetDir.toPath().resolve("sample.jar"));
    KarafGenerator testGenerator = new KarafGenerator(generatorContext);

    // When
    testGenerator.customize(new ArrayList<>(), false);

    // Then
    verify(generatorContext.getLogger(), times(0)).info(anyString());
    verify(generatorContext.getLogger(), times(0)).error(anyString());
  }

  @Test
  @DisplayName("Warn if the final output artifact is same as previous")
  void warnIfFinalArtifactSameAsPrevious() throws IOException {
    // @Todo: move these initializations to setup method when replacing mocks with real objects
    KitLogger testLogger = spy(new KitLogger.SilentLogger());
    File targetDir = Files.createDirectory(temporaryFolder.toPath().resolve("target")).toFile();
    JavaProject project = JavaProject.builder()
            .baseDirectory(targetDir)
            .buildDirectory(targetDir.getAbsoluteFile())
            .buildPackageDirectory(targetDir.getAbsoluteFile())
            .outputDirectory(targetDir)
            .buildFinalName("sample")
            .version("1.0.0-SNAPSHOT")
            .packaging("jar")
            .build();
    generatorContext = GeneratorContext.builder()
            .project(project)
            .logger(testLogger)
            .build();

    // Given
    Files.createFile(targetDir.toPath().resolve("sample.jar"));
    KarafGenerator generator = new KarafGenerator(generatorContext);

    // When
    generator.customize(new ArrayList<>(), false);
    generator.customize(new ArrayList<>(), false); // calling twice to simulate that the final output artifacts were not changed

    // Then
    verify(generatorContext.getLogger())
            .info("karaf: Final output artifact is the same as previous build. " +
                  "HINT: You might have forgotten to compile and package your application after making changes.");
  }

  @Test
  @DisplayName("No warn or error if the final output artifact is present and different from previous")
  void noWarnOrErrorIfFinalOutputNotSameAsPrevious() throws IOException {
    // @Todo: move these initializations to setup method when replacing mocks with real objects
    KitLogger testLogger = spy(new KitLogger.SilentLogger());
    File targetDir = Files.createDirectory(temporaryFolder.toPath().resolve("target")).toFile();
    JavaProject project = JavaProject.builder()
            .baseDirectory(targetDir)
            .buildDirectory(targetDir.getAbsoluteFile())
            .buildPackageDirectory(targetDir.getAbsoluteFile())
            .outputDirectory(targetDir)
            .buildFinalName("sample")
            .version("1.0.0-SNAPSHOT")
            .packaging("jar")
            .build();
    generatorContext = GeneratorContext.builder()
            .project(project)
            .logger(testLogger)
            .build();

    // Given
    Files.createFile(targetDir.toPath().resolve("sample.jar"));
    KarafGenerator generator = new KarafGenerator(generatorContext);

    // When (This simulates that final output artifacts were changed)
    generator.customize(new ArrayList<>(), false);
    Files.delete(targetDir.toPath().resolve("sample.jar"));
    Files.createFile(targetDir.toPath().resolve("sample.jar"));

    // Then
    verify(generatorContext.getLogger(), times(0)).info(anyString());
    verify(generatorContext.getLogger(), times(0)).error(anyString());
  }


  @Test
  void isApplicableHasKarafMavenPluginShouldReturnTrue() {
    // Given
    generatorContext.getProject().setPlugins(Collections.singletonList(Plugin.builder()
      .groupId("org.apache.karaf.or.any.other.groupid")
      .artifactId("karaf-maven-plugin")
      .build()));
    // When
    final boolean result = new KarafGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isApplicableHasNotKarafMavenPluginShouldReturnFalse() {
    // Given
    generatorContext.getProject().setPlugins(Collections.singletonList(Plugin.builder()
      .groupId("org.apache.karaf.tooling")
      .artifactId("not-karaf-maven-plugin")
      .build()));
    // When
    final boolean result = new KarafGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void customizeWithKarafMavenPluginShouldAddImageConfiguration() {
    // Given
    generatorContext.getProject().setPlugins(Collections.singletonList(Plugin.builder()
      .groupId("org.apache.karaf.tooling")
      .artifactId("karaf-maven-plugin")
      .build()));
    generatorContext.getProject().setBuildDirectory(temporaryFolder);
    generatorContext.getProject().setVersion("1.33.7-SNAPSHOT");
    // When
    final List<ImageConfiguration> result = new KarafGenerator(generatorContext)
            .customize(new ArrayList<>(), false);
    // Then
    assertThat(result)
        .singleElement()
        .hasFieldOrPropertyWithValue("name", "%g/%a:%l")
        .hasFieldOrPropertyWithValue("alias", "karaf")
        .extracting(ImageConfiguration::getBuildConfiguration)
        .hasFieldOrPropertyWithValue("tags", Collections.singletonList("latest"))
        .hasFieldOrPropertyWithValue("ports", Arrays.asList("8181", "8778"))
        .extracting(BuildConfiguration::getEnv)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsOnly(
             entry("DEPLOYMENTS_DIR", "/deployments"),
             entry("KARAF_HOME", "/deployments/karaf")
        );
    assertThat(result.iterator().next().getBuildConfiguration().getAssembly())
        .hasFieldOrPropertyWithValue("name", "deployments")
        .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", false)
        .extracting(AssemblyConfiguration::getLayers).asList()
        .singleElement()
        .extracting("fileSets").asList()
        .extracting("directory", "outputDirectory", "directoryMode", "fileMode")
        .containsExactly(
            tuple(
         new File(temporaryFolder, "assembly"),
                new File("karaf"),
                "0775",
                null),
            tuple(
         temporaryFolder.toPath().resolve("assembly").resolve("bin").toFile(),
                new File("karaf", "bin"),
                "0775",
                "0777")
        );
  }

  @Test
  void customizeWithKarafMavenPluginAndCustomConfigShouldAddImageConfiguration() {
    // Given
    final Properties props = new Properties();
    props.put("jkube.generator.karaf.baseDir", "/other-dir");
    props.put("jkube.generator.karaf.webPort", "8080");
    generatorContext.getProject().setBuildDirectory(temporaryFolder);
    generatorContext.getProject().setVersion("1.33.7-SNAPSHOT");
    generatorContext.getProject().setProperties(props);
    // When
    final List<ImageConfiguration> result = new KarafGenerator(generatorContext)
        .customize(new ArrayList<>(), false);
    // Then
    assertThat(result).singleElement()
            .extracting(ImageConfiguration::getBuildConfiguration)
            .hasFieldOrPropertyWithValue("ports", Arrays.asList("8080", "8778"))
            .extracting(BuildConfiguration::getEnv)
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsEntry("DEPLOYMENTS_DIR", "/other-dir");
  }
}
