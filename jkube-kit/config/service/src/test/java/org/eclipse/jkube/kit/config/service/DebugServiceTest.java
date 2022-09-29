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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpecBuilder;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigSpecBuilder;
import mockit.Expectations;
import mockit.Verifications;
import org.eclipse.jkube.kit.common.KitLogger;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetSpecBuilder;
import mockit.Mocked;
import org.assertj.core.groups.Tuple;
import org.eclipse.jkube.kit.config.service.portforward.PortForwardPodWatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SuppressWarnings("unused")
class DebugServiceTest {

  @Mocked
  private KitLogger logger;

  @Mocked
  private NamespacedKubernetesClient kubernetesClient;

  @Mocked
  private PortForwardService portForwardService;

  @Mocked
  private ApplyService applyService;

  @Mocked
  private PortForwardPodWatcher portForwardPodWatcher;

  private DebugService debugService;

  @BeforeEach
  void setUp() {
    debugService = new DebugService(logger, kubernetesClient, portForwardService, applyService);
  }

  @Test
  void initDebugEnvVarsMap() {
    // When
    final Map<String, String> result = debugService.initDebugEnvVarsMap(false);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("JAVA_DEBUG_SUSPEND", "false")
        .hasFieldOrPropertyWithValue("JAVA_ENABLE_DEBUG", "true");
  }

  @Test
  void enableDebuggingWithNullEntity() {
    // When - Then
    assertThatCode(() -> debugService.enableDebugging(null, "file.name", false))
        .doesNotThrowAnyException();
  }

  @Test
  void enableDebuggingWithNotApplicableEntity() {
    // Given
    final ConfigMap configMap = new ConfigMap();
    // When
    debugService.enableDebugging(configMap, "file.name", false);
    // Then
    assertThat(configMap).isEqualTo(new ConfigMap());
  }

  @Test
  void enableDebuggingWithDeployment() {
    // Given
    final Deployment deployment = initDeployment();
    // When
    debugService.enableDebugging(deployment, "file.name", false);
    // Then
    assertThat(deployment)
        .extracting("spec.template.spec.containers").asList()
        .flatExtracting("env")
        .extracting("name", "value")
        .containsExactlyInAnyOrder(
            new Tuple("JAVA_DEBUG_SUSPEND", "false"),
            new Tuple("JAVA_ENABLE_DEBUG", "true"));
  }

  @Test
  void enableDebuggingWithReplicaSet() {
    // Given
    final ReplicaSet replicaSet = initReplicaSet();
    // When
    debugService.enableDebugging(replicaSet, "file.name", false);
    // Then
    assertThat(replicaSet)
        .extracting("spec.template.spec.containers").asList()
        .flatExtracting("env")
        .extracting("name", "value")
        .containsExactlyInAnyOrder(
            new Tuple("JAVA_DEBUG_SUSPEND", "false"),
            new Tuple("JAVA_ENABLE_DEBUG", "true"));
  }

  @Test
  void enableDebuggingWithReplicationController() {
    // Given
    final ReplicationController replicationController = initReplicationController();
    mockFromServer(replicationController);
    // @formatter:on
    // When
    debugService.enableDebugging(replicationController, "file.name", false);
    // Then
    assertThat(replicationController)
        .extracting("spec.template.spec.containers").asList()
        .flatExtracting("env")
        .extracting("name", "value")
        .containsExactlyInAnyOrder(
            new Tuple("JAVA_DEBUG_SUSPEND", "false"),
            new Tuple("JAVA_ENABLE_DEBUG", "true"));
  }

  @Test
  void enableDebuggingWithDeploymentConfig() {
    // Given
    final DeploymentConfig deploymentConfig = initDeploymentConfig();
    mockFromServer(deploymentConfig);
    // When
    debugService.enableDebugging(deploymentConfig, "file.name", false);
    // Then
    assertThat(deploymentConfig)
        .extracting("spec.template.spec.containers").asList()
        .flatExtracting("env")
        .extracting("name", "value")
        .containsExactlyInAnyOrder(
            new Tuple("JAVA_DEBUG_SUSPEND", "false"),
            new Tuple("JAVA_ENABLE_DEBUG", "true"));
  }

  @Test
  void debugWithNotApplicableEntitiesShouldReturn() {
    // When
    debugService.debug("namespace", "file.name", Collections.emptySet(), null, false, logger);
    // Then
    // @formatter:off
    new Verifications() {{
      logger.error("Unable to proceed with Debug. No application resource found running in the cluster");
      times = 1;
    }};
    // @formatter:on
  }

  @Test
  void debugWithApplicableEntities(@Mocked CountDownLatch cdl) {
    // Given
    final Deployment deployment = mockDebugDeployment(cdl);
    // When
    debugService.debug(
        "namespace", "file.name", new HashSet<>(Collections.singletonList(deployment)),
        "1337", false, logger);
    // Then
    verifyDebugCompletedSuccessfully(deployment, 1337, false);
  }

  @Test
  void debugWithApplicableEntitiesAndSuspend(@Mocked CountDownLatch cdl) {
    // Given
    final Deployment deployment = mockDebugDeployment(cdl);
    // When
    debugService.debug(
        "namespace", "file.name", new HashSet<>(Collections.singletonList(deployment)),
        "31337", true, logger);
    // Then
    verifyDebugCompletedSuccessfully(deployment, 31337, true);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private Deployment mockDebugDeployment(CountDownLatch cdl) {
    final Deployment deployment = initDeployment();
    // @formatter:off
    new Expectations() {{
      applyService.isAlreadyApplied(deployment); result = true;
      portForwardPodWatcher.getPodReadyLatch(); result = cdl;
      cdl.getCount(); result = 1L;
    }};
    // @formatter:on
    return deployment;
  }

  private void verifyDebugCompletedSuccessfully(Deployment deployment, int localDebugPort, boolean debugSuspend) {
    // @formatter:off
    new Verifications() {{
      logger.info("No Active debug pod with provided selector and environment variables found! Waiting for pod to be ready...");
      times = 1;
      portForwardService.startPortForward(kubernetesClient, anyString, 5005, localDebugPort);
      times = 1;
    }};
    // @formatter:on
    assertThat(deployment)
        .extracting("spec.template.spec.containers").asList()
        .flatExtracting("env")
        .extracting("name", "value")
        .contains(
            new Tuple("JAVA_DEBUG_SUSPEND", String.valueOf(debugSuspend)),
            new Tuple("JAVA_ENABLE_DEBUG", "true"));
  }

  private void mockFromServer(HasMetadata entity) {
    // @formatter:off
    new Expectations() {{
      kubernetesClient.resource(entity).fromServer().get(); result = entity;
    }};
    // @formatter:on
  }

  private static Map<String, String> initLabels() {
    final Map<String, String> labels = new HashMap<>();
    labels.put("app", "test-app");
    labels.put("group", "test");
    labels.put("provider", "jkube");
    return labels;
  }

  private static LabelSelector initLabelSelector() {
    return new LabelSelectorBuilder().withMatchLabels(initLabels()).build();
  }

  private static PodTemplateSpec initPodTemplateSpec() {
    return new PodTemplateSpecBuilder()
        .withSpec(new PodSpecBuilder()
            .addNewContainer()
            .withImage("foo/test-app:0.0.1")
            .withName("test-app")
            .addNewPort().withContainerPort(8080).withName("http").withProtocol("TCP").endPort()
            .endContainer()
            .build())
        .build();
  }

  private static Deployment initDeployment() {
    return new DeploymentBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withName("test-app")
            .addToLabels(initLabels())
            .build())
        .withSpec(new DeploymentSpecBuilder()
            .withReplicas(1)
            .withRevisionHistoryLimit(2)
            .withSelector(initLabelSelector())
            .withTemplate(initPodTemplateSpec())
            .build())
        .build();
  }

  private static ReplicaSet initReplicaSet() {
    return new ReplicaSetBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withName("test-app")
            .addToLabels(initLabels())
            .build())
        .withSpec(new ReplicaSetSpecBuilder()
            .withReplicas(1)
            .withSelector(initLabelSelector())
            .withTemplate(initPodTemplateSpec())
            .build())
        .build();
  }

  private static ReplicationController initReplicationController() {
    return new ReplicationControllerBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withName("test-app")
            .addToLabels(initLabels())
            .build())
        .withSpec(new ReplicationControllerSpecBuilder()
            .withReplicas(1)
            .withTemplate(initPodTemplateSpec())
            .build())
        .build();
  }

  private static DeploymentConfig initDeploymentConfig() {
    return new DeploymentConfigBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withName("test-app")
            .addToLabels(initLabels())
            .build())
        .withSpec(new DeploymentConfigSpecBuilder()
            .withReplicas(1)
            .withSelector(initLabels())
            .withTemplate(initPodTemplateSpec())
            .build())
        .build();
  }
}
