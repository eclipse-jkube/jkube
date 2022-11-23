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

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SuppressWarnings("unused")
@EnableKubernetesMockClient(crud = true)
class PortForwardServicePortOrderTest {
  private NamespacedKubernetesClient kubernetesClient;
  private KubernetesMockServer mockServer;

  @SuppressWarnings("EmptyTryBlock")
  @DisplayName("portForward")
  @ParameterizedTest(name = "with containerPort {0} and localPort {1} should perform HTTP request to {0}")
  @MethodSource("ports")
  void portsSpecifiedInCorrectOrderPortForward(int containerPort, int localPort) throws Exception {
    // Given
    final CompletableFuture<RecordedRequest> request = new CompletableFuture<>();
    mockServer.expect().get().withPath("/api/v1/namespaces/test/pods/foo-pod/portforward?ports=" + containerPort)
      .andReply(200, r -> {
        request.complete(r);
        return "";
      }).always();
    PortForwardService.forwardPortAsync(kubernetesClient, "foo-pod", containerPort, localPort);
    // When
    try (final Socket ignored = new Socket(InetAddress.getLocalHost(), localPort)) {
      // The socket connection triggers the KubernetesClient request to the k8s portforward endpoint
    }
    // Then
    assertThat(request).succeedsWithin(10, TimeUnit.SECONDS);
  }

  static Stream<Arguments> ports() {
    return Stream.of(
      arguments(8080, 1337),
      arguments(1337, 8080)
    );
  }
}
