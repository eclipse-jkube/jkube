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
package org.eclipse.jkube.maven.plugin.mojo.build;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;
import org.eclipse.jkube.kit.common.util.validator.ResourceValidator;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceService;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.enricher.api.DefaultEnricherManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceMojoTest {

  private MockedConstruction<DefaultEnricherManager> defaultEnricherManagerMockedConstruction;
  private MockedStatic<MavenUtil> mavenUtilMockedStatic;
  private ResourceMojo resourceMojo;
  private JKubeServiceHub mockedJKubeServiceHub;
  private ResourceService mockedResourceService;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws IOException {
    ImageConfigResolver mockedImageConfigResolver = mock(ImageConfigResolver.class, RETURNS_DEEP_STUBS);
    Properties properties = new Properties();
    MavenProject mockedMavenProject = mock(MavenProject.class, RETURNS_DEEP_STUBS);
    MavenSession mockedMavenSession = mock(MavenSession.class);
    mockedResourceService = mock(ResourceService.class, RETURNS_DEEP_STUBS);
    MojoExecution mockedMojoExecution = mock(MojoExecution.class, RETURNS_DEEP_STUBS);
    MojoDescriptor mockedMojoDescriptor = mock(MojoDescriptor.class, RETURNS_DEEP_STUBS);
    when(mockedMojoExecution.getMojoDescriptor()).thenReturn(mockedMojoDescriptor);
    when(mockedMojoExecution.getGoal()).thenReturn("k8s:resource");
    when(mockedMojoDescriptor.getFullGoalName()).thenReturn("k8s:resource");
    defaultEnricherManagerMockedConstruction = mockConstruction(DefaultEnricherManager.class);
    JavaProject javaProject = JavaProject.builder()
        .artifactId("test-project")
        .groupId("org.eclipse.jkube")
        .properties(properties)
        .compileClassPathElements(Collections.emptyList())
        .build();
    mockedJKubeServiceHub = mock(JKubeServiceHub.class, RETURNS_DEEP_STUBS);
    mavenUtilMockedStatic = mockStatic(MavenUtil.class);
    mavenUtilMockedStatic.when(() -> MavenUtil.convertMavenProjectToJKubeProject(any(), any())).thenReturn(javaProject);
    ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .name("%g/%a")
        .build(BuildConfiguration.builder()
            .from("test-base-image:latest")
            .build())
        .build();
    when(mockedImageConfigResolver.resolve(eq(imageConfiguration), any())).thenReturn(Collections.singletonList(imageConfiguration));

    this.resourceMojo = new ResourceMojo();
    this.resourceMojo.images = Collections.singletonList(imageConfiguration);
    this.resourceMojo.project = mockedMavenProject;
    this.resourceMojo.settings = mock(Settings.class, RETURNS_DEEP_STUBS);
    this.resourceMojo.session = mockedMavenSession;
    this.resourceMojo.jkubeServiceHub = mockedJKubeServiceHub;
    this.resourceMojo.log = mock(KitLogger.class, RETURNS_DEEP_STUBS);
    this.resourceMojo.skipResourceValidation = true;
    this.resourceMojo.projectHelper = mock(MavenProjectHelper.class, RETURNS_DEEP_STUBS);
    this.resourceMojo.imageConfigResolver = mockedImageConfigResolver;
    this.resourceMojo.javaProject = javaProject;
    this.resourceMojo.interpolateTemplateParameters = true;
    this.resourceMojo.resourceDir = Files.createDirectories(temporaryFolder.resolve("src").resolve("main").resolve("jkube")).toFile();
    resourceMojo.mojoExecution = mockedMojoExecution;

    when(mockedMavenProject.getProperties()).thenReturn(properties);
    when(mockedMavenSession.getGoals()).thenReturn(Collections.singletonList("k8s:resource"));
    when(mockedJKubeServiceHub.getConfiguration().getProject()).thenReturn(javaProject);
    when(mockedJKubeServiceHub.getConfiguration().getBasedir()).thenReturn(temporaryFolder.toFile());
    when(mockedJKubeServiceHub.getResourceService()).thenReturn(mockedResourceService);
  }

  @AfterEach
  void cleanUp() {
    defaultEnricherManagerMockedConstruction.close();
    mavenUtilMockedStatic.close();
  }

  @Test
  void executeInternal_whenInvoked_shouldDelegateResourceGenerationToResourceService() throws MojoExecutionException, MojoFailureException, IOException {
    // Given + When
    resourceMojo.initJKubeServiceHubBuilder(resourceMojo.javaProject);
    resourceMojo.executeInternal();

    // Then
    assertThat(defaultEnricherManagerMockedConstruction.constructed()).hasSize(1);
    assertThat(resourceMojo.resolvedImages)
        .hasSize(1)
        .asList()
        .first(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getName)
        .isEqualTo("jkube/test-project");
    verify(mockedJKubeServiceHub, times(2)).getResourceService();
    verify(mockedResourceService, times(1)).generateResources(eq(PlatformMode.kubernetes), any(), any());
    verify(mockedResourceService, times(1)).writeResources(any(), eq(ResourceClassifier.KUBERNETES), any());
  }

  @Test
  void executeInternal_whenSkipTrue_shouldDoNothing() throws MojoExecutionException, MojoFailureException, IOException {
    // Given
    this.resourceMojo.skip = true;

    // When
    resourceMojo.execute();

    // Then
    assertThat(defaultEnricherManagerMockedConstruction.constructed()).isEmpty();
    verify(mockedJKubeServiceHub, times(0)).getResourceService();
    verify(mockedResourceService, times(0)).generateResources(any(), any(), any());
    verify(mockedResourceService, times(0)).writeResources(any(), any(), any());
  }

  @Test
  void executeInternal_whenSkipResourceTrue_shouldDoNothing() throws MojoExecutionException, MojoFailureException, IOException {
    // Given
    this.resourceMojo.skipResource = true;

    // When
    resourceMojo.execute();

    // Then
    assertThat(defaultEnricherManagerMockedConstruction.constructed()).isEmpty();
    verify(mockedJKubeServiceHub, times(0)).getResourceService();
    verify(mockedResourceService, times(0)).generateResources(any(), any(), any());
    verify(mockedResourceService, times(0)).writeResources(any(), any(), any());
  }

  @Test
  void executeInternal_whenInvoked_shouldWriteResourcesFirstThenValidate() throws IOException, MojoExecutionException, MojoFailureException {
    try (MockedConstruction<ResourceValidator> resourceValidatorMockedConstruction = mockConstruction(ResourceValidator.class)) {
      // Given
      resourceMojo.initJKubeServiceHubBuilder(resourceMojo.javaProject);
      resourceMojo.skipResourceValidation = false;
      // When
      resourceMojo.executeInternal();
      // Then
      assertThat(resourceValidatorMockedConstruction.constructed()).hasSize(1);
      ResourceValidator mockResourceValidator = resourceValidatorMockedConstruction.constructed().get(0);
      InOrder inOrder = inOrder(mockedResourceService, mockResourceValidator);
      inOrder.verify(mockedResourceService).writeResources(any(), any(), any());
      inOrder.verify(mockResourceValidator).validate();
    }
  }
}
