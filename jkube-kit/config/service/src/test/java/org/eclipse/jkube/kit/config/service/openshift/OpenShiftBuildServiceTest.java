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

import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.jkube.kit.build.service.docker.ArchiveService;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
class OpenShiftBuildServiceTest {

  private JKubeServiceHub jKubeServiceHub;

  private KitLogger kitLogger;

  private ImageConfiguration imageConfiguration;

  private ImageConfiguration imageConfigurationWithSkipEnabled;

  @BeforeEach
  void setUp() {
    kitLogger = spy(new KitLogger.SilentLogger());
    jKubeServiceHub = mock(JKubeServiceHub.class, RETURNS_DEEP_STUBS);
    final OpenShiftClient oc = mock(OpenShiftClient.class);
    when(jKubeServiceHub.getClient()).thenReturn(oc);
    when(jKubeServiceHub.getClusterAccess().createDefaultClient()).thenReturn(oc);
    when(oc.adapt(OpenShiftClient.class)).thenReturn(oc);
    //  @formatter:off
    imageConfiguration = ImageConfiguration.builder()
        .name("foo/bar:latest")
        .registry("harbor.xyz.local")
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
  void push_withEmptyList_shouldNotLogWarning() throws JKubeServiceException {
    // Given
    when(jKubeServiceHub.getLog()).thenReturn(kitLogger);

    // When
    new OpenshiftBuildService(jKubeServiceHub).push(Collections.emptyList(), 0, new RegistryConfig(), false);
    // Then
    verify(kitLogger, times(0)).warn("Image is pushed to OpenShift's internal registry during oc:build goal. Skipping...");
  }

  @Test
  void push_withValidImage_shouldLogWarning() throws JKubeServiceException {
    // Given
    when(jKubeServiceHub.getLog()).thenReturn(kitLogger);

    // When
    new OpenshiftBuildService(jKubeServiceHub).push(Collections.singletonList(imageConfiguration), 0, new RegistryConfig(), false);
    // Then
    verify(kitLogger, times(1)).warn("Image is pushed to OpenShift's internal registry during oc:build goal. Skipping...");
  }

  @Test
  void initClient_withNoOpenShift_shouldThrowException() {
    // Given
    when(jKubeServiceHub.getClient().adapt(OpenShiftClient.class).isSupported()).thenReturn(false);
    OpenshiftBuildService openshiftBuildService = new OpenshiftBuildService(jKubeServiceHub);
    // When + Then
    assertThatIllegalStateException()
        .isThrownBy(() -> openshiftBuildService.build(imageConfiguration))
        .withMessage("OpenShift platform has been specified but OpenShift has not been detected!");
  }

  @Test
  void build_withImageBuildConfigurationSkipEnabled_shouldNotBuildImage() throws JKubeServiceException, IOException {
    // Given
    ArchiveService mockedArchiveService = mock(ArchiveService.class, RETURNS_DEEP_STUBS);
    when(jKubeServiceHub.getDockerServiceHub().getArchiveService()).thenReturn(mockedArchiveService);
    OpenshiftBuildService openshiftBuildService = new OpenshiftBuildService(jKubeServiceHub);

    // When
    openshiftBuildService.build(imageConfigurationWithSkipEnabled);

    // Then
    verify(mockedArchiveService, times(0))
        .createDockerBuildArchive(any(), any(), any(), any());
  }

  @Test
  void getApplicableImageConfiguration_withRegistryInImageConfigurationAndDockerImageBuildOutput_shouldAppendRegistryToImageName() {
    // Given
    when(jKubeServiceHub.getBuildServiceConfig()).thenReturn(BuildServiceConfig.builder()
            .buildOutputKind("DockerImage")
        .build());
    OpenshiftBuildService openshiftBuildService = new OpenshiftBuildService(jKubeServiceHub);

    // When
    ImageConfiguration applicableImageConfig = openshiftBuildService.getApplicableImageConfiguration(imageConfiguration);

    // Then
    assertThat(applicableImageConfig)
        .hasFieldOrPropertyWithValue("name", "harbor.xyz.local/foo/bar:latest");
  }

  @Test
  void getApplicableImageConfiguration_withRegistryInImageConfiguration_shouldNotAppendRegistryToImageName() {
    // Given
    when(jKubeServiceHub.getBuildServiceConfig()).thenReturn(BuildServiceConfig.builder()
        .buildOutputKind("ImageStreamTag")
        .build());
    OpenshiftBuildService openshiftBuildService = new OpenshiftBuildService(jKubeServiceHub);

    // When
    ImageConfiguration applicableImageConfig = openshiftBuildService.getApplicableImageConfiguration(imageConfiguration);

    // Then
    assertThat(applicableImageConfig)
        .hasFieldOrPropertyWithValue("name", "foo/bar:latest");
  }
}
