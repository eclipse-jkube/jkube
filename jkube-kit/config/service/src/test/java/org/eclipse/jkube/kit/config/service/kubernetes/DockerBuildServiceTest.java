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

import mockit.Expectations;
import mockit.Verifications;
import org.eclipse.jkube.kit.build.service.docker.BuildService;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImagePullManager;
import mockit.Mocked;
import mockit.VerificationsInOrder;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

public class DockerBuildServiceTest {

  @Mocked
  private JKubeServiceHub jKubeServiceHub;

  @Test
  public void build_withValidConfiguration_shouldBuildAndTag() throws Exception {
    // Given
    final ImageConfiguration image = ImageConfiguration.builder()
        .name("image-name")
        .build(BuildConfiguration.builder()
            .from("from")
            .build()
        ).build();
    // When
    new DockerBuildService(jKubeServiceHub).build(image);
    // Then
    // @formatter:off
    new VerificationsInOrder() {{
      final BuildService buildService = jKubeServiceHub.getDockerServiceHub().getBuildService();
      buildService.buildImage(image, (ImagePullManager) any, withInstanceOf(JKubeConfiguration.class)); times = 1;
      buildService.tagImage("image-name", image); times = 1;
    }};
    // @formatter:on
  }

  @Test
  public void build_withFailure_shouldThrowException() throws Exception {
    // Given
    // @formatter:off
    new Expectations() {{
      jKubeServiceHub.getDockerServiceHub().getBuildService()
          .buildImage((ImageConfiguration) any, withInstanceOf(ImagePullManager.class), withInstanceOf(JKubeConfiguration.class));
      result = new IOException("Mock IO error");
    }};
    // @formatter:on
    // When
    final JKubeServiceException result = assertThrows(JKubeServiceException.class, () ->
        new DockerBuildService(jKubeServiceHub).build(null));
    // Then
    assertThat(result).hasMessage("Error while trying to build the image: Mock IO error");
  }

  @Test
  public void push_withDefaults_shouldPush() throws Exception {
    // When
    new DockerBuildService(jKubeServiceHub).push(Collections.emptyList(), 0, null, false);
    // Then
    // @formatter:off
    new Verifications() {{
      jKubeServiceHub.getDockerServiceHub().getRegistryService()
          .pushImages(Collections.emptyList(), 0, null, false); times = 1;
    }};
    // @formatter:on
  }
}
