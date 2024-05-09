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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.BuildRecreateMode;
import org.eclipse.jkube.kit.common.JKubeException;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.GeneratorManager;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
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

  private ImageConfiguration baseImageConfig;
  private GeneratorManager generatorManager;
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
    baseImageConfig = ImageConfiguration.builder()
      .name("foo/bar:latest")
      .build(BuildConfiguration.builder()
        .from("foobase:latest")
        .build())
      .build();
    generatorContext = GeneratorContext.builder()
        .config(processorConfig)
        .logger(logger)
        .project(javaProject)
        .build();
    generatorManager = new DefaultGeneratorManager(generatorContext);
  }

  @Test
  void withEmptyImageConfigurations_shouldCreteImageConfigViaGenerator() {
    // Given
    final List<ImageConfiguration> images = Collections.emptyList();
    // When
    final List<ImageConfiguration> result = generatorManager.generateAndMerge(images);
    // Then
    assertThat(result)
      .isNotSameAs(images)
      .singleElement()
      .hasFieldOrPropertyWithValue("alias", "processed-by-test")
      .hasFieldOrPropertyWithValue("name", "generated-by-test");
    verify(logger, times(1)).info("Running generator %s", "fake-generator");
  }

  @Test
  void withImageConfigurationNameBlank_throwsException() {
    // Given
    final List<ImageConfiguration> images = Collections.singletonList(ImageConfiguration.builder().build());
    // When + Then
    assertThatThrownBy(() -> generatorManager.generateAndMerge(images))
      .isInstanceOf(JKubeException.class)
      .hasMessage("Configuration error: <image> must have a non-null <name>");
  }

  @Test
  void withSimpleImageConfiguration_shouldMergeWithImageConfigGeneratedViaGenerator() {
    // Given
    final List<ImageConfiguration> images = Collections.singletonList(baseImageConfig);
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
  void whenNoMatchForImageFilter_thenLogWarning() {
    // Given
    generatorContext = generatorContext.toBuilder().filter("i-dont-exist").build();
    // When
    new DefaultGeneratorManager(generatorContext).generateAndMerge(Collections.singletonList(baseImageConfig));
    // Then
    verify(logger).warn("None of the resolved images [%s] match the configured filter '%s'", "foo/bar:latest", "i-dont-exist");
  }

  @Test
  void whenNoMatchForMultipleImageNameFilters_thenLogWarning() {
    // Given
    generatorContext = generatorContext.toBuilder().filter("filter1,filter2").build();
    // When
    new DefaultGeneratorManager(generatorContext).generateAndMerge(Collections.singletonList(baseImageConfig));
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

  @Nested
  @DisplayName("merge configuration parameters for OpenShift S2I build into Image Build configuration")
  class MergeOpenShiftS2IImageConfigParams {
    @Test
    @DisplayName("zero configuration, do not set anything in image build configuration")
    void whenNoConfigurationProvided_thenDoNotSetInBuildConfig() {
      // When
      final List<ImageConfiguration> result = generatorManager.generateAndMerge(Collections.singletonList(baseImageConfig));
      // Then
      assertThat(result)
        .singleElement()
        .extracting(ImageConfiguration::getBuild)
        .hasFieldOrPropertyWithValue("openshiftForcePull", false)
        .hasFieldOrPropertyWithValue("openshiftS2iBuildNameSuffix", null)
        .hasFieldOrPropertyWithValue("openshiftS2iImageStreamLookupPolicyLocal", false)
        .hasFieldOrPropertyWithValue("openshiftPullSecret", null)
        .hasFieldOrPropertyWithValue("openshiftPushSecret", null)
        .hasFieldOrPropertyWithValue("openshiftBuildOutputKind", null)
        .hasFieldOrPropertyWithValue("openshiftBuildRecreateMode", null);
    }

    @Test
    @DisplayName("image Configuration, then fields retained in Image Configuration")
    void whenProvidedInImageConfiguration_thenDoNotUpdateBuildConfig() {
      // Given
      baseImageConfig = baseImageConfig.toBuilder()
        .build(BuildConfiguration.builder()
          .openshiftForcePull(true)
          .openshiftS2iBuildNameSuffix("-custom")
          .openshiftS2iImageStreamLookupPolicyLocal(true)
          .openshiftPushSecret("push-secret")
          .openshiftPullSecret("pull-secret")
          .openshiftBuildOutputKind("ImageStreamTag")
          .openshiftBuildRecreateMode(BuildRecreateMode.buildConfig)
          .build())
        .build();
      // When
      List<ImageConfiguration> result = generatorManager.generateAndMerge(Collections.singletonList(baseImageConfig));

      // Then
      assertThat(result)
        .singleElement()
        .extracting(ImageConfiguration::getBuild)
        .hasFieldOrPropertyWithValue("openshiftForcePull", true)
        .hasFieldOrPropertyWithValue("openshiftS2iBuildNameSuffix", "-custom")
        .hasFieldOrPropertyWithValue("openshiftS2iImageStreamLookupPolicyLocal", true)
        .hasFieldOrPropertyWithValue("openshiftPullSecret", "pull-secret")
        .hasFieldOrPropertyWithValue("openshiftPushSecret", "push-secret")
        .hasFieldOrPropertyWithValue("openshiftBuildOutputKind", "ImageStreamTag")
        .hasFieldOrPropertyWithValue("openshiftBuildRecreateMode", BuildRecreateMode.buildConfig);
    }

    @Test
    @DisplayName("plugin configuration, then fields merged in final Image Configuration")
    void whenProvidedViaPluginConfiguration_thenSetInBuildConfig() {
      // Given
      generatorContext = generatorContext.toBuilder()
        .openshiftForcePull(true)
        .openshiftS2iBuildNameSuffix("-custom")
        .openshiftS2iImageStreamLookupPolicyLocal(true)
        .openshiftPullSecret("pull-secret")
        .openshiftPushSecret("push-secret")
        .openshiftBuildOutputKind("ImageStreamTag")
        .openshiftBuildRecreate(BuildRecreateMode.buildConfig)
        .build();
      // When
      final List<ImageConfiguration> result = new DefaultGeneratorManager(generatorContext)
        .generateAndMerge(Collections.singletonList(baseImageConfig));
      // Then
      assertThat(result)
        .singleElement()
        .extracting(ImageConfiguration::getBuild)
        .hasFieldOrPropertyWithValue("openshiftForcePull", true)
        .hasFieldOrPropertyWithValue("openshiftS2iBuildNameSuffix", "-custom")
        .hasFieldOrPropertyWithValue("openshiftS2iImageStreamLookupPolicyLocal", true)
        .hasFieldOrPropertyWithValue("openshiftPullSecret", "pull-secret")
        .hasFieldOrPropertyWithValue("openshiftPushSecret", "push-secret")
        .hasFieldOrPropertyWithValue("openshiftBuildOutputKind", "ImageStreamTag")
        .hasFieldOrPropertyWithValue("openshiftBuildRecreateMode", BuildRecreateMode.buildConfig);
    }

    @Test
    @DisplayName("plugin configuration and image configuration, then fields retained in Image Configuration")
    void whenProvidedViaBothPluginAndImageConfiguration_thenDoNotModifyConfigurationSetInBuildConfig() {
      // Given
      baseImageConfig = baseImageConfig.toBuilder()
        .build(BuildConfiguration.builder()
          .openshiftForcePull(true)
          .openshiftS2iBuildNameSuffix("-custom-via-image-config")
          .openshiftS2iImageStreamLookupPolicyLocal(true)
          .openshiftPushSecret("push-secret-via-image-config")
          .openshiftPullSecret("pull-secret-via-image-config")
          .openshiftBuildOutputKind("ImageStreamTag-via-image-config")
          .openshiftBuildRecreateMode(BuildRecreateMode.buildConfig)
          .build())
        .build();
      generatorContext = generatorContext.toBuilder()
        .openshiftForcePull(true)
        .openshiftS2iBuildNameSuffix("-custom")
        .openshiftS2iImageStreamLookupPolicyLocal(true)
        .openshiftPullSecret("pull-secret")
        .openshiftPushSecret("push-secret")
        .openshiftBuildOutputKind("ImageStreamTag")
        .openshiftBuildRecreate(BuildRecreateMode.buildConfig)
        .build();
      // When
      final List<ImageConfiguration> result = new DefaultGeneratorManager(generatorContext)
        .generateAndMerge(Collections.singletonList(baseImageConfig));
      // Then
      assertThat(result)
        .singleElement()
        .extracting(ImageConfiguration::getBuild)
        .hasFieldOrPropertyWithValue("openshiftForcePull", true)
        .hasFieldOrPropertyWithValue("openshiftS2iBuildNameSuffix", "-custom-via-image-config")
        .hasFieldOrPropertyWithValue("openshiftS2iImageStreamLookupPolicyLocal", true)
        .hasFieldOrPropertyWithValue("openshiftPullSecret", "pull-secret-via-image-config")
        .hasFieldOrPropertyWithValue("openshiftPushSecret", "push-secret-via-image-config")
        .hasFieldOrPropertyWithValue("openshiftBuildOutputKind", "ImageStreamTag-via-image-config")
        .hasFieldOrPropertyWithValue("openshiftBuildRecreateMode", BuildRecreateMode.buildConfig);
    }

    @Test
    void whenOpenShiftRuntimeMode_thenShouldResolveGroupToNamespace() {
      // Given
      baseImageConfig = baseImageConfig.toBuilder().name("%g/%a").build();
      generatorContext = generatorContext.toBuilder()
        .openshiftNamespace("test-custom-namespace")
        .runtimeMode(RuntimeMode.OPENSHIFT)
        .build();
      // When
      final List<ImageConfiguration> result = new DefaultGeneratorManager(generatorContext)
        .generateAndMerge(Collections.singletonList(baseImageConfig));
      // Then
      assertThat(result)
        .singleElement()
        .hasFieldOrPropertyWithValue("name", "test-custom-namespace/test-java-project");
    }
  }

  @Nested
  @DisplayName("Resolve ImageConfiguration from properties")
  class ImageConfigurationFromProperties {

    @Test
    void withGlobalPrefix() {
      // Given
      generatorContext.getProject().getProperties().put("jkube.container-image.name", "image-name");
      generatorContext.getProject().getProperties().put("jkube.container-image.alias", "image-alias");
      generatorContext.getProject().getProperties().put("jkube.container-image.from", "scratch");
      // When
      final List<ImageConfiguration> result = generatorManager.generateAndMerge(Collections.singletonList(new ImageConfiguration()));
      // Then
      assertThat(result).singleElement()
        .hasFieldOrPropertyWithValue("name", "image-name")
        .extracting(ImageConfiguration::getBuild)
        .hasFieldOrPropertyWithValue("from", "scratch");
    }

    @Test
    void withPrefix() {
      // Given
      final ImageConfiguration prefixed = ImageConfiguration.builder()
        .propertyResolverPrefix("app.images.image-1")
        .build();
      final ImageConfiguration standard = ImageConfiguration.builder().build();
      generatorContext.getProject().getProperties().put("app.images.image-1.name", "prefixed-image-name");
      generatorContext.getProject().getProperties().put("jkube.container-image.name", "image-name");
      // When
      final List<ImageConfiguration> result = generatorManager.generateAndMerge(Arrays.asList(prefixed, standard));
      // Then
      assertThat(result).extracting("name").containsExactly("prefixed-image-name", "image-name");
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
      if (existingConfigs == null || existingConfigs.isEmpty()) {
        existingConfigs = Collections.singletonList(ImageConfiguration.builder()
            .name("generated-by-test")
          .build());
      }
      return existingConfigs.stream()
          .peek(ic -> ic.setAlias("processed-by-test"))
          .collect(Collectors.toList());
    }
  }
}
