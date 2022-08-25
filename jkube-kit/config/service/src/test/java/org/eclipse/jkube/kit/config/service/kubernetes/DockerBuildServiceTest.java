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
package org.eclipse.jkube.kit.config.service.kubernetes;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.jkube.kit.build.service.docker.BuildService;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DockerBuildServiceTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private JKubeServiceHub mockedJKubeServiceHub;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private BuildService mockedDockerBuildService;

  private ImageConfiguration image;

  private ImageConfiguration imageWithSkipEnabled;

  @Before
  public void setUp() {
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
  public void build_withInvalidConfiguration_shouldNotBuildAndTag() throws Exception {
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
  public void build_withValidConfiguration_shouldBuildAndTag() throws Exception {
    // When
    new DockerBuildService(mockedJKubeServiceHub).build(image);
    // Then
    verify(mockedDockerBuildService, times(1))
        .buildImage(eq(image), any(), any());
    verify(mockedDockerBuildService, times(1))
        .tagImage("image-name", image);
  }

  @Test
  public void build_withValidConfiguration_shouldCallPluginServiceAddFiles() throws Exception {
    // When
    new DockerBuildService(mockedJKubeServiceHub).build(image);
    // Then
    verify(mockedJKubeServiceHub.getPluginManager().resolvePluginService(), times(1))
      .addExtraFiles();
  }

  @Test
  public void build_withImageBuildConfigurationSkipEnabled_shouldNotBuildAndTag() throws Exception {
    // When
    new DockerBuildService(mockedJKubeServiceHub).build(imageWithSkipEnabled);
    // Then
    verify(mockedDockerBuildService, times(0))
        .buildImage(eq(image), any(), any());
    verify(mockedDockerBuildService, times(0))
        .tagImage(anyString(), eq(image));
  }

  @Test
  public void build_withFailure_shouldThrowException() throws Exception {
    // Given
    doThrow(new IOException("Mock IO error")).when(mockedDockerBuildService).buildImage(eq(image), any(), any());
    // When
    final JKubeServiceException result = assertThrows(JKubeServiceException.class, () ->
        new DockerBuildService(mockedJKubeServiceHub).build(image));
    // Then
    assertThat(result).hasMessage("Error while trying to build the image: Mock IO error");
  }

  @Test
  public void push_withDefaults_shouldPush() throws Exception {
    // When
    new DockerBuildService(mockedJKubeServiceHub).push(Collections.emptyList(), 0, null, false);
    // Then
    verify(mockedJKubeServiceHub.getDockerServiceHub().getRegistryService(), times(0))
        .pushImage(any(), eq(0), isNull(), eq(false));
  }

  @Test
  public void push_withImageBuildConfigurationSkipEnabled_shouldNotPush() throws Exception {
    // When
    new DockerBuildService(mockedJKubeServiceHub).build(imageWithSkipEnabled);
    // Then
    verify(mockedJKubeServiceHub.getDockerServiceHub().getRegistryService(), times(0))
        .pushImage(any(), eq(0), isNull(), eq(false));
  }
}
