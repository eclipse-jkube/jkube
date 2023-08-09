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
package org.eclipse.jkube.maven.plugin.mojo.build;

import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class OpenShiftPushMojoTest {
  private OpenshiftPushMojo ocPushMojo;

  @BeforeEach
  void setup() {
    ocPushMojo = new OpenshiftPushMojo();
  }

  @ParameterizedTest(name = "buildStrategy = {0} isDockerAccessRequired = {1}")
  @CsvSource({"s2i,false", "docker,false", "jib,false", "spring,true"})
  void isDockerAccessRequired_whenBuildStrategyProvided_thenReturnTrueOnlyForSpring(String buildStrategy, boolean expectedResult) {
    // Given
    ocPushMojo.buildStrategy = JKubeBuildStrategy.valueOf(buildStrategy);

    // When + Then
    assertThat(ocPushMojo.isDockerAccessRequired()).isEqualTo(expectedResult);
  }

  @Test
  void getLogPrefix() {
    assertThat(ocPushMojo.getLogPrefix()).isEqualTo("oc: ");
  }

  @Test
  void getConfiguredRuntimeMode() {
    assertThat(ocPushMojo.getConfiguredRuntimeMode()).isEqualTo(RuntimeMode.OPENSHIFT);
  }
}
