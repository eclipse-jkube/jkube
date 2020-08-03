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

import io.fabric8.openshift.client.OpenShiftClient;
import mockit.Expectations;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.image.build.JKubeConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.kubernetes.DockerBuildService;
import org.eclipse.jkube.kit.config.service.kubernetes.JibBuildService;
import org.eclipse.jkube.kit.config.service.openshift.OpenshiftBuildService;
import mockit.Mocked;
import org.junit.Test;

import java.util.Properties;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class JKubeServiceHubTest {

  @Mocked
  private KitLogger logger;

  @Mocked
  private ClusterAccess clusterAccess;

  @Mocked
  private OpenShiftClient openShiftClient;

  @Mocked
  private ServiceHub dockerServiceHub;

  @Mocked
  private JKubeConfiguration configuration;

  @Mocked
  private BuildServiceConfig buildServiceConfig;

  @Test(expected = NullPointerException.class)
  public void testMissingClusterAccess() {
    JKubeServiceHub.builder()
        .log(logger)
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void testMissingKitLogger() {
    JKubeServiceHub.builder()
        .clusterAccess(clusterAccess)
        .build();
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void testBasicInit() {
    // Given
    // @formatter:off
    new Expectations() {{
      configuration.getProject().getProperties(); result = new Properties();
    }};
    // @formatter:on
    // When
    try (final JKubeServiceHub jKubeServiceHub = JKubeServiceHub.builder()
        .platformMode(RuntimeMode.KUBERNETES)
        .configuration(configuration)
        .log(logger)
        .build()
    ) {
      // Then
      assertThat(jKubeServiceHub, notNullValue());
      assertThat(jKubeServiceHub.getClient(), notNullValue());
      assertThat(jKubeServiceHub.getRuntimeMode(), is(RuntimeMode.KUBERNETES));
    }
  }

  @Test
  public void testObtainBuildService() {
    // Given
    JKubeServiceHub hub = JKubeServiceHub.builder()
        .configuration(configuration)
        .clusterAccess(clusterAccess)
        .log(logger)
        .platformMode(RuntimeMode.KUBERNETES)
        .dockerServiceHub(dockerServiceHub)
        .buildServiceConfig(buildServiceConfig)
        .build();
    // When
    BuildService buildService = hub.getBuildService();
    // Then
    assertNotNull(buildService);
    assertTrue(buildService instanceof DockerBuildService);
  }

  @Test
  public void testObtainOpenshiftBuildService() {
    // Given
    // @formatter:off
      new Expectations() {{
        buildServiceConfig.getJKubeBuildStrategy(); result = null;
      }};
      // @formatter:on
    JKubeServiceHub hub = JKubeServiceHub.builder()
        .configuration(configuration)
        .clusterAccess(clusterAccess)
        .log(logger)
        .platformMode(RuntimeMode.OPENSHIFT)
        .dockerServiceHub(dockerServiceHub)
        .buildServiceConfig(buildServiceConfig)
        .build();
    // When
    BuildService buildService = hub.getBuildService();
    // Then
    assertNotNull(buildService);
    assertTrue(buildService instanceof OpenshiftBuildService);
  }

  @Test
  public void testObtainArtifactResolverService() {
    JKubeServiceHub hub = JKubeServiceHub.builder()
        .configuration(configuration)
        .clusterAccess(clusterAccess)
        .log(logger)
        .platformMode(RuntimeMode.KUBERNETES)
        .dockerServiceHub(dockerServiceHub)
        .build();

    assertNotNull(hub.getArtifactResolverService());
  }

  @Test
  public void testObtainJibBuildService() {
    // Given
    // @formatter:off
    new Expectations() {{
      buildServiceConfig.getJKubeBuildStrategy(); result = JKubeBuildStrategy.jib;
    }};
    // @formatter:on
    JKubeServiceHub hub = JKubeServiceHub.builder()
        .configuration(configuration)
        .clusterAccess(clusterAccess)
        .log(logger)
        .platformMode(RuntimeMode.KUBERNETES)
        .dockerServiceHub(dockerServiceHub)
        .buildServiceConfig(buildServiceConfig)
        .build();
    // When
    BuildService buildService = hub.getBuildService();
    // Then
    assertNotNull(buildService);
    assertTrue(buildService instanceof JibBuildService);
  }
}
