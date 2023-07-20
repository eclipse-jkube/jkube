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
package org.eclipse.jkube.kit.config.service.kubernetes;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.jkube.kit.build.service.docker.BuildService;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DockerBuildServiceTest {

  private JKubeServiceHub mockedJKubeServiceHub;

  private BuildService mockedDockerBuildService;

  private ImageConfiguration image;

  private ImageConfiguration imageWithSkipEnabled;

  @BeforeEach
  void setUp() {
    mockedJKubeServiceHub = mock(JKubeServiceHub.class, RETURNS_DEEP_STUBS);
    mockedDockerBuildService = mock(BuildService.class, RETURNS_DEEP_STUBS);
    when(mockedJKubeServiceHub.getDockerServiceHub().getBuildService()).thenReturn(mockedDockerBuildService);
    image = ImageConfiguration.builder()
        .name("image-name")
        .build(BuildConfiguration.builder()
            .from("from")
            .build()
        ).build();
    imageWithSkipEnabled = ImageConfiguration.builder()
        .name("image-name")
        .build(BuildConfiguration.builder()
            .skip(true)
            .build())
        .build();
  }

  @Test
  void build_withInvalidConfiguration_shouldNotBuildAndTag() throws Exception {
    // When
    image.setBuild(null);
    new DockerBuildService(mockedJKubeServiceHub).build(image);
    // Then
    verify(mockedDockerBuildService, times(0))
        .buildImage(eq(image), any(), any());
    verify(mockedDockerBuildService, times(0))
        .tagImage(anyString(), eq(image));
  }

  @Test
  void build_withValidConfiguration_shouldBuildAndTag() throws Exception {
    // When
    new DockerBuildService(mockedJKubeServiceHub).build(image);
    // Then
    verify(mockedDockerBuildService, times(1))
        .buildImage(eq(image), any(), any());
    verify(mockedDockerBuildService, times(1))
        .tagImage("image-name", image);
  }

  @Test
  void build_withValidConfiguration_shouldCallPluginServiceAddFiles() throws Exception {
    // When
    new DockerBuildService(mockedJKubeServiceHub).build(image);
    // Then
    verify(mockedJKubeServiceHub.getPluginManager().resolvePluginService(), times(1))
      .addExtraFiles();
  }

  @Test
  void build_withImageBuildConfigurationSkipEnabled_shouldNotBuildAndTag() throws Exception {
    // When
    new DockerBuildService(mockedJKubeServiceHub).build(imageWithSkipEnabled);
    // Then
    verify(mockedDockerBuildService, times(0))
        .buildImage(eq(image), any(), any());
    verify(mockedDockerBuildService, times(0))
        .tagImage(anyString(), eq(image));
  }

  @Test
  void build_withFailure_shouldThrowException() throws Exception {
    // Given
    doThrow(new IOException("Mock IO error")).when(mockedDockerBuildService).buildImage(eq(image), any(), any());
    // When + Then
    assertThatExceptionOfType(JKubeServiceException.class)
        .isThrownBy(() -> new DockerBuildService(mockedJKubeServiceHub).build(image))
        .withMessage("Error while trying to build the image: Mock IO error");
  }

  @Test
  void push_withDefaults_shouldPush() throws Exception {
    // When
    new DockerBuildService(mockedJKubeServiceHub).push(Collections.emptyList(), 0, null, false);
    // Then
    verify(mockedJKubeServiceHub.getDockerServiceHub().getRegistryService(), times(0))
        .pushImage(any(), eq(0), isNull(), eq(false));
  }

  @Test
  void push_withImageBuildConfigurationSkipEnabled_shouldNotPush() throws Exception {
    // When
    new DockerBuildService(mockedJKubeServiceHub).build(imageWithSkipEnabled);
    // Then
    verify(mockedJKubeServiceHub.getDockerServiceHub().getRegistryService(), times(0))
        .pushImage(any(), eq(0), isNull(), eq(false));
  }
}
