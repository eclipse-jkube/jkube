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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.entry;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author roland
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseGeneratorTest {

  @Mock
  private GeneratorContext ctx;

  @Mock
  private JavaProject project;

  private Properties properties;
  private ProcessorConfig config;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void setUp() {
    properties = new Properties();
    config = new ProcessorConfig();
    when(ctx.getProject()).thenReturn(project);
    when(project.getProperties()).thenReturn(properties);
    when(ctx.getConfig()).thenReturn(config);
  }

  @After
  public void tearDown() {
    config = null;
    properties = null;
  }

  @Test
  public void fromAsConfiguredWithPropertiesAndConfigurationShouldReturnConfigured() {
    // Given
    properties.put("jkube.generator.from", "fromInProperties");
    config.getConfig().put("test-generator", Collections.singletonMap("from", "fromInConfig"));
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getFromAsConfigured();
    // Then
    assertEquals("fromInConfig", result);
  }

  @Test
  public void fromAsConfiguredWithPropertiesShouldReturnValueInProperties() {
    // Given
    properties.put("jkube.generator.from", "fromInProperties");
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getFromAsConfigured();
    // Then
    assertEquals("fromInProperties", result);
  }

  @Test
  public void getImageNameWithPropertiesAndConfigurationShouldReturnConfigured() {
    // Given
    properties.put("jkube.generator.name", "nameInProperties");
    config.getConfig().put("test-generator", Collections.singletonMap("name", "nameInConfig"));
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getImageName();
    // Then
    assertEquals("nameInConfig", result);
  }

  @Test
  public void getImageNameWithPropertiesShouldReturnValueInProperties() {
    // Given
    properties.put("jkube.generator.name", "nameInProperties");
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getImageName();
    // Then
    assertEquals("nameInProperties", result);
  }

  @Test
  public void getImageNameShouldReturnDefault() {
    // Given
    inKubernetes();
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getImageName();
    // Then
    assertEquals("%g/%a:%l", result);
  }

  @Test
  public void getImageNameOpenShiftShouldReturnDefaultWithoutRegistry() {
    // Given
    inOpenShift();
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getImageName();
    // Then
    assertEquals("%a:%l", result);
  }

  @Test
  public void getRegistryWithPropertiesAndConfigurationShouldReturnConfigured() {
    // Given
    inKubernetes();
    properties.put("jkube.generator.registry", "registryInProperties");
    config.getConfig().put("test-generator", Collections.singletonMap("registry", "registryInConfiguration"));
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getRegistry();
    // Then
    assertEquals("registryInConfiguration", result);
  }

  @Test
  public void getRegistryWithPropertiesShouldReturnValueInProperties() {
    // Given
    inKubernetes();
    properties.put("jkube.generator.registry", "registryInProperties");
    config.getConfig().put("test-generator", Collections.singletonMap("registry", "registryInConfiguration"));
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getRegistry();
    // Then
    assertEquals("registryInConfiguration", result);
  }

  @Test
  public void getRegistryInOpenshiftShouldReturnNull() {
    // Given
    inOpenShift();
    // When
    final String result = new TestBaseGenerator(ctx, "test-generator").getRegistry();
    // Then
    assertNull(result);
  }

  public TestBaseGenerator createGenerator(FromSelector fromSelector) {
    return fromSelector != null ? new TestBaseGenerator(ctx, "test-generator", fromSelector)
        : new TestBaseGenerator(ctx, "test-generator");
  }

  @Test
  public void addFromWithDefaultsShouldAddNull() {
    // Given
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
    // When
    new TestBaseGenerator(ctx, "test-generator").addFrom(builder);
    // Then
    assertNull(builder.build().getFrom());
    assertNull(builder.build().getFromExt());
  }

  @Test
  public void addFromInDockerWithConfiguredImageAndSelectorShouldReturnConfigured() {
    // Given
    config.getConfig().put("test-generator", Collections.singletonMap("from", "my/image"));
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
    // When
    new TestBaseGenerator(ctx, "test-generator", new TestFromSelector(ctx)).addFrom(builder);
    // Then
    assertEquals("my/image", builder.build().getFrom());
  }

  @Test
  public void addFromInDockerWithSelectorShouldReturnSelectorImage() {
    // Given
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
    // When
    new TestBaseGenerator(ctx, "test-generator", new TestFromSelector(ctx)).addFrom(builder);
    ;
    // Then
    assertEquals("selectorDockerFromUpstream", builder.build().getFrom());
  }

  @Test
  public void addFromInIsTagModeWithDefaultsShouldAddNull() {
    // Given
    properties.put("jkube.generator.fromMode", "istag");
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
    // When
    new TestBaseGenerator(ctx, "test-generator").addFrom(builder);
    // Then
    assertNull(builder.build().getFrom());
    assertNull(builder.build().getFromExt());
  }

  @Test
  public void addFromInIsTagModeWithConfiguredImageAndSelectorShouldReturnConfigured() {
    // Given
    properties.put("jkube.generator.fromMode", "istag");
    config.getConfig().put("test-generator", Collections.singletonMap("from", "my/image"));
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
    // When
    new TestBaseGenerator(ctx, "test-generator", new TestFromSelector(ctx)).addFrom(builder);
    // Then
    assertEquals("image:latest", builder.build().getFrom());
    assertThat(builder.build().getFromExt()).contains(
            entry("kind", "ImageStreamTag"),
            entry("name", "image:latest"),
            entry("namespace", "my")
    );
  }

  @Test
  public void addFromInIsTagModeWithConfiguredImageWithTagAndSelectorShouldReturnConfigured() {
    // Given
    properties.put("jkube.generator.fromMode", "istag");
    config.getConfig().put("test-generator", Collections.singletonMap("from", "my/image:tag"));
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
    // When
    new TestBaseGenerator(ctx, "test-generator", new TestFromSelector(ctx)).addFrom(builder);
    // Then
    assertEquals("image:tag", builder.build().getFrom());
    assertThat(builder.build().getFromExt()).contains(
            entry("kind", "ImageStreamTag"),
            entry("name", "image:tag"),
            entry("namespace", "my")
    );
  }

  @Test
  public void addFromInIsTagModeWithSelectorShouldReturnSelectorImage() {
    // Given
    properties.put("jkube.generator.fromMode", "istag");
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
    // When
    new TestBaseGenerator(ctx, "test-generator", new TestFromSelector(ctx)).addFrom(builder);
    // Then
    assertEquals("selectorIstagFromUpstream", builder.build().getFrom());
    assertThat(builder.build().getFromExt()).contains(
            entry("kind", "ImageStreamTag"),
            entry("name", "selectorIstagFromUpstream"),
            entry("namespace", "openshift")
    );
  }

  @Test
  public void addFromWithInvalidModeShouldThrowException() {
    // Given
    properties.put("jkube.generator.fromMode", "invalid");
    final BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
    final TestBaseGenerator testBaseGenerator = new TestBaseGenerator(ctx, "test-generator", new TestFromSelector(ctx));
    // When
    final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
      testBaseGenerator.addFrom(builder);
      fail();
    });
    // Then
    assertThat(ex).hasMessageContaining("Invalid 'fromMode' in generator configuration for 'test-generator'");;
  }

  @Test
  public void shouldAddDefaultImage() {
    ImageConfiguration ic1 = mock(ImageConfiguration.class);
    ImageConfiguration ic2 = mock(ImageConfiguration.class);
    BuildConfiguration bc = mock(BuildConfiguration.class);
    when(ic1.getBuildConfiguration()).thenReturn(bc);
    when(ic2.getBuildConfiguration()).thenReturn(null);
    BaseGenerator generator = createGenerator(null);
    assertTrue(generator.shouldAddGeneratedImageConfiguration(Collections.emptyList()));
    assertFalse(generator.shouldAddGeneratedImageConfiguration(Arrays.asList(ic1, ic2)));
    assertTrue(generator.shouldAddGeneratedImageConfiguration(Collections.singletonList(ic2)));
    assertFalse(generator.shouldAddGeneratedImageConfiguration(Collections.singletonList(ic1)));
  }

  @Test
  public void shouldNotAddDefaultImageInCaseOfSimpleDockerfile() throws IOException {
    // Given
    File projectBaseDir = folder.newFolder("test-project-dir");
    File dockerFile = new File(projectBaseDir, "Dockerfile");
    boolean isTestDockerfileCreated = dockerFile.createNewFile();
    when(ctx.getProject()).thenReturn(project);
    when(project.getBaseDirectory()).thenReturn(projectBaseDir);
    // When
    BaseGenerator generator = createGenerator(null);

    // Then
    assertTrue(isTestDockerfileCreated);
    assertFalse(generator.shouldAddGeneratedImageConfiguration(Collections.emptyList()));
  }

  @Test
  public void shouldAddGeneratedImageConfiguration_whenAddEnabledViaConfig_shouldReturnTrue() {
    // Given
    when(ctx.getProject()).thenReturn(project);
    properties.put("jkube.generator.test-generator.add", "true");
    BaseGenerator generator = createGenerator(null);

    // When
    boolean result = generator.shouldAddGeneratedImageConfiguration(createNewImageConfigurationList());

    // Then
    assertTrue(result);
  }


  @Test
  public void shouldAddGeneratedImageConfiguration_whenAddEnabledViaProperty_shouldReturnTrue() {
    // Given
    when(ctx.getProject()).thenReturn(project);
    properties.put("jkube.generator.add", "true");
    BaseGenerator generator = createGenerator(null);

    // When
    boolean result = generator.shouldAddGeneratedImageConfiguration(createNewImageConfigurationList());

    // Then
    assertTrue(result);
  }

  @Test
  public void addLatestTagIfSnapshot() {
    when(ctx.getProject()).thenReturn(project);
    when(project.getVersion()).thenReturn("1.2-SNAPSHOT");
    BuildConfiguration.BuildConfigurationBuilder builder = BuildConfiguration.builder();
    BaseGenerator generator = createGenerator(null);
    generator.addLatestTagIfSnapshot(builder);
    BuildConfiguration config = builder.build();
    List<String> tags = config.getTags();
    assertEquals(1, tags.size());
    assertTrue(tags.get(0).endsWith("latest"));
  }

  @Test
  public void addTagsFromConfig() {
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
  public void addTagsFromProperty() {
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

  public void inKubernetes() {
    when(ctx.getRuntimeMode()).thenReturn(RuntimeMode.KUBERNETES);
  }

  public void inOpenShift() {
    when(ctx.getRuntimeMode()).thenReturn(RuntimeMode.OPENSHIFT);
  }

  private class TestBaseGenerator extends BaseGenerator {
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

  private class TestFromSelector extends FromSelector {

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
