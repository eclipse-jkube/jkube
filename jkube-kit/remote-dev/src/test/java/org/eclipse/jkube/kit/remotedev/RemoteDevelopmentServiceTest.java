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
package org.eclipse.jkube.kit.remotedev;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@EnableKubernetesMockClient(crud = true)
class RemoteDevelopmentServiceTest {
  private static JKubeServiceHub jKubeServiceHub;
  @SuppressWarnings("unused")
  private KubernetesMockServer mockServer;
  @SuppressWarnings("unused")
  private KubernetesClient kubernetesClient;

  @BeforeEach
  void setUp() {
    mockServer.reset();
    jKubeServiceHub = JKubeServiceHub.builder()
      .platformMode(RuntimeMode.KUBERNETES)
      .log(spy(new KitLogger.StdoutLogger()))
      .configuration(JKubeConfiguration.builder().build())
      .clusterAccess(new ClusterAccess(null, null) {
        @SuppressWarnings("unchecked")
        @Override
        public KubernetesClient createDefaultClient() {
          return kubernetesClient;
        }
      })
      .build();
  }

  @Test
  @DisplayName("service can be stopped before it is started")
  void canBeStoppedBeforeStart() {
    // When
    new RemoteDevelopmentService(jKubeServiceHub, RemoteDevelopmentConfig.builder().build()).stop();
    // Then
    verify(jKubeServiceHub.getLog()).info("Remote development service stopped");
  }

  @Test
  @DisplayName("service can be stopped multiple times before it is started")
  void canBeStoppedMultipleTimesBeforeStart() {
    // Given
    final RemoteDevelopmentService remoteDev = new RemoteDevelopmentService(
      jKubeServiceHub, RemoteDevelopmentConfig.builder().build());
    remoteDev.stop();
    // When
    remoteDev.stop();
    // Then
    verify(jKubeServiceHub.getLog(), times(2)).info("Remote development service stopped");
  }
}
