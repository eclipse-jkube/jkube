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
package org.eclipse.jkube.generator.api.support;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.entry;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author roland
 */

class BaseGeneratorTest {
  private GeneratorContext ctx;
  private JavaProject project;

  private Properties properties;
  private ProcessorConfig config;

  @BeforeEach
  void setUp() {
    ctx = mock(GeneratorContext.class,RETURNS_DEEP_STUBS);
    project = mock(JavaProject.class);
    properties = new Properties();
    config = new ProcessorConfig();

    when(project.getProperties()).thenReturn(properties);
    when(ctx.getProject()).thenReturn(project);
    when(ctx.getConfig()).thenReturn(config);
  }

  @AfterEach
  void tearDown() {
    config = null;
    properties = null;
  }

  @Test
  @DisplayName("get from as configured with properties and configuration, should return configured")
  void fromAsConfiguredWithPropertiesAndConfigurationShouldReturnConfigured() {
    // Given
    properties.put("jkube.generator.from", "fromInProperties");
    config.getConfig().put("test-generator", Collections.singletonMap("from", "fromInConfig"));
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getFromAsConfigured();
    // Then
    assertThat(result).isEqualTo("fromInConfig");
  }

  @Test
  @DisplayName("get from as configured with properties, should return value in properties")
  void fromAsConfiguredWithPropertiesShouldReturnValueInProperties() {
    // Given
    properties.put("jkube.generator.from", "fromInProperties");
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getFromAsConfigured();
    // Then
    assertThat(result).isEqualTo("fromInProperties");
  }

  @Test
  @DisplayName("get image name with properties and configuration, should return configured")
  void getImageNameWithPropertiesAndConfigurationShouldReturnConfigured() {
    // Given
    properties.put("jkube.generator.name", "nameInProperties");
    config.getConfig().put("test-generator", Collections.singletonMap("name", "nameInConfig"));
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getImageName();
    // Then
    assertThat(result).isEqualTo("nameInConfig");
  }

  @Test
  @DisplayName("get image name with properties, should return value in properties")
  void getImageNameWithPropertiesShouldReturnValueInProperties() {
    // Given
    properties.put("jkube.generator.name", "nameInProperties");
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getImageName();
    // Then
    assertThat(result).isEqualTo("nameInProperties");
  }

  @Test
  @DisplayName("get image name in kubernetes, should return defaults without registry")
  void getImageNameShouldReturnDefault() {
    // Given
    inKubernetes();
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getImageName();
    // Then
    assertThat(result).isEqualTo("%g/%a:%l");
  }

  @Test
  @DisplayName("get image name in openshift, should return defaults without registry")
  void getImageNameOpenShiftShouldReturnDefaultWithoutRegistry() {
    // Given
    inOpenShift();
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getImageName();
    // Then
    assertThat(result).isEqualTo("%a:%l");
  }

  @Test
  @DisplayName("get registry with properties and configuration, should return configured")
  void getRegistryWithPropertiesAndConfigurationShouldReturnConfigured() {
    // Given
    inKubernetes();
    properties.put("jkube.generator.registry", "registryInProperties");
    config.getConfig().put("test-generator", Collections.singletonMap("registry", "registryInConfiguration"));
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getRegistry();
    // Then
    assertThat(result).isEqualTo("registryInConfiguration");
  }

  @Test
  @DisplayName("get registry with properties, should return value in properties")
  void getRegistryWithPropertiesShouldReturnValueInProperties() {
    // Given
    inKubernetes();
    properties.put("jkube.generator.registry", "registryInProperties");
    config.getConfig().put("test-generator", Collections.singletonMap("registry", "registryInConfiguration"));
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getRegistry();
    // Then
    assertThat(result).isEqualTo("registryInConfiguration");
  }

  @Test
  @DisplayName("get registry in openshift, should return null")
  void getRegistryInOpenshiftShouldReturnNull() {
    // Given
    inOpenShift();
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getRegistry();
    // Then
    assertThat(result).isNull();
  }

  private TestBaseGenerator createGenerator(FromSelector fromSelector) {
    return fromSelector != null ? new TestBaseGenerator(ctx, "test-generator", fromSelector)
        : new TestBaseGenerator(ctx, "test-generator");
  }

  @Nested
  @DisplayName("add from")
  class AddFrom {
    @Test
    @DisplayName("with defaults, should add null")
    void addFromWithDefaultsShouldAddNull() {
      // Given
      final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
      // When
      new TestBaseGenerator(ctx, "test-generator").addFrom(builder);
      // Then
      assertThat(builder.build())
              .hasFieldOrPropertyWithValue("from", null)
              .hasFieldOrPropertyWithValue("fromExt", null);
    }

    @Test
    @DisplayName("in docker with configured image and selector, should return configured")
    void addFromInDockerWithConfiguredImageAndSelectorShouldReturnConfigured() {
      // Given
      config.getConfig().put("test-generator", Collections.singletonMap("from", "my/image"));
      final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
      // When
      new TestBaseGenerator(ctx, "test-generator", new TestFromSelector(ctx)).addFrom(builder);
      // Then
      assertThat(builder.build())
              .hasFieldOrPropertyWithValue("from", "my/image");
    }

    @Test
    @DisplayName("in docker with selector, should return selector image")
    void addFromInDockerWithSelectorShouldReturnSelectorImage() {
      // Given
      final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
      // When
      new TestBaseGenerator(ctx, "test-generator", new TestFromSelector(ctx)).addFrom(builder);

      // Then
      assertThat(builder.build())
              .hasFieldOrPropertyWithValue("from", "selectorDockerFromUpstream");
    }

    @Test
    @DisplayName("in is-tag mode with defaults, should add null")
    void addFromInIsTagModeWithDefaultsShouldAddNull() {
      // Given
      properties.put("jkube.generator.fromMode", "istag");
      final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
      // When
      new TestBaseGenerator(ctx, "test-generator").addFrom(builder);
      // Then
      assertThat(builder.build())
              .hasFieldOrPropertyWithValue("from", null)
              .hasFieldOrPropertyWithValue("fromExt", null);
    }

    @Test
    @DisplayName("in-is tag mode with configured image and selector, should return configured")
    void addFromInIsTagModeWithConfiguredImageAndSelectorShouldReturnConfigured() {
      // Given
      properties.put("jkube.generator.fromMode", "istag");
      config.getConfig().put("test-generator", Collections.singletonMap("from", "my/image"));
      final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
      // When
      new TestBaseGenerator(ctx, "test-generator", new TestFromSelector(ctx)).addFrom(builder);
      // Then
      assertThat(builder.build())
              .hasFieldOrPropertyWithValue("from", "image:latest")
              .extracting(BuildConfiguration::getFromExt, InstanceOfAssertFactories.MAP)
              .contains(
                      entry("kind", "ImageStreamTag"),
                      entry("name", "image:latest"),
                      entry("namespace", "my")
              );
    }

    @Test
    @DisplayName("in is-tag mode with configured image with tag and selector, should return configured")
    void addFromInIsTagModeWithConfiguredImageWithTagAndSelectorShouldReturnConfigured() {
      // Given
      properties.put("jkube.generator.fromMode", "istag");
      config.getConfig().put("test-generator", Collections.singletonMap("from", "my/image:tag"));
      final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
      // When
      new TestBaseGenerator(ctx, "test-generator", new TestFromSelector(ctx)).addFrom(builder);
      // Then
      assertThat(builder.build())
              .hasFieldOrPropertyWithValue("from", "image:tag")
              .extracting(BuildConfiguration::getFromExt, InstanceOfAssertFactories.MAP)
              .contains(
                      entry("kind", "ImageStreamTag"),
                      entry("name", "image:tag"),
                      entry("namespace", "my")
              );
    }

    @Test
    @DisplayName("in is-tag mode with selector, should return selector image")
    void addFromInIsTagModeWithSelectorShouldReturnSelectorImage() {
      // Given
      properties.put("jkube.generator.fromMode", "istag");
      final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
      // When
      new TestBaseGenerator(ctx, "test-generator", new TestFromSelector(ctx)).addFrom(builder);
      // Then
      assertThat(builder.build())
              .hasFieldOrPropertyWithValue("from", "selectorIstagFromUpstream")
              .extracting(BuildConfiguration::getFromExt, InstanceOfAssertFactories.MAP)
              .contains(
                      entry("kind", "ImageStreamTag"),
                      entry("name", "selectorIstagFromUpstream"),
                      entry("namespace", "openshift"));
    }

    @Test
    @DisplayName("with invalid mode, should throw exception")
    void addFromWithInvalidModeShouldThrowException() {
      // Given
      properties.put("jkube.generator.fromMode", "invalid");
      final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
      final TestBaseGenerator testBaseGenerator = new TestBaseGenerator(ctx, "test-generator", new TestFromSelector(ctx));
      // When
      assertThatThrownBy(()-> testBaseGenerator.addFrom(builder)).
              isInstanceOf(IllegalArgumentException.class).
              hasMessageContaining("Invalid 'fromMode' in generator configuration for 'test-generator'");
    }
  }

  @Test
  @DisplayName("should add default image")
  void shouldAddDefaultImage() {
    ImageConfiguration ic1 = mock(ImageConfiguration.class);
    ImageConfiguration ic2 = mock(ImageConfiguration.class);
    BuildConfiguration bc = mock(BuildConfiguration.class);
    when(ic1.getBuildConfiguration()).thenReturn(bc);
    when(ic2.getBuildConfiguration()).thenReturn(null);
    BaseGenerator generator = createGenerator(null);
    assertThat(generator)
        .returns(true, g -> g.shouldAddGeneratedImageConfiguration(Collections.emptyList()))
        .returns(false, g -> g.shouldAddGeneratedImageConfiguration(Arrays.asList(ic1, ic2)))
        .returns(true, g -> g.shouldAddGeneratedImageConfiguration(Collections.singletonList(ic2)))
        .returns(false, g -> g.shouldAddGeneratedImageConfiguration(Collections.singletonList(ic1)));
  }

  @Test
  @DisplayName("should not add default image in case of simple dockerfile")
  void shouldNotAddDefaultImageInCaseOfSimpleDockerfile(@TempDir Path folder) throws IOException {
    // Given
    File projectBaseDir = Files.createDirectory(folder.resolve("test-project-dir")).toFile();
    File dockerFile = new File(projectBaseDir, "Dockerfile");
    boolean isTestDockerfileCreated = dockerFile.createNewFile();
    when(ctx.getProject()).thenReturn(project);
    when(project.getBaseDirectory()).thenReturn(projectBaseDir);
    // When
    BaseGenerator generator = createGenerator(null);

    // Then
    assertThat(isTestDockerfileCreated).isTrue();
    assertThat(generator.shouldAddGeneratedImageConfiguration(Collections.emptyList())).isFalse();
  }

  @Test
  @DisplayName("should add generated image configuration when add enabled via config, should return true")
  void shouldAddGeneratedImageConfiguration_whenAddEnabledViaConfig_shouldReturnTrue() {
    // Given
    when(ctx.getProject()).thenReturn(project);
    properties.put("jkube.generator.test-generator.add", "true");
    BaseGenerator generator = createGenerator(null);

    // When
    boolean result = generator.shouldAddGeneratedImageConfiguration(createNewImageConfigurationList());

    // Then
    assertThat(result).isTrue();
  }


  @Test
  @DisplayName("should add generated image configuration when enabled via property, should return true")
  void shouldAddGeneratedImageConfiguration_whenAddEnabledViaProperty_shouldReturnTrue() {
    // Given
    when(ctx.getProject()).thenReturn(project);
    properties.put("jkube.generator.add", "true");
    BaseGenerator generator = createGenerator(null);

    // When
    boolean result = generator.shouldAddGeneratedImageConfiguration(createNewImageConfigurationList());

    // Then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("add latest tag if project's version is SNAPSHOT")
  void addLatestTagIfSnapshot() {
    when(ctx.getProject()).thenReturn(project);
    when(project.getVersion()).thenReturn("1.2-SNAPSHOT");
    BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
    BaseGenerator generator = createGenerator(null);
    generator.addLatestTagIfSnapshot(builder);
    BuildConfiguration config = builder.build();
    List<String> tags = config.getTags();
    assertThat(tags)
        .singleElement()
        .asString()
        .endsWith("latest");
  }

  @Test
  @DisplayName("add tags from config")
  void addTagsFromConfig() {
    when(ctx.getProject()).thenReturn(project);
    BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
    properties.put("jkube.generator.test-generator.tags", " tag-1, tag-2 , other-tag");
    BaseGenerator generator = createGenerator(null);
    generator.addTagsFromConfig(builder);
    BuildConfiguration config = builder.build();
    assertThat(config.getTags())
        .hasSize(3)
        .containsExactlyInAnyOrder("tag-1", "tag-2", "other-tag");
  }

  @Test
  @DisplayName("add tags from property")
  void addTagsFromProperty() {
    when(ctx.getProject()).thenReturn(project);
    BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
    properties.put("jkube.generator.tags", " tag-1, tag-2 , other-tag");
    BaseGenerator generator = createGenerator(null);
    generator.addTagsFromConfig(builder);
    BuildConfiguration config = builder.build();
    assertThat(config.getTags())
        .hasSize(3)
        .containsExactlyInAnyOrder("tag-1", "tag-2", "other-tag");
  }

  private void inKubernetes() {
    when(ctx.getRuntimeMode()).thenReturn(RuntimeMode.KUBERNETES);
  }

  private void inOpenShift() {
    when(ctx.getRuntimeMode()).thenReturn(RuntimeMode.OPENSHIFT);
  }

  private static class TestBaseGenerator extends BaseGenerator {
    public TestBaseGenerator(GeneratorContext context, String name) {
      super(context, name);
    }

    public TestBaseGenerator(GeneratorContext context, String name, FromSelector fromSelector) {
      super(context, name, fromSelector);
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
      return true;
    }

    @Override
    public List<ImageConfiguration> customize(List<ImageConfiguration> existingConfigs, boolean prePackagePhase) {
      return existingConfigs;
    }
  }

  private static class TestFromSelector extends FromSelector {

    public TestFromSelector(GeneratorContext context) {
      super(context);
    }

    @Override
    protected String getDockerBuildFrom() {
      return "selectorDockerFromUpstream";
    }

    @Override
    protected String getS2iBuildFrom() {
      return "selectorS2iFromUpstream";
    }

    @Override
    protected String getIstagFrom() {
      return "selectorIstagFromUpstream";
    }
  }

  private List<ImageConfiguration> createNewImageConfigurationList() {
    return Collections.singletonList(ImageConfiguration.builder()
        .name("test:latest")
        .build(BuildConfiguration.builder().from("foo:latest").build())
        .build());
  }
}
