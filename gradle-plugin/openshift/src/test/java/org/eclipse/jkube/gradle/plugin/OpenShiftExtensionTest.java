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
package org.eclipse.jkube.gradle.plugin;

import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenShiftExtensionTest {

  @Test
  void getRuntimeMode_withDefaults_shouldReturnOpenShift() {
    // Given
    final OpenShiftExtension partial = mock(OpenShiftExtension.class);
    when(partial.getRuntimeMode()).thenCallRealMethod();
    // When
    final RuntimeMode result = partial.getRuntimeMode();
    // Then
    assertThat(result).isEqualTo(RuntimeMode.OPENSHIFT);
  }

  @Test
  void getPlatformMode_withDefaults_shouldReturnOpenShift() {
    final OpenShiftExtension partial = mock(OpenShiftExtension.class);
    when(partial.getPlatformMode()).thenCallRealMethod();
    final PlatformMode result = partial.getPlatformMode();
    assertThat(result).isEqualTo(PlatformMode.openshift);
  }

  @ParameterizedTest(name = "buildStrategy = {0} isDockerAccessRequired = {1}")
  @CsvSource({"s2i,false", "docker,false", "jib,false", "spring,true"})
  void isDockerAccessRequired_whenBuildStrategyProvided_thenReturnTrueOnlyForSpring(String buildStrategy, boolean expectedResult) {
    // Given
    final OpenShiftExtension partial = mock(OpenShiftExtension.class);
    when(partial.getBuildStrategyOrDefault()).thenReturn(JKubeBuildStrategy.valueOf(buildStrategy));
    when(partial.isDockerAccessRequired()).thenCallRealMethod();

    // When + Then
    assertThat(partial.isDockerAccessRequired()).isEqualTo(expectedResult);
  }
}
