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
package org.eclipse.jkube.generator.javaexec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JavaExecGeneratorTest {
  private JavaProject project;
  private GeneratorContext generatorContext;
  private Properties properties;

  @TempDir
  Path temporaryFolder;

  @BeforeEach
  void setUp() throws IOException {
    properties = new Properties();
    final File targetDir = Files.createDirectory(temporaryFolder.resolve("target")).toFile();
    project = JavaProject.builder()
      .properties(properties)
      .baseDirectory(temporaryFolder.toFile())
      .buildDirectory(targetDir)
      .buildPackageDirectory(targetDir)
      .outputDirectory(targetDir)
      .version("0.0.1")
      .build();
    generatorContext = GeneratorContext.builder()
      .logger(spy(new KitLogger.SilentLogger()))
      .project(project)
      .build();
  }

  @Test
  void isApplicableWithDefaultsShouldReturnFalse() {
    // When
    final boolean result = new JavaExecGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isApplicableWithConfiguredMainClassShouldReturnTrue() {
    // Given
    properties.put("jkube.generator.java-exec.mainClass", "com.example.main");
    // When
    final boolean result = new JavaExecGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isApplicableWithExecPluginShouldReturnTrue() {
    // Given
    project.setPlugins(Collections.singletonList(
      Plugin.builder().groupId("org.apache.maven.plugins").artifactId("maven-shade-plugin").build()));
    // When
    final boolean result = new JavaExecGenerator(generatorContext).isApplicable(Collections.emptyList());
    // Then
    assertThat(result).isTrue();
  }

  @Test
  void createAssemblyWithNoFatJarShouldAddDefaultFileSets() {
    // When
    final AssemblyConfiguration result = new JavaExecGenerator(generatorContext).createAssembly();
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", false)
        .extracting(AssemblyConfiguration::getLayers).asInstanceOf(InstanceOfAssertFactories.list(Assembly.class)).hasSize(1)
        .first().asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
        .extracting(Assembly::getFileSets).asInstanceOf(InstanceOfAssertFactories.list(AssemblyFileSet.class))
        .hasSize(2);
  }

  @Test
  void createAssemblyWithFatJarShouldAddDefaultFileSetsAndFatJar() throws Exception {
    // Given
    final Path fatJarDirectory = Paths.get(JavaExecGeneratorTest.class.getResource("/fatjar-simple").toURI());
    generatorContext = generatorContext.toBuilder()
        .project(project.toBuilder()
          .buildPackageDirectory(fatJarDirectory.toFile())
          .baseDirectory(fatJarDirectory.getParent().toFile())
          .build())
        .build();
    // When
    final AssemblyConfiguration result = new JavaExecGenerator(generatorContext).createAssembly();
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("excludeFinalOutputArtifact", true)
        .extracting(AssemblyConfiguration::getLayers)
        .asInstanceOf(InstanceOfAssertFactories.list(AssemblyFileSet.class))
        .hasSize(1).first().asInstanceOf(InstanceOfAssertFactories.type(Assembly.class))
        .extracting(Assembly::getFileSets)
        .asInstanceOf(InstanceOfAssertFactories.list(AssemblyFileSet.class)).hasSize(3)
        .extracting("directory", "includes")
        .containsExactlyInAnyOrder(
            tuple(new File("src/main/jkube-includes"), Collections.emptyList()),
            tuple(new File("src/main/jkube-includes/bin"), Collections.emptyList()),
            tuple(new File("fatjar-simple"), Collections.singletonList("fat.jar"))
        );
  }


  @Test
  @DisplayName("createAssembly, logs info if the final output artifact is same as previous")
  void createAssembly_logsFinalArtifactSameAsPrevious() throws IOException {
    final Path fatJar = Files.createFile(project.getBuildDirectory().toPath().resolve("fat.jar"));
    try (MockedConstruction<FatJarDetector> fatJarDetector = mockConstruction(FatJarDetector.class,
      (mock, ctx) -> {
        FatJarDetector.Result mockedResult = mock(FatJarDetector.Result.class);
        when(mockedResult.getArchiveFile()).thenReturn(fatJar.toFile());
        when(mock.scan()).thenReturn(mockedResult);
      })) {
      JavaExecGenerator generator = new JavaExecGenerator(generatorContext);
      generator.createAssembly();
      generator.createAssembly(); // calling twice to simulate that the final output artifacts were not changed
      // Then
      verify(generatorContext.getLogger())
        .info("Final output artifact file was not rebuilt since last build. " +
          "HINT: try to compile and package your application prior to running the container image build task.");
    }
  }

  @Test
  @DisplayName("createAssembly, doesn't log info if artifact is recently built")
  void createAssembly_doesntLogWarnings() throws IOException {
    final Path fatJar = Files.createFile(project.getBuildDirectory().toPath().resolve("fat.jar"));
    try (MockedConstruction<FatJarDetector> fatJarDetector = mockConstruction(FatJarDetector.class,
      (mock, ctx) -> {
        FatJarDetector.Result mockedResult = mock(FatJarDetector.Result.class);
        when(mockedResult.getArchiveFile()).thenReturn(fatJar.toFile());
        when(mock.scan()).thenReturn(mockedResult);
      })) {
      new JavaExecGenerator(generatorContext).createAssembly();
      verify(generatorContext.getLogger(), times(0)).info(anyString());
      verify(generatorContext.getLogger(), times(0)).warn(anyString());
    }
  }

  @Test
  void customize_whenInvoked_shouldAddLabelsToBuildConfiguration() {
    // Given
    properties.put("jkube.generator.java-exec.mainClass", "org.example.Foo");

    // When
    final List<ImageConfiguration> result = new JavaExecGenerator(generatorContext).customize(new ArrayList<>(), false);

    // Then
    assertThat(result)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getLabels)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsKeys("org.label-schema.build-date", "org.label-schema.description", "org.label-schema.version",
            "org.label-schema.schema-version", "org.label-schema.build-date", "org.label-schema.name");
  }

  @Test
  void customize_whenInvoked_shouldNotAddBuildTimestampToBuildDateLabel() {
    // Given
    properties.put("jkube.generator.java-exec.mainClass", "org.example.Foo");
    generatorContext = generatorContext.toBuilder()
      .project(project.toBuilder()
        .buildDate(LocalDate.of(2015, 10, 21))
        .build())
      .build();
    // When
    final List<ImageConfiguration> result = new JavaExecGenerator(generatorContext).customize(new ArrayList<>(), false);
    // Then
    assertThat(result)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getLabels)
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsEntry("org.label-schema.build-date", "2015-10-21");
  }

  static Stream<Arguments> jdkDetectionCases() {
    return Stream.of(
      Arguments.of("maven.compiler.release", "11", JavaExecGenerator.JDK.JDK_11),
      Arguments.of("maven.compiler.release", "17", JavaExecGenerator.JDK.JDK_17),
      Arguments.of("maven.compiler.release", "21", JavaExecGenerator.JDK.JDK_21),
      Arguments.of("maven.compiler.release", "25", JavaExecGenerator.JDK.JDK_25),
      Arguments.of("maven.compiler.target", "11", JavaExecGenerator.JDK.JDK_11),
      Arguments.of("maven.compiler.target", "17", JavaExecGenerator.JDK.JDK_17),
      Arguments.of("maven.compiler.target", "21", JavaExecGenerator.JDK.JDK_21),
      Arguments.of("maven.compiler.target", "25", JavaExecGenerator.JDK.JDK_25),
      Arguments.of("maven.compiler.source", "17", JavaExecGenerator.JDK.JDK_17),
      Arguments.of("maven.compiler.target", "8", JavaExecGenerator.JDK.DEFAULT),
      Arguments.of("maven.compiler.target", "1.8", JavaExecGenerator.JDK.DEFAULT)
    );
  }

  @Nested
  @DisplayName("detectJdk")
  class DetectJdk {

    @ParameterizedTest(name = "with {0}={1} should return {2}")
    @MethodSource("org.eclipse.jkube.generator.javaexec.JavaExecGeneratorTest#jdkDetectionCases")
    void shouldDetectCorrectJdk(String property, String value, JavaExecGenerator.JDK expected) {
      // Given
      Properties props = new Properties();
      props.setProperty(property, value);
      JavaProject javaProject = JavaProject.builder().properties(props).build();
      // When & Then
      assertThat(JavaExecGenerator.detectJdk(javaProject)).isEqualTo(expected);
    }

    @Test
    @DisplayName("with no Java version properties should return DEFAULT")
    void withNoProperties_shouldReturnDefault() {
      // Given
      JavaProject javaProject = JavaProject.builder().properties(new Properties()).build();
      // When & Then
      assertThat(JavaExecGenerator.detectJdk(javaProject)).isEqualTo(JavaExecGenerator.JDK.DEFAULT);
    }

    @Test
    @DisplayName("maven.compiler.release takes precedence over maven.compiler.target")
    void releaseTakesPrecedenceOverTarget() {
      // Given
      Properties props = new Properties();
      props.setProperty("maven.compiler.release", "17");
      props.setProperty("maven.compiler.target", "21");
      JavaProject javaProject = JavaProject.builder().properties(props).build();
      // When & Then
      assertThat(JavaExecGenerator.detectJdk(javaProject)).isEqualTo(JavaExecGenerator.JDK.JDK_17);
    }

    @Test
    @DisplayName("maven.compiler.target takes precedence over maven.compiler.source")
    void targetTakesPrecedenceOverSource() {
      // Given
      Properties props = new Properties();
      props.setProperty("maven.compiler.target", "21");
      props.setProperty("maven.compiler.source", "17");
      JavaProject javaProject = JavaProject.builder().properties(props).build();
      // When & Then
      assertThat(JavaExecGenerator.detectJdk(javaProject)).isEqualTo(JavaExecGenerator.JDK.JDK_21);
    }

    @Test
    @DisplayName("with all three properties set, maven.compiler.release takes precedence")
    void allThreePropertiesSet_releaseTakesPrecedence() {
      // Given
      Properties props = new Properties();
      props.setProperty("maven.compiler.release", "25");
      props.setProperty("maven.compiler.target", "21");
      props.setProperty("maven.compiler.source", "17");
      JavaProject javaProject = JavaProject.builder().properties(props).build();
      // When & Then
      assertThat(JavaExecGenerator.detectJdk(javaProject)).isEqualTo(JavaExecGenerator.JDK.JDK_25);
    }
  }

  @Nested
  @DisplayName("customize with Java version auto-detection")
  class CustomizeWithJavaVersionAutoDetection {

    @Test
    @DisplayName("with maven.compiler.target=17, should use jkube-java-17 base image")
    void withJava17Target_shouldUseJava17Image() throws IOException {
      // Given
      properties.put("jkube.generator.java-exec.mainClass", "org.example.Foo");
      properties.put("maven.compiler.target", "17");
      // When
      final List<ImageConfiguration> result = new JavaExecGenerator(generatorContext)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(result).singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getFrom)
        .asString()
        .contains("jkube-java-17");
    }

    @Test
    @DisplayName("with maven.compiler.target=21, should use jkube-java-21 base image")
    void withJava21Target_shouldUseJava21Image() throws IOException {
      // Given
      properties.put("jkube.generator.java-exec.mainClass", "org.example.Foo");
      properties.put("maven.compiler.target", "21");
      // When
      final List<ImageConfiguration> result = new JavaExecGenerator(generatorContext)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(result).singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getFrom)
        .asString()
        .contains("jkube-java-21");
    }

    @Test
    @DisplayName("with maven.compiler.release=25, should use jkube-java-25 base image")
    void withJava25Release_shouldUseJava25Image() throws IOException {
      // Given
      properties.put("jkube.generator.java-exec.mainClass", "org.example.Foo");
      properties.put("maven.compiler.release", "25");
      // When
      final List<ImageConfiguration> result = new JavaExecGenerator(generatorContext)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(result).singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getFrom)
        .asString()
        .contains("jkube-java-25");
    }

    @Test
    @DisplayName("with no Java version property, should use default jkube-java base image")
    void withNoJavaVersion_shouldUseDefaultImage() throws IOException {
      // Given
      properties.put("jkube.generator.java-exec.mainClass", "org.example.Foo");
      // When
      final List<ImageConfiguration> result = new JavaExecGenerator(generatorContext)
        .customize(new ArrayList<>(), false);
      // Then
      assertThat(result).singleElement()
        .extracting(ImageConfiguration::getBuildConfiguration)
        .extracting(BuildConfiguration::getFrom)
        .asString()
        .contains("jkube-java:")
        .doesNotContain("jkube-java-17", "jkube-java-21", "jkube-java-25", "jkube-java-11");
    }
  }
}
