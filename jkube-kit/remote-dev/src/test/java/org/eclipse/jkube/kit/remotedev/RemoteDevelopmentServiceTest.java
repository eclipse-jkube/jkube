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
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.IoUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@EnableKubernetesMockClient(crud = true)
class RemoteDevelopmentServiceTest {
  @SuppressWarnings("unused")
  private KubernetesMockServer mockServer;
  @SuppressWarnings("unused")
  private KubernetesClient kubernetesClient;
  private KitLogger logger;
  private RemoteDevelopmentService remoteDevelopmentService;

  @BeforeEach
  void setUp() {
    mockServer.reset();
    logger = spy(new KitLogger.StdoutLogger());
  }

  @AfterEach
  void tearDown() {
    if (remoteDevelopmentService != null) {
      remoteDevelopmentService.stop();
    }
  }

  @Test
  @DisplayName("service can be stopped before it is started")
  void canBeStoppedBeforeStart() {
    // When
    new RemoteDevelopmentService(logger, kubernetesClient, RemoteDevelopmentConfig.builder().build()).stop();
    // Then
    verify(logger).info("Remote development service stopped");
  }

  @Test
  @DisplayName("service can be stopped multiple times before it is started")
  void canBeStoppedMultipleTimesBeforeStart() {
    // Given
    remoteDevelopmentService = new RemoteDevelopmentService(
      logger, kubernetesClient,  RemoteDevelopmentConfig.builder().build());
    remoteDevelopmentService.stop();
    // When
    remoteDevelopmentService.stop();
    // Then
    verify(logger, times(2)).info("Remote development service stopped");
  }

  @Test
  @DisplayName("start initiates PortForwarder and KubernetesSshServiceForwarder")
  void startInitiatesChildProcesses() {
    // Given
    remoteDevelopmentService = new RemoteDevelopmentService(
      logger, kubernetesClient, RemoteDevelopmentConfig.builder().build());
    // When
    remoteDevelopmentService.start();
    // Then
    verify(logger, timeout(1000L).times(1))
      .debug("Starting Kubernetes SSH service forwarder...");
    verify(logger, timeout(1000L).times(1))
      .debug("Starting port forwarder...");
  }

  @Test
  @DisplayName("start initiates if LocalPort for remote service are available")
  void startInitsIfLocalPortAvailable() {
    RemoteService remoteService = RemoteService.builder()
        .hostname("remote-host").localPort(IoUtil.getFreeRandomPort()).port(1234).build();
    remoteDevelopmentService = new RemoteDevelopmentService(
      logger, kubernetesClient, RemoteDevelopmentConfig.builder().remoteService(remoteService).build());

    // When
    remoteDevelopmentService.start();

    // Then
    verify(logger, timeout(1000L).times(1))
      .debug("Local port '%s' for remote service '%s:%s' is available",
        remoteService.getLocalPort(),remoteService.getHostname(),remoteService.getPort());
    verify(logger,  timeout(1000L).times(1))
      .debug("Creating or replacing Kubernetes services for exposed ports from local environment");
  }

  @Test
  @DisplayName("start fails if LocalPort for remote service is in use")
  void startFailsIfLocalPortInUse() {
    final int localPort = IoUtil.getFreeRandomPort();
    RemoteService remoteService = RemoteService.builder()
      .hostname("remote-host").localPort(localPort).port(1234).build();
    remoteDevelopmentService = new RemoteDevelopmentService(
      logger, kubernetesClient, RemoteDevelopmentConfig.builder().remoteService(remoteService).build());
    assertThatThrownBy(() -> {
      try (ServerSocket localPortInUse = new ServerSocket(localPort)) {
        // When
        remoteDevelopmentService.start();
      }
    })
      .hasMessageContaining("Local port '" + localPort + "' is already in use (remote-host)");
  }
}
