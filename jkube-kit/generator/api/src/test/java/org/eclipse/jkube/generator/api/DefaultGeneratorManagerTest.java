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
package org.eclipse.jkube.generator.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.eclipse.jkube.kit.common.JKubeException;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.GeneratorManager;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DefaultGeneratorManagerTest {

  private KitLogger logger;

  private GeneratorManager generatorManager;
  private ImageConfiguration imageConfig;
  private GeneratorContext generatorContext;

  @BeforeEach
  void setUp() {
    logger = spy(new KitLogger.SilentLogger());
    final ProcessorConfig processorConfig = new ProcessorConfig();
    processorConfig.setIncludes(Collections.singletonList("fake-generator"));
    JavaProject javaProject = JavaProject.builder()
        .groupId("org.eclipse.jkube")
        .artifactId("test-java-project")
        .version("0.0.1-SNAPSHOT")
        .properties(new Properties())
        .baseDirectory(new File("dummy-dir"))
        .build();
    generatorContext = GeneratorContext.builder()
        .config(processorConfig)
        .logger(logger)
        .project(javaProject)
        .build();
    imageConfig = ImageConfiguration.builder()
        .name("foo/bar:latest")
        .build(BuildConfiguration.builder()
            .from("foobase:latest")
            .build())
        .build();
    generatorManager = new DefaultGeneratorManager(generatorContext);
  }


  @Nested
  @DisplayName("generateAndMerge")
  class GenerateAndMerge {
    @Test
    void withEmptyImageConfiguration_shouldMergeWithImageConfigGeneratedViaGenerator() {
      // Given
      ImageConfiguration imageConfiguration = new ImageConfiguration();
      imageConfiguration.setName("foo/bar:latest");
      final List<ImageConfiguration> images = Collections.singletonList(imageConfiguration);
      // When
      final List<ImageConfiguration> result = generatorManager.generateAndMerge(images);
      // Then
      assertThat(result)
          .isNotSameAs(images)
          .hasSize(1)
          .extracting(ImageConfiguration::getAlias)
          .contains("processed-by-test");
      verify(logger, times(1)).info("Running generator %s", "fake-generator");
    }

    @Test
    void withSimpleImageConfiguration_shouldReturnImageConfiguration() {
      // Given
      List<ImageConfiguration> images = new ArrayList<>();
      images.add(imageConfig);

      // When
      List<ImageConfiguration> resolvedImages = generatorManager.generateAndMerge(images);

      // Then
      AssertionsForInterfaceTypes.assertThat(resolvedImages).isNotNull()
          .singleElement()
          .isEqualTo(imageConfig);
    }

    @Test
    void whenImageConfigurationNameBlank_thenThrowException() {
      // Given
      ImageConfiguration imageConfiguration = ImageConfiguration.builder().build();
      List<ImageConfiguration> images = Collections.singletonList(imageConfiguration);

      // When + Then
      assertThatThrownBy(() -> generatorManager.generateAndMerge(images))
          .isInstanceOf(JKubeException.class)
          .hasMessage("Configuration error: <image> must have a non-null <name>");
    }

    @Test
    void whenNoMatchForImageFilter_thenLogWarning() {
      // Given
      generatorContext = generatorContext.toBuilder().filter("i-dont-exist").build();
      List<ImageConfiguration> images = Collections.singletonList(imageConfig);

      // When
      new DefaultGeneratorManager(generatorContext).generateAndMerge(images);

      // Then
      verify(logger).warn("None of the resolved images [%s] match the configured filter '%s'", "foo/bar:latest", "i-dont-exist");
    }

    @Test
    void whenNoMatchForMultipleImageNameFilters_thenLogWarning() {
      // Given
      generatorContext = generatorContext.toBuilder().filter("filter1,filter2").build();
      List<ImageConfiguration> images = Collections.singletonList(imageConfig);

      // When
      new DefaultGeneratorManager(generatorContext).generateAndMerge(images);

      // Then
      verify(logger).warn("None of the resolved images [%s] match the configured filter '%s'", "foo/bar:latest", "filter1,filter2");
    }

    @Test
    void whenDockerfileUsed_thenLogDockerfilePathAndContextDir(@TempDir File temporaryFolder) {
      File dockerFile = temporaryFolder.toPath().resolve("Dockerfile").toFile();
      ImageConfiguration dummyImageConfiguration = ImageConfiguration.builder()
          .name("imageconfiguration-no-build:latest")
          .build(BuildConfiguration.builder()
              .dockerFile(dockerFile.getAbsolutePath())
              .build())
          .build();
      List<ImageConfiguration> images = new ArrayList<>();
      images.add(dummyImageConfiguration);

      // When
      generatorManager.generateAndMerge(images);

      // Then
      verify(logger).info(eq("Using Dockerfile: %s"), anyString());
      verify(logger).info(eq("Using Docker Context Directory: %s"), any(File.class));
    }
  }

  // Loaded from META-INF/jkube/generator-default
  public static final class TestGenerator implements Generator {

    public TestGenerator(GeneratorContext ignored) {
    }

    @Override
    public String getName() {
      return "fake-generator";
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
      return true;
    }

    @Override
    public List<ImageConfiguration> customize(List<ImageConfiguration> existingConfigs, boolean prePackagePhase) {
      return existingConfigs.stream()
          .peek(ic -> ic.setAlias("processed-by-test"))
          .collect(Collectors.toList());
    }
  }
}
