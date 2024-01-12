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
import io.fabric8.kubernetes.api.model.LabelSelector;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collection;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@EnableKubernetesMockClient(crud = true)
class PodLogServiceTest {

  KubernetesMockServer kubernetesMockServer;
  KubernetesClient kubernetesClient;
  private KitLogger log;
  private KitLogger oldPodLog;
  private KitLogger newPodLog;

  private PodLogService.PodLogServiceContext podLogServiceContext;
  private Collection<HasMetadata> entities;
  private Pod runningPod;

  @BeforeEach
  void setUp() {
    log = spy(new KitLogger.SilentLogger());
    oldPodLog = spy(new KitLogger.SilentLogger());
    newPodLog = spy(new KitLogger.SilentLogger());
    podLogServiceContext = PodLogService.PodLogServiceContext.builder()
      .log(log)
      .oldPodLog(oldPodLog)
      .newPodLog(newPodLog)
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
    verify(log, timeout(1000))
      .info(eq("Watching pods with selector %s waiting for a running pod..."), any(LabelSelector.class));
  }

  @Test
  @DisplayName("When name specified, should log name")
  void shouldLogName() {
    new PodLogService(podLogServiceContext.toBuilder().podName("the-pod").build())
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    verify(log, timeout(1000))
      .info(eq("Watching pod with selector %s, and name %s waiting for a running pod..."), any(LabelSelector.class), eq("the-pod"));
  }

  @Test
  @DisplayName("With no Pod found, should warn user")
  void noPodShouldWarnUser() {
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    verify(log, timeout(1000))
      .warn("No pod is running yet. Are you sure you deployed your app using Eclipse JKube apply/deploy mechanism?");
    verify(log, timeout(1000))
      .warn("Or did you undeploy it? If so try running the Eclipse JKube apply/deploy tasks again.");
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
    verify(log, timeout(1000))
      .error("log container name %s does not exist in pod!! Did you set the correct value for property 'jkube.log.container'", "non-existent");
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
    verify(newPodLog, timeout(1000)).info("Tailing log of pod: the-pod");
    verify(newPodLog, timeout(1000)).info("Press Ctrl-C to stop tailing the log");
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
    verify(newPodLog, timeout(1000)).info("%s status: %s%s", "the-pod", "Running ", "");
  }

  @Test
  @DisplayName("With Pod running and deletion, should log status update")
  void podRunningAndDeletionShouldLogStatusUpdates() {
    // Given
    kubernetesClient.resource(runningPod).createOr(NonDeletingOperation::update);
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    verify(newPodLog, timeout(1000)).info("%s status: %s%s", "the-pod", "Running ", "");
    // When
    kubernetesClient.resource(runningPod).delete();
    // Then
    verify(oldPodLog, timeout(1000)).info("%s status: %s%s", "the-pod", "Running ", ": Pod Deleted");
  }


  @Test
  @DisplayName("With Pod running and deletion, should close previous watch")
  void podRunningAndDeletionShouldClosePreviousWatch() {
    // Given
    kubernetesClient.resource(runningPod).createOr(NonDeletingOperation::update);
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    verify(newPodLog, timeout(1000)).info("%s status: %s%s", "the-pod", "Running ", "");
    // When
    kubernetesClient.resource(runningPod).delete();
    // Then
    verify(log, timeout(1000)).info("Closing log watcher for %s (Deleted)", "the-pod");
  }

  @Test
  @DisplayName("With Pod running and addition, should log status update of new Pod")
  void podRunningAndAdditionShouldLogStatusUpdates() {
    // Given
    kubernetesClient.resource(runningPod).createOr(NonDeletingOperation::update);
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    verify(newPodLog, timeout(1000)).info("Tailing log of pod: the-pod");
    // When
    kubernetesClient.resource(new PodBuilder(runningPod).editMetadata()
        .withName("new-pod")
        .endMetadata().build())
      .createOr(NonDeletingOperation::update);
    // Then
    verify(newPodLog, timeout(1000)).info("%s status: %s%s", "new-pod", "Running ", "");
  }

  @Test
  @DisplayName("With Pod running and addition, should log status update of new Pod")
  void podPendingAndThenRunningShouldLogStatusUpdates() {
    // Given
    kubernetesClient.resource(new PodBuilder(runningPod).editStatus().withPhase("Pending").endStatus().build()).createOr(NonDeletingOperation::update);
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    verify(newPodLog, timeout(1000)).info("%s status: %s%s", "the-pod", "Pending ", "");
    // When
    kubernetesClient.pods().withName("the-pod")
      .edit(p -> new PodBuilder(p).editStatus().withPhase("Running").endStatus().build());
    // Then
    verify(newPodLog, timeout(1000)).info("%s status: %s%s", "the-pod", "Running ", "");
    // Finally (prevents Client from being closed before completing the log execution)
    verify(newPodLog, timeout(1000)).info("Tailing log of pod: the-pod");
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
    verify(log, timeout(1000)).info("[[s]]%s", "The");
    verify(log, timeout(1000)).info("[[s]]%s", "Application");
    verify(log, timeout(1000)).info("[[s]]%s", "Logs");
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
    verify(log, timeout(1000)).info("[[s]]%s", "The");
    verify(log, timeout(1000)).info("[[s]]%s", "Application");
    verify(log, timeout(1000)).info("[[s]]%s", "Logs");
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
    verify(log).info("[[s]]%s", "The");
    verify(log).info("[[s]]%s", "Application");
    verify(log).info("[[s]]%s", "Logs");
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
    verify(newPodLog, timeout(1000)).info("Tailing log of pod: the-pod");
    kubernetesClient.resource(runningPod).delete();
    // When
    kubernetesClient.resource(new PodBuilder(runningPod).editMetadata().withName("new-pod").endMetadata().build())
      .createOr(NonDeletingOperation::update);
    // Then
    verify(log, timeout(1000)).info("Closing log watcher for %s (Deleted)", "the-pod");
    // Finally (prevents Client from being closed before completing the log execution)
    verify(newPodLog, timeout(1000)).info("Tailing log of pod: new-pod");
  }

  @Test
  @DisplayName("With Pod running and addition, should close previous watch")
  void podRunningAndAdditionShouldClosePreviousWatch() {
    // Given
    kubernetesClient.resource(runningPod).createOr(NonDeletingOperation::update);
    new PodLogService(podLogServiceContext)
      .tailAppPodsLogs(kubernetesClient, null, entities, false, null, true, null, false);
    verify(newPodLog, timeout(1000)).info("Tailing log of pod: the-pod");
    // When
    kubernetesClient.resource(new PodBuilder(runningPod).editMetadata()
        .withName("new-pod")
        .endMetadata().build())
      .createOr(NonDeletingOperation::update);
    // Then
    verify(log, timeout(1000)).info("Closing log watcher for %s as now watching %s", "the-pod", "new-pod");
  }

}
