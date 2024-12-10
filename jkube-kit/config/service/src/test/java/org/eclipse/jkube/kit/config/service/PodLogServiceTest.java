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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.AsyncUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@EnableKubernetesMockClient(crud = true)
class PodLogServiceTest {

  KubernetesMockServer kubernetesMockServer;
  KubernetesClient kubernetesClient;
  private ByteArrayOutputStream out;
  private ByteArrayOutputStream oldPodOut;
  private ByteArrayOutputStream newPodOut;

  private PodLogService.PodLogServiceContext podLogServiceContext;
  private Collection<HasMetadata> entities;
  private Pod runningPod;

  @BeforeEach
  void setUp() {
    out = new ByteArrayOutputStream();
    oldPodOut = new ByteArrayOutputStream();
    newPodOut = new ByteArrayOutputStream();
    podLogServiceContext = PodLogService.PodLogServiceContext.builder()
      .log(new KitLogger.PrintStreamLogger(new PrintStream(out)))
      .oldPodLog(new KitLogger.PrintStreamLogger(new PrintStream(oldPodOut)))
      .newPodLog(new KitLogger.PrintStreamLogger(new PrintStream(newPodOut)))
      .build();
    // Create a Deployment to use as the source of the Selector
    final Deployment deployment = kubernetesClient.resource(new DeploymentBuilder()
        .withMetadata(new ObjectMetaBuilder()
          .withName("the-deployment")
          .build())
        .withSpec(new DeploymentSpecBuilder()
          .editOrNewSelector().addToMatchLabels("app", "the-app").endSelector()
          .editOrNewTemplate()
          .withNewMetadata().withName("the-app-pod").endMetadata()
          .endTemplate()
          .build())
        .build())
      .createOr(NonDeletingOperation::update);
    entities = Collections.singletonList(deployment);
    runningPod = new PodBuilder()
      .withNewMetadata().withName("the-pod").addToLabels("app", "the-app").endMetadata()
      .withNewSpec().endSpec()
      .withNewStatus().withPhase("Running").endStatus()
      .build();
  }

  @Test
  @DisplayName("When no name specified, should log selector")
  void shouldLogSelector() {
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    assertThat(AsyncUtil.await(out::toString).apply(o -> o.contains("[INFO] Watching pods with selector LabelSelector(")))
      .succeedsWithin(1, TimeUnit.SECONDS).asString()
      .contains(") waiting for a running pod...");
  }

  @Test
  @DisplayName("When name specified, should log name")
  void shouldLogName() {
    new PodLogService(podLogServiceContext.toBuilder().podName("the-pod").build())
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    assertThat(AsyncUtil.await(out::toString).apply(o -> o.contains("[INFO] Watching pod with selector LabelSelector(")))
      .succeedsWithin(1, TimeUnit.SECONDS).asString()
      .contains("), and name the-pod waiting for a running pod...");
  }

  @Test
  @DisplayName("With no Pod found, should warn user")
  void noPodShouldWarnUser() {
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    assertThat(AsyncUtil.await(out::toString).apply(o -> o.contains("No pod is running yet")))
      .succeedsWithin(1, TimeUnit.SECONDS).asString()
      .containsSubsequence(
        "[WARN] No pod is running yet. Are you sure you deployed your app using Eclipse JKube apply/deploy mechanism?",
        "[WARN] Or did you undeploy it? If so try running the Eclipse JKube apply/deploy tasks again."
      );
  }

  @DisplayName("With Pod running and mismatched container, should error")
  @ParameterizedTest(name = "follow: {0}")
  @ValueSource(booleans = {true, false})
  void podRunningNoContainer(boolean follow) {
    // Given
    kubernetesClient.resource(new PodBuilder(runningPod)
        .editOrNewSpec()
        .addNewContainer().withName("container-one").endContainer()
        .addNewContainer().withName("container-two").endContainer()
        .endSpec()
        .build())
      .createOr(NonDeletingOperation::update);
    // When
    new PodLogService(podLogServiceContext.toBuilder().logContainerName("non-existent").build())
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, follow, null, false);
    // Then
    assertThat(AsyncUtil.await(out::toString).apply(o -> o.contains("[ERROR] log container name ")))
      .succeedsWithin(1, TimeUnit.SECONDS).asString()
      .containsSubsequence(
        "[ERROR] log container name non-existent does not exist in pod!! Did you set the correct value for property 'jkube.log.container'"
      );
  }

  @Test
  @DisplayName("With Pod running, should log control messages")
  void podRunningShouldLogControlMessages() {
    // Given
    kubernetesClient.resource(runningPod).createOr(NonDeletingOperation::update);
    // When
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    // Then
    assertThat(AsyncUtil.await(newPodOut::toString).apply(o -> o.contains("[INFO] Tailing log of pod: the-pod")))
      .succeedsWithin(1, TimeUnit.SECONDS).asString()
      .contains("[INFO] Press Ctrl-C to stop tailing the log");
  }

  @Test
  @DisplayName("With Pod running, should log status update")
  void podRunningShouldLogStatusUpdates() {
    // Given
    kubernetesClient.resource(runningPod).createOr(NonDeletingOperation::update);
    // When
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    // Then
    assertThat(AsyncUtil.await(newPodOut::toString).apply(o -> o.contains("[INFO] the-pod status: Running ")))
      .succeedsWithin(1, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("With Pod running and deletion, should log status update")
  void podRunningAndDeletionShouldLogStatusUpdates() {
    // Given
    kubernetesClient.resource(runningPod).createOr(NonDeletingOperation::update);
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    assertThat(AsyncUtil.await(newPodOut::toString).apply(o -> o.contains("[INFO] the-pod status: Running ")))
      .succeedsWithin(1, TimeUnit.SECONDS);
    // When
    kubernetesClient.resource(runningPod).delete();
    // Then
    assertThat(AsyncUtil.await(oldPodOut::toString).apply(o -> o.contains("[INFO] the-pod status: Running : Pod Deleted")))
      .succeedsWithin(1, TimeUnit.SECONDS);
  }


  @Test
  @DisplayName("With Pod running and deletion, should close previous watch")
  void podRunningAndDeletionShouldClosePreviousWatch() {
    // Given
    kubernetesClient.resource(runningPod).createOr(NonDeletingOperation::update);
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    assertThat(AsyncUtil.await(newPodOut::toString).apply(o -> o.contains("[INFO] the-pod status: Running ")))
      .succeedsWithin(1, TimeUnit.SECONDS);
    // When
    kubernetesClient.resource(runningPod).delete();
    // Then
    assertThat(AsyncUtil.await(out::toString).apply(o -> o.contains("[INFO] Closing log watcher for the-pod (Deleted)")))
      .succeedsWithin(1, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("With Pod running and addition, should log status update of new Pod")
  void podRunningAndAdditionShouldLogStatusUpdates() {
    // Given
    kubernetesClient.resource(runningPod).createOr(NonDeletingOperation::update);
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    assertThat(AsyncUtil.await(newPodOut::toString).apply(o -> o.contains("[INFO] Tailing log of pod: the-pod")))
      .succeedsWithin(1, TimeUnit.SECONDS);
    // When
    kubernetesClient.resource(new PodBuilder(runningPod).editMetadata()
        .withName("new-pod")
        .endMetadata().build())
      .createOr(NonDeletingOperation::update);
    // Then
    assertThat(AsyncUtil.await(newPodOut::toString).apply(o -> o.contains("[INFO] new-pod status: Running ")))
      .succeedsWithin(1, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("With Pod running and addition, should log status update of new Pod")
  void podPendingAndThenRunningShouldLogStatusUpdates() {
    // Given
    kubernetesClient.resource(new PodBuilder(runningPod).editStatus().withPhase("Pending").endStatus().build()).createOr(NonDeletingOperation::update);
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    assertThat(AsyncUtil.await(newPodOut::toString).apply(o -> o.contains("[INFO] the-pod status: Pending ")))
      .succeedsWithin(1, TimeUnit.SECONDS);
    // When
    kubernetesClient.pods().withName("the-pod")
      .edit(p -> new PodBuilder(p).editStatus().withPhase("Running").endStatus().build());
    // Then
    assertThat(AsyncUtil.await(newPodOut::toString).apply(o -> o.contains("[INFO] Tailing log of pod: the-pod")))
      .succeedsWithin(1, TimeUnit.SECONDS).asString()
      .containsSubsequence(
        "[INFO] the-pod status: Running ",
        // Finally (prevents Client from being closed before completing the log execution)
        "[INFO] Tailing log of pod: the-pod"
      );
  }

  @Test
  @DisplayName("With Pod running, should log container logs")
  void podRunningShouldLogContainerLogs() {
    // Given
    kubernetesClient.resource(runningPod).createOr(NonDeletingOperation::update);
    kubernetesMockServer.expect()
      .get()
      .withPath("/api/v1/namespaces/test/pods/the-pod/log?pretty=false&follow=true")
      .andReturn(200, "The\nApplication\nLogs")
      .always();
    // When
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    // Then
    assertThat(AsyncUtil.await(out::toString).apply(o -> o.contains("[INFO] [[s]]Logs")))
      .succeedsWithin(1, TimeUnit.SECONDS).asString()
        .containsSubsequence(
          "[INFO] [[s]]The",
          "[INFO] [[s]]Application",
          "[INFO] [[s]]Logs"
        );
  }

  @Test
  @DisplayName("With Pod running and no follow, should log container logs")
  void podRunningWithNoFollowShouldLogContainerLogs() {
    // Given
    kubernetesClient.resource(runningPod).createOr(NonDeletingOperation::update);
    kubernetesMockServer.expect()
      .get()
      .withPath("/api/v1/namespaces/test/pods/the-pod/log?pretty=false")
      .andReturn(200, "The\nApplication\nLogs")
      .always();
    // When
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, false, null, false);
    // Then
    assertThat(AsyncUtil.await(out::toString).apply(o -> o.contains("[INFO] [[s]]Logs")))
      .succeedsWithin(1, TimeUnit.SECONDS).asString()
      .containsSubsequence(
        "[INFO] [[s]]The",
        "[INFO] [[s]]Application",
        "[INFO] [[s]]Logs"
      );
  }

  @Test
  @DisplayName("With Pod running and no follow in current thread, should log container logs synchronously")
  void podRunningWithNoFollowInCurrentThreadShouldLogContainerLogs() {
    // Given
    kubernetesClient.resource(runningPod).createOr(NonDeletingOperation::update);
    kubernetesMockServer.expect()
      .get()
      .withPath("/api/v1/namespaces/test/pods/the-pod/log?pretty=false")
      .andReturn(200, "The\nApplication\nLogs")
      .always();
    // When
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, false, null, true);
    // Then
    assertThat(AsyncUtil.await(out::toString).apply(o -> o.contains("[INFO] [[s]]Logs")))
      .succeedsWithin(1, TimeUnit.SECONDS).asString()
      .containsSubsequence(
        "[INFO] [[s]]The",
        "[INFO] [[s]]Application",
        "[INFO] [[s]]Logs"
      );
  }

  @Test
  @DisplayName("With Pod running and deleted, should close previous watcher")
  void podRunningAndDeletedShouldClosePreviousWatcher() {
    // Given
    kubernetesClient.resource(runningPod).createOr(NonDeletingOperation::update);
    kubernetesMockServer.expect()
      .get()
      .withPath("/api/v1/namespaces/test/pods/the-pod/log?pretty=false&follow=true")
      .andReturn(200, "The\nApplication\nLogs")
      .always();
    kubernetesMockServer.expect()
      .get()
      .withPath("/api/v1/namespaces/test/pods/new-pod/log?pretty=false&follow=true")
      .andReturn(200, "The\nApplication\nLogs")
      .always();
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    assertThat(AsyncUtil.await(newPodOut::toString).apply(o -> o.contains("Tailing log of pod: the-pod")))
      .succeedsWithin(1000, TimeUnit.SECONDS);
    kubernetesClient.resource(runningPod).delete();
    // When
    kubernetesClient.resource(new PodBuilder(runningPod).editMetadata().withName("new-pod").endMetadata().build())
      .createOr(NonDeletingOperation::update);
    // Then
    assertThat(AsyncUtil.await(out::toString).apply(o -> o.contains("[INFO] Closing log watcher for the-pod (Deleted)")))
      .succeedsWithin(1, TimeUnit.SECONDS);
    // Finally (prevents Client from being closed before completing the log execution)
    assertThat(AsyncUtil.await(newPodOut::toString).apply(o -> o.contains("Tailing log of pod: new-pod")))
      .succeedsWithin(1000, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("With Pod running and addition, should close previous watch")
  void podRunningAndAdditionShouldClosePreviousWatch() throws Exception {
    // Given
    // n.b. The precision of the creationTimestamp is to seconds.
    // This leads to a problem in case two pods were created rapidly, since the PodLogEventHandler
    // will not be able to distinguish between the two pods and the result of mostRecentPod() will be non-deterministic.
    kubernetesClient.resource(runningPod).createOr(NonDeletingOperation::update);
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    assertThat(AsyncUtil.await(newPodOut::toString).apply(o -> o.contains("[INFO] Tailing log of pod: the-pod")))
      .succeedsWithin(10, TimeUnit.SECONDS);
    // TODO: adding one second delay
    Thread.sleep(1001);
    // When
    kubernetesClient.resource(new PodBuilder(runningPod).editMetadata()
        .withName("new-pod")
        .endMetadata().build())
      .createOr(NonDeletingOperation::update);
    // Then
    assertThat(AsyncUtil.await(newPodOut::toString).apply(o -> o.contains("Tailing log of pod: new-pod")))
      .withFailMessage(() -> "Expected Tailing log of pod: new-pod but got " + newPodOut.toString())
      .succeedsWithin(5, TimeUnit.SECONDS);
    assertThat(AsyncUtil.await(out::toString).apply(o -> o.contains("[INFO] Closing log watcher for the-pod as now watching new-pod")))
      .succeedsWithin(1, TimeUnit.SECONDS);
  }

}
