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
package org.eclipse.jkube.kit.build.service.docker;

import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BuildServiceTest {
  private DockerAccess mockedDockerAccess;
  private BuildService buildService;
  private ImageConfiguration imageConfiguration;
  private ImagePullManager mockedImagePullManager;
  private JKubeConfiguration mockedJKubeConfiguration;

  @Before
  public void setUp() {
    mockedDockerAccess = mock(DockerAccess.class, RETURNS_DEEP_STUBS);
    ArchiveService mockedArchiveService = mock(ArchiveService.class, RETURNS_DEEP_STUBS);
    RegistryService mockedRegistryService = mock(RegistryService.class, RETURNS_DEEP_STUBS);
    KitLogger mockedLog = mock(KitLogger.SilentLogger.class, RETURNS_DEEP_STUBS);
    mockedImagePullManager = mock(ImagePullManager.class, RETURNS_DEEP_STUBS);
    mockedJKubeConfiguration = mock(JKubeConfiguration.class, RETURNS_DEEP_STUBS);
    QueryService mockedQueryService = new QueryService(mockedDockerAccess);
    buildService = new BuildService(mockedDockerAccess, mockedQueryService, mockedRegistryService, mockedArchiveService, mockedLog);
    imageConfiguration = ImageConfiguration.builder()
        .name("image-name")
        .build(BuildConfiguration.builder()
            .from("from")
            .tags(Collections.singletonList("latest"))
            .build()
        ).build();
  }

  @Test
  public void buildImage_whenValidImageConfigurationProvidedAndDockerDaemonReturnsValidId_shouldBuildImage() throws IOException {
    // Given
    when(mockedDockerAccess.getImageId("image-name")).thenReturn("c8003cb6f5db");

    // When
    buildService.buildImage(imageConfiguration, mockedImagePullManager, mockedJKubeConfiguration);

    // Then
    verify(mockedDockerAccess, times(1))
        .buildImage(eq("image-name"), any(), any());
  }

  @Test
  public void buildImage_whenValidImageConfigurationProvidedAndDockerDaemonReturnsNull_shouldBuildImage() throws IOException {
    // Given
    when(mockedDockerAccess.getImageId("image-name")).thenReturn(null);

    // When
    IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
        () -> buildService.buildImage(imageConfiguration, mockedImagePullManager, mockedJKubeConfiguration));

    // Then
    assertThat(illegalStateException)
        .hasMessage("Failure in building image, unable to find image built with name image-name");
  }

  @Test
  public void tagImage_whenValidImageConfigurationProvided_shouldTagImage() throws DockerAccessException {
    // When
    buildService.tagImage("image-name", imageConfiguration);

    // Then
    verify(mockedDockerAccess, times(1))
            .tag("image-name", "image-name:latest", true);
  }

}
