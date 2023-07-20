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
package org.eclipse.jkube.kit.config.service;

import java.util.stream.Stream;

import org.eclipse.jkube.kit.build.service.docker.DockerServiceHub;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.LazyBuilder;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.kubernetes.DockerBuildService;
import org.eclipse.jkube.kit.config.service.kubernetes.JibBuildService;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftBuildService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

class JKubeServiceHubBuildServiceTest {

  static Stream<Arguments> data() {
    return Stream.of(
        arguments(RuntimeMode.KUBERNETES, null, DockerBuildService.class),
        arguments(RuntimeMode.KUBERNETES, JKubeBuildStrategy.docker, DockerBuildService.class),
        arguments(RuntimeMode.KUBERNETES, JKubeBuildStrategy.s2i, DockerBuildService.class),
        arguments(RuntimeMode.KUBERNETES, JKubeBuildStrategy.jib, JibBuildService.class),
        arguments(RuntimeMode.OPENSHIFT, null, OpenshiftBuildService.class),
        arguments(RuntimeMode.OPENSHIFT, JKubeBuildStrategy.docker, OpenshiftBuildService.class),
        arguments(RuntimeMode.OPENSHIFT, JKubeBuildStrategy.s2i, OpenshiftBuildService.class),
        arguments(RuntimeMode.OPENSHIFT, JKubeBuildStrategy.jib, JibBuildService.class));
  }

  @DisplayName("get build service")
  @ParameterizedTest(name = "{index}: {0} with {1} build strategy should create {2}")
  @MethodSource("data")
  void getBuildService(RuntimeMode runtimeMode, JKubeBuildStrategy buildStrategy,
      Class<? extends BuildService> buildServiceClass) {
    // Given
    final BuildServiceConfig config = BuildServiceConfig.builder().jKubeBuildStrategy(buildStrategy).build();
    final JKubeServiceHub jKubeServiceHub = JKubeServiceHub.builder()
      .platformMode(runtimeMode)
      .log(new KitLogger.SilentLogger())
      .dockerServiceHub(mock(DockerServiceHub.class, RETURNS_DEEP_STUBS))
      .configuration(new JKubeConfiguration())
      .buildServiceConfig(config)
      .resourceService(new LazyBuilder<>(hub -> null))
      .offline(true)
      .build();
    // When
    final BuildService result = jKubeServiceHub.getBuildService();
    // Then
    assertThat(result).isNotNull().isInstanceOf(buildServiceClass);
  }
}
