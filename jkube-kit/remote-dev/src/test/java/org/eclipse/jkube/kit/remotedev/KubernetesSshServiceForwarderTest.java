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

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodConditionBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.common.util.AsyncUtil.await;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@EnableKubernetesMockClient(crud = true)
class KubernetesSshServiceForwarderTest {

  @SuppressWarnings("unused")
  private KubernetesMockServer mockServer;
  @SuppressWarnings("unused")
  private KubernetesClient kubernetesClient;
  private RemoteDevelopmentContext context;
  private KubernetesSshServiceForwarder kubernetesSshServiceForwarder;
  private ExecutorService executorService;

  @BeforeEach
  void setUp() {
    mockServer.reset();
    context = new RemoteDevelopmentContext(
      spy(new KitLogger.StdoutLogger()),
      kubernetesClient,
      RemoteDevelopmentConfig.builder()
        .localService(LocalService.builder().serviceName("service").port(1337).build())
        .build()
    );
    kubernetesSshServiceForwarder = new KubernetesSshServiceForwarder(context);
    executorService = Executors.newSingleThreadExecutor();
  }

  @AfterEach
  void tearDown() {
    kubernetesSshServiceForwarder.stop();
    executorService.shutdownNow();
  }

  @Test
  @DisplayName("deploys Pod with image: quay.io/jkube/jkube-remote-dev:0.0.18")
  void deployPodWithImage() {
    // When
    executorService.submit(kubernetesSshServiceForwarder);
    // Then
    final Pod result = kubernetesClient.pods()
      .withLabel("app.kubernetes.io/name", "jkube-remote-dev")
      .withLabel("app.kubernetes.io/part-of", "jkube-kit")
      .withLabel("app.kubernetes.io/instance", context.getSessionID().toString())
      .waitUntilCondition(Objects::nonNull, 10, TimeUnit.SECONDS);

    assertThat(result)
      .extracting(Pod::getSpec)
      .extracting(PodSpec::getContainers)
      .asList()
      .singleElement()
      .hasFieldOrPropertyWithValue("image", "quay.io/jkube/jkube-remote-dev:0.0.18");
  }
  @Test
  @DisplayName("deploys Pod with port definitions")
  void deployPodWithPortDefinitions() {
    // When
    executorService.submit(kubernetesSshServiceForwarder);
    // Then
    final Pod result = kubernetesClient.pods()
      .withLabel("app.kubernetes.io/name", "jkube-remote-dev")
      .withLabel("app.kubernetes.io/part-of", "jkube-kit")
      .withLabel("app.kubernetes.io/instance", context.getSessionID().toString())
      .waitUntilCondition(Objects::nonNull, 10, TimeUnit.SECONDS);

    assertThat(result)
      .extracting(Pod::getSpec)
      .extracting(PodSpec::getContainers)
      .asList()
      .singleElement()
      .extracting("ports")
      .asList()
      .extracting("containerPort")
      .containsExactly(2222, 1337);
  }

  @Test
  @DisplayName("waits for Pod to be ready and retrieves the user")
  void waitsForDeployedPodToBeReadyAndLogUser() throws Exception {
    // Given
    executorService.submit(kubernetesSshServiceForwarder);
    final String podName = getRemoteDevPodName();
    setRemoteDevPodReady(podName);
    printUserInPodLog(podName, "the-random-user");
    final CompletableFuture<String> userFuture = await(context::getUser).apply(Objects::nonNull);
    // When
    final String result = userFuture.get(10, TimeUnit.SECONDS);
    // Then
    assertThat(result).isEqualTo("the-random-user");
  }

  // TODO: should use Nested tests instead, but KubernetesMockServer extension doesn't support it (yet)
  @DisplayName("logs waiting for pod to be ready")
  @Test
  void logsWaitingForPod() {
    // Given
    executorService.submit(kubernetesSshServiceForwarder);
    // Then
    verify(context.getLogger(), timeout(10000).times(1))
      .info("Waiting for JKube remote development Pod [%s] to be ready...", getRemoteDevPodName());
  }

  @DisplayName("logs Pod is ready")
  @Test
  void logsPodReady() {
    // Given
    executorService.submit(kubernetesSshServiceForwarder);
    final String podName = getRemoteDevPodName();
    setRemoteDevPodReady(podName);
    // Then
    verify(context.getLogger(), timeout(10000).times(1))
      .info("JKube remote development Pod [%s] is ready", podName);
  }

  @DisplayName("logs opening remote-dev connection")
  @Test
  void logsOpeningConnection() {
    // Given
    executorService.submit(kubernetesSshServiceForwarder);
    final String podName = getRemoteDevPodName();
    setRemoteDevPodReady(podName);
    printUserInPodLog(podName, "the-random-user");
    // Then
    verify(context.getLogger(), timeout(10000).times(1))
      .info("Opening remote development connection to Kubernetes: %s:%s%n", podName, context.getSshPort());
  }

  @DisplayName("logs removing Pod when stopped")
  @Test
  void logsRemovingPod() {
    executorService.submit(kubernetesSshServiceForwarder);
    final String podName = getRemoteDevPodName();
    setRemoteDevPodReady(podName);
    printUserInPodLog(podName, "1000");
    // When
    kubernetesSshServiceForwarder.stop();
    // Then
    verify(context.getLogger(), timeout(10000).times(1))
      .info("Removing JKube remote development Pod [%s]...", podName);
  }

  private String getRemoteDevPodName() {
    return getRemoteDevPodName(kubernetesClient);
  }

  static String getRemoteDevPodName(KubernetesClient kubernetesClient) {
    return kubernetesClient.pods()
      .withLabel("app.kubernetes.io/name", "jkube-remote-dev")
      .withLabel("app.kubernetes.io/part-of", "jkube-kit")
      .waitUntilCondition(Objects::nonNull, 10, TimeUnit.SECONDS)
      .getMetadata().getName();
  }

  private void setRemoteDevPodReady(String podName) {
    setRemoteDevPodReady(kubernetesClient, podName);
  }

  static void setRemoteDevPodReady(KubernetesClient kubernetesClient, String podName) {
    kubernetesClient.pods().withName(podName).edit(p -> new PodBuilder(p)
      .editOrNewStatus().addToConditions(new PodConditionBuilder()
        .withType("Ready").withStatus("True").build()).endStatus().build());
  }

  private void printUserInPodLog(String podName, String user) {
    printUserInPodLog(mockServer, podName, user);
  }

  static void printUserInPodLog(KubernetesMockServer mockServer, String podName, String user) {
    mockServer.expect().get()
      .withPath("/api/v1/namespaces/test/pods/" + podName + "/log?pretty=false")
      .andReturn(200, "Current container user is: " + user + "\n")
      .always();
  }
}
