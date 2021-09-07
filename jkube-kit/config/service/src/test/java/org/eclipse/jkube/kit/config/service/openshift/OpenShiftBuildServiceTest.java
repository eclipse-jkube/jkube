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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.Collections;

import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import mockit.Mocked;
import mockit.Verifications;
import org.junit.Test;

@SuppressWarnings("unused")
public class OpenShiftBuildServiceTest {

  @Mocked
  private JKubeServiceHub jKubeServiceHub;

  @Test
  public void push_withDefaults_shouldLogWarning() throws JKubeServiceException {
    // When
    new OpenshiftBuildService(jKubeServiceHub).push(Collections.emptyList(), 0, new RegistryConfig(), false);
    // Then
    //  @formatter:off
    new Verifications() {{
      jKubeServiceHub.getLog().warn("Image is pushed to OpenShift's internal registry during oc:build goal. Skipping..."); times = 1;
    }};
    // @formatter:on
  }

  @Test
  public void initClient_withNoOpenShift_shouldThrowException() {
    // Given
    //  @formatter:off
    ImageConfiguration imageConfiguration = ImageConfiguration.builder()
      .name("foo/bar:latest")
      .build(BuildConfiguration.builder()
        .from("baseimage:latest")
        .build())
      .build();
    // @formatter:on
    OpenshiftBuildService openshiftBuildService = new OpenshiftBuildService(jKubeServiceHub);

    // When + Then
    IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> openshiftBuildService.build(imageConfiguration));
    assertThat(illegalStateException.getMessage())
        .isEqualTo("OpenShift platform has been specified but OpenShift has not been detected!");
  }
}
