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
package org.eclipse.jkube.gradle.plugin.task;

import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.BuildRecreateMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TaskUtilTest {
  private TestKubernetesExtension extension;
  private KitLogger kitLogger;

  @Before
  public void setUp() {
    extension = new TestKubernetesExtension();
    kitLogger = mock(KitLogger.class, RETURNS_DEEP_STUBS);
  }

  @Test
  public void buildServiceConfigBuilder_shouldInitializeBuildServiceConfigWithDefaults() {
    // When
    BuildServiceConfig buildServiceConfig = TaskUtil.buildServiceConfigBuilder(extension).build();

    // Then
    assertThat(buildServiceConfig)
      .hasFieldOrPropertyWithValue("buildRecreateMode", BuildRecreateMode.none)
      .hasFieldOrPropertyWithValue("jKubeBuildStrategy", JKubeBuildStrategy.docker)
      .hasFieldOrPropertyWithValue("forcePull", false)
      .hasFieldOrPropertyWithValue("buildDirectory", null);
  }

  @Test
  public void buildServiceConfigBuilder_shouldInitializeBuildServiceConfigWithConfiguredValues() {
    // Given
    extension.buildRecreate = "true";
    extension.isForcePull = true;
    extension.buildStrategy = JKubeBuildStrategy.jib;
    when(extension.javaProject.getBuildDirectory().getAbsolutePath()).thenReturn("/tmp/foo");

    // When
    BuildServiceConfig buildServiceConfig = TaskUtil.buildServiceConfigBuilder(extension).build();

    // Then
    assertThat(buildServiceConfig)
      .hasFieldOrPropertyWithValue("buildRecreateMode", BuildRecreateMode.all)
      .hasFieldOrPropertyWithValue("jKubeBuildStrategy", JKubeBuildStrategy.jib)
      .hasFieldOrPropertyWithValue("forcePull", true)
      .hasFieldOrPropertyWithValue("buildDirectory", "/tmp/foo");
  }

  @Test
  public void initDockerAccess_withDockerAccessRequired_shouldReturnDockerAccess() {
    try (MockedConstruction<DockerAccessFactory> daf = mockConstruction(DockerAccessFactory.class)) {
      // When
      TaskUtil.initDockerAccess(extension, kitLogger);
      // Then
      assertThat(daf.constructed()).hasSize(1);
      verify(daf.constructed().iterator().next(), times(1)).createDockerAccess(any());
    }
  }

  @Test
  public void initDockerAccess_withNoDockerAccessRequired_shouldReturnNull() {
    try (MockedConstruction<DockerAccessFactory> daf = mockConstruction(DockerAccessFactory.class)) {
      // Given
      extension.buildStrategy = JKubeBuildStrategy.jib;
      // When
      TaskUtil.initDockerAccess(extension, kitLogger);
      // Then
      assertThat(daf.constructed()).isEmpty();
    }
  }
}
