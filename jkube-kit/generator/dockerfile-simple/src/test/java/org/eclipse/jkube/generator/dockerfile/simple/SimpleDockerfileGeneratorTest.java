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
package org.eclipse.jkube.generator.dockerfile.simple;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;

import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class SimpleDockerfileGeneratorTest {
  @TempDir
  private File temporaryFolder;
  private File dockerFile;
  private SimpleDockerfileGenerator simpleDockerfileGenerator;
  private Properties properties;

  @BeforeEach
  void setUp() throws IOException {
    dockerFile = temporaryFolder.toPath().resolve("Dockerfile").toFile();
    File srcDockerFile = new File(Objects.requireNonNull(getClass().getResource("/dummy-javaproject/Dockerfile")).getFile());
    Files.copy(srcDockerFile.toPath(), dockerFile.toPath());
    properties = new Properties();
    JavaProject javaProject = JavaProject.builder()
        .baseDirectory(temporaryFolder)
        .buildDirectory(temporaryFolder.toPath().resolve("target").toFile())
        .properties(properties)
        .build();
    GeneratorContext generatorContext = GeneratorContext.builder()
        .project(javaProject)
        .build();
    simpleDockerfileGenerator = new SimpleDockerfileGenerator(generatorContext);
  }

  @Nested
  @DisplayName("isApplicable")
  class IsApplicable {
    @Test
    @DisplayName("Dockerfile present in project base directory, return true")
    void whenDockerFilePresentInProjectBaseDir_thenReturnTrue() {
      assertThat(simpleDockerfileGenerator.isApplicable(Collections.emptyList())).isTrue();
    }

    @Test
    @DisplayName("Dockerfile present in project base directory and ImageConfiguration present, return false")
    void whenImageConfigurationAlreadyProvided_thenReturnFalse() {
      // Given
      List<ImageConfiguration> imageConfigurationList = new ArrayList<>();
      imageConfigurationList.add(ImageConfiguration.builder()
          .name("foo/bar:latest")
          .build(BuildConfiguration.builder()
              .from("foo/base:latest")
              .build())
          .build());

      // When
      assertThat(simpleDockerfileGenerator.isApplicable(imageConfigurationList)).isFalse();
    }

    @Test
    @DisplayName("Dockerfile present in project base directory and ImageConfiguration does not contain BuildConfig, return true")
    void whenProvidedImageConfigurationDoesNotContainBuildConfig_thenReturnFalse() {
      // Given
      List<ImageConfiguration> imageConfigurationList = new ArrayList<>();
      imageConfigurationList.add(ImageConfiguration.builder()
          .name("foo/bar:latest")
          .build());

      // When
      assertThat(simpleDockerfileGenerator.isApplicable(imageConfigurationList)).isTrue();
    }

    @Test
    @DisplayName("Dockerfile NOT present in project base directory, return false")
    void whenDockerFileAbsentInProjectBaseDir_thenReturnFalse() throws IOException {
      // Given
      Files.delete(dockerFile.toPath());

      // When + Then
      assertThat(simpleDockerfileGenerator.isApplicable(Collections.emptyList())).isFalse();
    }
  }

  @Nested
  @DisplayName("customize")
  class Customize {
    @Test
    @DisplayName("no existing ImageConfiguration, create opinionated ImageConfiguration for Dockerfile")
    void whenNoExistingImageConfiguration_thenCreateImageConfiguration() {
      List<ImageConfiguration> images = new ArrayList<>();

      // When
      List<ImageConfiguration> resolvedImages = simpleDockerfileGenerator.customize(images, false);
      resolvedImages.forEach(i -> i.getBuild().initAndValidate());

      // Then
      assertThat(resolvedImages).isNotNull()
          .singleElement()
          .hasFieldOrPropertyWithValue("name", "%g/%a:%l")
          .hasFieldOrPropertyWithValue("build.dockerFile", dockerFile)
          .hasFieldOrPropertyWithValue("build.ports", Collections.singletonList("8080"));
    }

    @Test
    @DisplayName("no existing ImageConfiguration and jkube.image.name property provided, create opinionated ImageConfiguration with configured name for Dockerfile")
    void whenNoExistingImageConfigurationAndImageNamePropertyProvided_thenCreateImageConfiguration() {
      List<ImageConfiguration> images = new ArrayList<>();
      properties.put("jkube.image.name", "configured-via-jkube-image-name-property:latest");

      // When
      List<ImageConfiguration> resolvedImages = simpleDockerfileGenerator.customize(images, false);
      resolvedImages.forEach(i -> i.getBuild().initAndValidate());

      // Then
      assertThat(resolvedImages).isNotNull()
          .singleElement()
          .hasFieldOrPropertyWithValue("name", "configured-via-jkube-image-name-property:latest")
          .hasFieldOrPropertyWithValue("build.dockerFile", dockerFile)
          .hasFieldOrPropertyWithValue("build.ports", Collections.singletonList("8080"));
    }

    @Test
    @DisplayName("no existing ImageConfiguration and jkube.generator.name property provided, create opinionated ImageConfiguration with configured name for Dockerfile")
    void whenNoExistingImageConfigurationAndGeneratorNamePropertyProvided_thenCreateImageConfiguration() {
      List<ImageConfiguration> images = new ArrayList<>();
      properties.put("jkube.generator.name", "configured-via-jkube-generator-name-property:latest");

      // When
      List<ImageConfiguration> resolvedImages = simpleDockerfileGenerator.customize(images, false);
      resolvedImages.forEach(i -> i.getBuild().initAndValidate());

      // Then
      assertThat(resolvedImages).isNotNull()
          .singleElement()
          .hasFieldOrPropertyWithValue("name", "configured-via-jkube-generator-name-property:latest")
          .hasFieldOrPropertyWithValue("build.dockerFile", dockerFile)
          .hasFieldOrPropertyWithValue("build.ports", Collections.singletonList("8080"));
    }

    @Test
    @DisplayName("ImageConfiguration without BuildConfig provided, merge opinionated ImageConfiguration for Dockerfile with provided ImageConfig")
    void whenProvidedImageConfigurationWithNoBuild_shouldMergeOpinionatedWithExistingImageConfiguration() {
      ImageConfiguration dummyImageConfiguration = ImageConfiguration.builder()
          .name("imageconfiguration-no-build:latest")
          .build();
      List<ImageConfiguration> images = new ArrayList<>();
      images.add(dummyImageConfiguration);

      // When
      List<ImageConfiguration> resolvedImages = simpleDockerfileGenerator.customize(images, false);
      resolvedImages.forEach(i -> i.getBuild().initAndValidate());

      // Then
      assertThat(resolvedImages).isNotNull()
          .singleElement()
          .hasFieldOrPropertyWithValue("name", "imageconfiguration-no-build:latest")
          .hasFieldOrPropertyWithValue("build.dockerFileFile", dockerFile)
          .hasFieldOrPropertyWithValue("build.ports", Collections.singletonList("8080"));
    }

    @Test
    @DisplayName("Valid ImageConfiguration already provided, then do not change provided ImageConfiguration")
    void whenImageConfigurationProvided_thenDoNotModifyExistingImageConfiguration() {
      ImageConfiguration dummyImageConfiguration = ImageConfiguration.builder()
          .name("imageconfiguration-no-build:latest")
          .build(BuildConfiguration.builder()
              .from("foo/base:latest")
              .port("8002")
              .build())
          .build();
      List<ImageConfiguration> images = new ArrayList<>();
      images.add(dummyImageConfiguration);

      // When
      List<ImageConfiguration> resolvedImages = simpleDockerfileGenerator.customize(images, false);
      resolvedImages.forEach(i -> i.getBuild().initAndValidate());

      // Then
      assertThat(resolvedImages).isNotNull()
          .singleElement()
          .hasFieldOrPropertyWithValue("name", "imageconfiguration-no-build:latest")
          .hasFieldOrPropertyWithValue("build.dockerFileFile", null)
          .hasFieldOrPropertyWithValue("build.from", "foo/base:latest")
          .hasFieldOrPropertyWithValue("build.ports", Collections.singletonList("8002"));
    }
  }
}
