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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@EnableKubernetesMockClient(crud = true)
class RemoteDevelopmentServiceTest {
  @SuppressWarnings("unused")
  private KubernetesMockServer mockServer;
  @SuppressWarnings("unused")
  private KubernetesClient kubernetesClient;
  private KitLogger logger;

  @BeforeEach
  void setUp() {
    mockServer.reset();
    logger = spy(new KitLogger.StdoutLogger());
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
    final RemoteDevelopmentService remoteDev = new RemoteDevelopmentService(
      logger, kubernetesClient,  RemoteDevelopmentConfig.builder().build());
    remoteDev.stop();
    // When
    remoteDev.stop();
    // Then
    verify(logger, times(2)).info("Remote development service stopped");
  }

  @Test
  @DisplayName("start initiates PortForwarder and KubernetesSshServiceForwarder")
  void startInitiatesChildProcesses() {
    CompletableFuture<Void> future = null;
    try {
      // When
      future = new RemoteDevelopmentService(logger, kubernetesClient,  RemoteDevelopmentConfig.builder().build())
        .start();
      // Then
      verify(logger, times(1))
        .debug("Starting Kubernetes SSH service forwarder...");
      verify(logger, times(1))
        .debug("Starting port forwarder...");
    } finally {
      Optional.ofNullable(future).ifPresent(f -> f.cancel(true));
    }
  }
}
