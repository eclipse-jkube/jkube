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
package org.eclipse.jkube.kit.config.service.openshift;

import org.eclipse.jkube.kit.build.service.docker.ArchiveService;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
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

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
@RunWith(MockitoJUnitRunner.class)
public class OpenShiftBuildServiceTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private JKubeServiceHub jKubeServiceHub;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private KitLogger mockedKitLogger;

  private ImageConfiguration imageConfiguration;

  private ImageConfiguration imageConfigurationWithSkipEnabled;

  @Before
  public void setUp() {
    //  @formatter:off
    imageConfiguration = ImageConfiguration.builder()
        .name("foo/bar:latest")
        .build(BuildConfiguration.builder()
            .from("baseimage:latest")
            .build())
        .build();
    imageConfigurationWithSkipEnabled = ImageConfiguration.builder()
        .name("foo/bar:latest")
        .build(BuildConfiguration.builder()
            .from("baseimage:latest")
            .skip(true)
            .build())
        .build();
    // @formatter:on

  }

  @Test
  public void push_withEmptyList_shouldNotLogWarning() throws JKubeServiceException {
    // Given
    when(jKubeServiceHub.getLog()).thenReturn(mockedKitLogger);

    // When
    new OpenshiftBuildService(jKubeServiceHub).push(Collections.emptyList(), 0, new RegistryConfig(), false);
    // Then
    verify(mockedKitLogger, times(0)).warn("Image is pushed to OpenShift's internal registry during oc:build goal. Skipping...");
  }

  @Test
  public void push_withValidImage_shouldLogWarning() throws JKubeServiceException {
    // Given
    when(jKubeServiceHub.getLog()).thenReturn(mockedKitLogger);

    // When
    new OpenshiftBuildService(jKubeServiceHub).push(Collections.singletonList(imageConfiguration), 0, new RegistryConfig(), false);
    // Then
    verify(mockedKitLogger, times(1)).warn("Image is pushed to OpenShift's internal registry during oc:build goal. Skipping...");
  }

  @Test
  public void initClient_withNoOpenShift_shouldThrowException() {
    // Given
    OpenshiftBuildService openshiftBuildService = new OpenshiftBuildService(jKubeServiceHub);

    // When + Then
    IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> openshiftBuildService.build(imageConfiguration));
    assertThat(illegalStateException.getMessage())
        .isEqualTo("OpenShift platform has been specified but OpenShift has not been detected!");
  }

  @Test
  public void build_withImageBuildConfigurationSkipEnabled_shouldNotBuildImage() throws JKubeServiceException, IOException {
    // Given
    ArchiveService mockedArchiveService = mock(ArchiveService.class, RETURNS_DEEP_STUBS);
    when(jKubeServiceHub.getDockerServiceHub().getArchiveService()).thenReturn(mockedArchiveService);
    OpenshiftBuildService openshiftBuildService = new OpenshiftBuildService(jKubeServiceHub);

    // When
    openshiftBuildService.build(imageConfigurationWithSkipEnabled);

    // Then
    verify(mockedArchiveService, times(0))
        .createDockerBuildArchive(any(), any(), any());
  }
}
