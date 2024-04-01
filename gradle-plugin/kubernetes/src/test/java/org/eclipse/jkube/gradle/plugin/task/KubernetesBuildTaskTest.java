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
package org.eclipse.jkube.gradle.plugin.task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.generator.api.DefaultGeneratorManager;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.kubernetes.DockerBuildService;

import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KubernetesBuildTaskTest {

  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  private MockedConstruction<DockerAccessFactory> dockerAccessFactoryMockedConstruction;
  private MockedConstruction<DockerBuildService> dockerBuildServiceMockedConstruction;
  private KubernetesExtension extension;
  private boolean isBuildServiceApplicable;
  private boolean isBuildError;

  @BeforeEach
  void setUp() throws IOException {
    // Mock required for environments with no DOCKER available (don't remove)
    dockerAccessFactoryMockedConstruction = mockConstruction(DockerAccessFactory.class,
      (mock, ctx) -> when(mock.createDockerAccess(any())).thenReturn(mock(DockerAccess.class)));
    dockerBuildServiceMockedConstruction = mockConstruction(DockerBuildService.class, (mock, ctx) -> {
      when(mock.isApplicable()).thenReturn(isBuildServiceApplicable);
      if (isBuildError) {
        doThrow(new JKubeServiceException("Exception during Build")).when(mock).build(any());
      }
    });
    isBuildServiceApplicable = true;
    isBuildError = false;
    extension = new TestKubernetesExtension();
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    extension.images = Collections.singletonList(ImageConfiguration.builder()
      .name("foo/bar:latest")
      .build(BuildConfiguration.builder()
        .dockerFile("Dockerfile")
        .build())
      .build());
  }

  @AfterEach
  void tearDown() {
    dockerBuildServiceMockedConstruction.close();
    dockerAccessFactoryMockedConstruction.close();
  }

  @Test
  void runTask_withImageConfiguration_shouldRunBuild() throws JKubeServiceException {
    // Given
    final KubernetesBuildTask buildTask = new KubernetesBuildTask(KubernetesExtension.class);
    // When
    buildTask.runTask();
    // Then
    assertThat(buildTask.jKubeServiceHub.getBuildService()).isNotNull()
      .isInstanceOf(DockerBuildService.class);
    verify(buildTask.jKubeServiceHub.getBuildService(), times(1)).build(any());
  }

  @Test
  void runTask_withImageConfigurationAndNoApplicableService_shouldThrowException() {
    // Given
    isBuildServiceApplicable = false;
    final KubernetesBuildTask buildTask = new KubernetesBuildTask(KubernetesExtension.class);
    // When & Then
    assertThatIllegalStateException()
      .isThrownBy(buildTask::runTask)
      .withMessage("No suitable Build Service was found for your current configuration");
  }

  @Test
  void runTask_withImageConfigurationAndBuildError_shouldThrowException() {
    // Given
    isBuildError = true;
    final KubernetesBuildTask buildTask = new KubernetesBuildTask(KubernetesExtension.class);
    // When & Then
    assertThatExceptionOfType(GradleException.class)
      .isThrownBy(buildTask::runTask)
      .withMessage("Exception during Build");
  }

  @Test
  void runTask_withSkipBuild_shouldDoNothing() throws JKubeServiceException {
    // Given
    extension = new TestKubernetesExtension() {
      @Override
      public Property<Boolean> getSkipBuild() {
        return super.getSkipBuild().value(true);
      }
    };
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    final KubernetesBuildTask buildTask = new KubernetesBuildTask(KubernetesExtension.class);

    // When
    buildTask.runTask();

    // Then
    assertThat(dockerBuildServiceMockedConstruction.constructed()).isEmpty();
    verify(buildTask.jKubeServiceHub.getBuildService(), times(0)).build(any());
  }

  @Test
  @DisplayName("when Dockerfile present in project base directory, then generate opinionated ImageConfiguration for Dockerfile")
  void simpleDockerfileModeWorksAsExpected(@TempDir File temporaryFolder) throws IOException {
    // Given
    File dockerFileInProjectBaseDir = temporaryFolder.toPath().resolve("Dockerfile").toFile();
    Files.createFile(dockerFileInProjectBaseDir.toPath());
    extension = new TestKubernetesExtension() {
      @Override
      public Property<File> getResourceSourceDirectory() {
        return super.getResourceSourceDirectory().value(new File(temporaryFolder, "src/main/jkube"));
      }
    };
    extension.javaProject = JavaProject.builder()
        .baseDirectory(temporaryFolder)
        .outputDirectory(temporaryFolder.toPath().resolve("target").toFile())
        .buildDirectory(temporaryFolder.toPath().resolve("target").toFile())
        .plugin(Plugin.builder().groupId("org.springframework.boot").artifactId("org.springframework.boot.gradle.plugin").build())
        .properties(new Properties()).build();
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    final KubernetesBuildTask buildTask = new KubernetesBuildTask(KubernetesExtension.class);
    buildTask.kitLogger = new KitLogger.SilentLogger();
    GeneratorContext generatorContext = buildTask.initGeneratorContextBuilder().build();
    List<ImageConfiguration> imageConfigurations = new ArrayList<>();
    DefaultGeneratorManager generatorManager = new DefaultGeneratorManager(generatorContext);

    // When
    List<ImageConfiguration> imageConfigurationList = generatorManager.generate(imageConfigurations);

    // Then
    Assertions.assertThat(imageConfigurationList)
        .hasSize(1)
        .singleElement(InstanceOfAssertFactories.type(ImageConfiguration.class))
        .extracting(ImageConfiguration::getBuild)
        .extracting(BuildConfiguration::getDockerFileRaw)
        .isEqualTo(dockerFileInProjectBaseDir.getAbsolutePath());
  }
}
