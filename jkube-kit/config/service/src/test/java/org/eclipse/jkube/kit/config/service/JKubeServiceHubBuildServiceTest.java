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
package org.eclipse.jkube.kit.config.service;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.LazyBuilder;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.kubernetes.DockerBuildService;
import org.eclipse.jkube.kit.config.service.kubernetes.JibBuildService;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftBuildService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class JKubeServiceHubBuildServiceTest {

  @Parameterized.Parameters(name = "{index}: {0} with {1} build strategy should create {2}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[]{RuntimeMode.KUBERNETES, null, DockerBuildService.class},
        new Object[]{RuntimeMode.KUBERNETES, JKubeBuildStrategy.docker, DockerBuildService.class},
        new Object[]{RuntimeMode.KUBERNETES, JKubeBuildStrategy.s2i, DockerBuildService.class},
        new Object[]{RuntimeMode.KUBERNETES, JKubeBuildStrategy.jib, JibBuildService.class},
        new Object[]{RuntimeMode.OPENSHIFT, null, OpenshiftBuildService.class},
        new Object[]{RuntimeMode.OPENSHIFT, JKubeBuildStrategy.docker, OpenshiftBuildService.class},
        new Object[]{RuntimeMode.OPENSHIFT, JKubeBuildStrategy.s2i, OpenshiftBuildService.class},
        new Object[]{RuntimeMode.OPENSHIFT, JKubeBuildStrategy.jib, JibBuildService.class}
    );
  }

  @Parameterized.Parameter
  public RuntimeMode runtimeMode;

  @Parameterized.Parameter(1)
  public JKubeBuildStrategy buildStrategy;

  @Parameterized.Parameter(2)
  public Class<? extends BuildService> buildServiceClass;

  @Test
  public void getBuildService() {
    // Given
    final BuildServiceConfig config = BuildServiceConfig.builder().jKubeBuildStrategy(buildStrategy).build();
    final JKubeServiceHub jKubeServiceHub = new JKubeServiceHub(
        null, runtimeMode, new KitLogger.StdoutLogger(), mock(ServiceHub.class, RETURNS_DEEP_STUBS), new JKubeConfiguration(), config, new LazyBuilder<>(() -> null), true);
    // When
    final BuildService result = jKubeServiceHub.getBuildService();
    // Then
    assertThat(result).isNotNull().isInstanceOf(buildServiceClass);
  }
}
