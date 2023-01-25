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
package org.eclipse.jkube.kit.build.service.docker.helper;

import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigHelperTest {
  private ImageConfigResolver imageConfigResolver;
  private KitLogger logger;
  private JavaProject javaProject;
  private JKubeConfiguration jKubeConfiguration;

  @BeforeEach
  void setUp() {
    imageConfigResolver = mock(ImageConfigResolver.class, RETURNS_DEEP_STUBS);
    logger = new KitLogger.SilentLogger();
    javaProject = mock(JavaProject.class, RETURNS_DEEP_STUBS);
    jKubeConfiguration = mock(JKubeConfiguration.class, RETURNS_DEEP_STUBS);
    when(jKubeConfiguration.getProject()).thenReturn(javaProject);
  }

  @Test
  void initImageConfiguration_withSimpleImageConfiguration_shouldReturnImageConfiguration() {
    // Given
    ImageConfiguration dummyImageConfiguration = ImageConfiguration.builder()
        .name("foo/bar:latest")
        .build(BuildConfiguration.builder()
            .from("foobase:latest")
            .build())
        .build();
    List<ImageConfiguration> images = new ArrayList<>();
    images.add(dummyImageConfiguration);
    when(jKubeConfiguration.getBasedir()).thenReturn(new File("dummydir"));
    when(imageConfigResolver.resolve(dummyImageConfiguration, javaProject)).thenReturn(images);

    // When
    List<ImageConfiguration> resolvedImages = ConfigHelper.initImageConfiguration("1.12", new Date(), images, imageConfigResolver, logger, null, configs -> configs, jKubeConfiguration);

    // Then
    assertThat(resolvedImages).isNotNull()
        .singleElement()
        .isEqualTo(dummyImageConfiguration);
  }

  @Test
  void initImageConfiguration_withSimpleDockerFileInProjectBaseDir_shouldCreateImageConfiguration() {
    List<ImageConfiguration> images = new ArrayList<>();
    File dockerFile = new File(getClass().getResource("/dummy-javaproject/Dockerfile").getFile());
    when(jKubeConfiguration.getBasedir()).thenReturn(dockerFile.getParentFile());
    when(javaProject.getProperties()).thenReturn(new Properties());
    when(javaProject.getGroupId()).thenReturn("org.eclipse.jkube");
    when(javaProject.getArtifactId()).thenReturn("test-java-project");
    when(javaProject.getVersion()).thenReturn("0.0.1-SNAPSHOT");

    // When
    List<ImageConfiguration> resolvedImages = ConfigHelper.initImageConfiguration("1.12", new Date(), images, imageConfigResolver, logger, null, configs -> configs, jKubeConfiguration);

    // Then
    assertThat(resolvedImages).isNotNull()
        .singleElement()
        .hasFieldOrPropertyWithValue("name", "jkube/test-java-project:latest")
        .hasFieldOrPropertyWithValue("build.dockerFile", dockerFile)
        .hasFieldOrPropertyWithValue("build.ports", Collections.singletonList("8080"));
  }

  @Test
  void initImageConfiguration_withSimpleDockerFileModeEnabledAndImageConfigurationWithNoBuild_shouldModifyExistingImageConfiguration() {
    ImageConfiguration dummyImageConfiguration = ImageConfiguration.builder()
        .name("imageconfiguration-no-build:latest")
        .build();
    List<ImageConfiguration> images = new ArrayList<>();
    images.add(dummyImageConfiguration);
    File dockerFile = new File(getClass().getResource("/dummy-javaproject/Dockerfile").getFile());
    when(imageConfigResolver.resolve(dummyImageConfiguration, javaProject)).thenReturn(images);
    when(jKubeConfiguration.getBasedir()).thenReturn(dockerFile.getParentFile());
    when(javaProject.getProperties()).thenReturn(new Properties());

    // When
    List<ImageConfiguration> resolvedImages = ConfigHelper.initImageConfiguration("1.12", new Date(), images, imageConfigResolver, logger, null, configs -> configs, jKubeConfiguration);

    // Then
    assertThat(resolvedImages).isNotNull()
        .singleElement()
        .hasFieldOrPropertyWithValue("name", "imageconfiguration-no-build:latest")
        .hasFieldOrPropertyWithValue("build.dockerFile", dockerFile)
        .hasFieldOrPropertyWithValue("build.ports", Collections.singletonList("8080"));
  }
}
