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

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import org.eclipse.jkube.kit.build.service.docker.access.CreateImageOptions;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;

import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RegistryServiceTest {

  private DockerAccess dockerAccess;
  private QueryService queryService;
  private RegistryService registryService;

  @Before
  public void setUp() throws Exception {
    dockerAccess = mock(DockerAccess.class);
    queryService = mock(QueryService.class);
    registryService = new RegistryService(dockerAccess, queryService, new KitLogger.SilentLogger());
  }

  @Test
  public void pullImageWithPolicy_pullPolicyNeverAndNoImage_shouldThrowException() {
    // When
    final IOException result = assertThrows(IOException.class, () -> registryService.pullImageWithPolicy("image",
        ImagePullManager.createImagePullManager("Never", "", new Properties()), null, null));
    // Then
    assertThat(result).hasMessageStartingWith("No image 'image' found and pull policy 'Never' is set");
  }

  @Test
  public void pullImageWithPolicy_pullPolicyNeverAndImage_shouldDoNothing() throws Exception {
    // Given
    when(queryService.hasImage("image")).thenReturn(true);
    // When
    registryService.pullImageWithPolicy("image", ImagePullManager.createImagePullManager(
        "Never", "", new Properties()), null, null);
    // Then
    verify(dockerAccess, times(0)).pullImage(any(), any(), any(), any());
  }

  @Test
  public void pullImageWithPolicy_pullPolicyAlways_shouldPull() throws Exception {
    // Given
    final ArgumentCaptor<CreateImageOptions>  createImageOptionsCaptor = ArgumentCaptor.forClass(CreateImageOptions.class);
    // When
    registryService.pullImageWithPolicy("quay.io/organization/image:version",
        ImagePullManager.createImagePullManager("Always", "", new Properties()),
        RegistryConfig.builder().settings(Collections.emptyList()).build(), new BuildConfiguration());
    // Then
    verify(dockerAccess, times(1)).pullImage(eq("quay.io/organization/image:version"), any(),
        eq("quay.io"), createImageOptionsCaptor.capture());
    assertThat(createImageOptionsCaptor.getValue().getOptions()).containsExactlyInAnyOrderEntriesOf(
        new CreateImageOptions().fromImage("quay.io/organization/image").tag("version").getOptions());
  }

  @Test
  public void pullImageWithPolicy_pullPolicyAlwaysAndBuildConfiguration_shouldPull() throws Exception {
    // Given
    final BuildConfiguration bc = BuildConfiguration.builder()
        .createImageOptions(Collections.singletonMap("platform", "linux/amd64")).build();
    final ArgumentCaptor<CreateImageOptions>  createImageOptionsCaptor = ArgumentCaptor.forClass(CreateImageOptions.class);
    // When
    registryService.pullImageWithPolicy("quay.io/organization/image:version",
        ImagePullManager.createImagePullManager("Always", "", new Properties()),
        RegistryConfig.builder().settings(Collections.emptyList()).build(), bc);
    // Then
    verify(dockerAccess, times(1)).pullImage(eq("quay.io/organization/image:version"), any(),
        eq("quay.io"), createImageOptionsCaptor.capture());
    assertThat(createImageOptionsCaptor.getValue().getOptions()).containsExactlyInAnyOrderEntriesOf(
        new CreateImageOptions().fromImage("quay.io/organization/image").tag("version").platform("linux/amd64").getOptions());
  }
}
