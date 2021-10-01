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

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.BuildRecreateMode;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.gradle.api.internal.provider.DefaultProperty;
import org.gradle.api.provider.Property;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
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
    extension = new TestKubernetesExtension();
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
  public void addDockerServiceHubToJKubeServiceHubBuilder_shouldInitializeJKubeServiceHubWithDefaults() {
    try (MockedConstruction<DockerAccessFactory> dockerAccessFactory = mockConstruction(DockerAccessFactory.class)) {
      // Given
      JKubeConfiguration jKubeConfiguration = mock(JKubeConfiguration.class, RETURNS_DEEP_STUBS);
      JKubeServiceHub.JKubeServiceHubBuilder builder = JKubeServiceHub.builder()
        .configuration(jKubeConfiguration)
        .log(kitLogger)
        .platformMode(RuntimeMode.KUBERNETES);

      // When
      JKubeServiceHub jKubeServiceHub = TaskUtil.addDockerServiceHubToJKubeServiceHubBuilder(builder, extension, kitLogger)
          .build();

      // Then
      assertThat(jKubeServiceHub)
        .hasFieldOrProperty("buildServiceConfig")
        .hasFieldOrProperty("dockerServiceHub");
    }
  }
}
