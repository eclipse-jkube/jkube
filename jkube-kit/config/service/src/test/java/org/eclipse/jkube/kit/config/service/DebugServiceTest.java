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

import io.fabric8.kubernetes.api.model.APIGroupListBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetSpecBuilder;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigSpecBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.groups.Tuple;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unused")
@EnableKubernetesMockClient(crud = true)
class DebugServiceTest {
  private KitLogger logger;
  private NamespacedKubernetesClient kubernetesClient;
  private KubernetesMockServer mockServer;
  private ExecutorService singleThreadExecutor;
  private DebugService debugService;

  @BeforeEach
  void setUp() {
    logger = spy(new KitLogger.SilentLogger());
    final JKubeServiceHub serviceHub = JKubeServiceHub.builder()
      .log(logger)
      .configuration(JKubeConfiguration.builder().build())
      .platformMode(RuntimeMode.KUBERNETES)
      .clusterAccess(new ClusterAccess(ClusterConfiguration.from(kubernetesClient.getConfiguration()).build()))
      .build();
    singleThreadExecutor = Executors.newSingleThreadExecutor();
    final ApplyService applyService = new ApplyService(serviceHub);
    applyService.setNamespace(kubernetesClient.getNamespace());
    debugService = new DebugService(logger, kubernetesClient, new PortForwardService(logger), applyService);
  }

  @AfterEach
  void tearDown() {
    singleThreadExecutor.shutdownNow();
  }

  @Test
  @DisplayName("initDebugEnvVars, returns a Map containing JKube's specific debug environment variables")
  void initDebugEnvVarsMapAddsJKubeDebugVariables() {
    // When
    final Map<String, String> result = debugService.initDebugEnvVarsMap(false);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("JAVA_DEBUG_SUSPEND", "false")
        .hasFieldOrPropertyWithValue("JAVA_ENABLE_DEBUG", "true");
  }

  @Test
  @DisplayName("enableDebugging, with null entity, does nothing")
  void enableDebuggingWithNullEntity() {
    // When - Then
    assertThatCode(() -> debugService.enableDebugging(null, "file.name", false))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("enableDebugging, with not applicable entity, does nothing")
  void enableDebuggingWithNotApplicableEntity() {
    // Given
    final ConfigMap configMap = new ConfigMap();
    // When
    debugService.enableDebugging(configMap, "file.name", false);
    // Then
    assertThat(configMap).isEqualTo(new ConfigMap());
  }

  @Test
  @DisplayName("enableDebugging, with Deployment entity, adds debug environment variables to PodSpec")
  void enableDebuggingWithDeployment() {
    // Given
    final Deployment deployment = initDeployment();
    // When
    debugService.enableDebugging(deployment, "file.name", false);
    // Then
    assertThat(kubernetesClient.apps().deployments().withName("test-app").get())
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
    assertThat(kubernetesClient.apps().replicaSets().withName("test-app").get())
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
    kubernetesClient.resource(replicationController).createOrReplace();
    // When
    debugService.enableDebugging(replicationController, "file.name", false);
    // Then
    assertThat(kubernetesClient.replicationControllers().withName("test-app").get())
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
    mockServer.expect()
      .get()
      .withPath("/apis")
      .andReturn(200, new APIGroupListBuilder()
        .addNewGroup().withName("build.openshift.io").withApiVersion("v1").endGroup()
        .build())
      .always();
    final DeploymentConfig deploymentConfig = initDeploymentConfig();
    kubernetesClient.resource(deploymentConfig).create();
    // When
    debugService.enableDebugging(deploymentConfig, "file.name", false);
    // Then
    assertThat(kubernetesClient.adapt(OpenShiftClient.class).deploymentConfigs().withName("test-app").get())
        .extracting("spec.template.spec.containers").asList()
        .flatExtracting("env")
        .extracting("name", "value")
        .containsExactlyInAnyOrder(
            new Tuple("JAVA_DEBUG_SUSPEND", "false"),
            new Tuple("JAVA_ENABLE_DEBUG", "true"));
  }

  @Test
  void debugWithNotApplicableEntitiesLogsError() {
    // When
    debugService.debug("namespace", "file.name", Collections.emptySet(), null, false, logger);
    // Then
    verify(logger, times(1))
      .error("Unable to proceed with Debug. No application resource found running in the cluster");
  }

  @Test
  void debugWithApplicableEntities() throws Exception {
    // Given
    final Deployment deployment = withDeploymentRollout(initDeployment());
    final CompletableFuture<RecordedRequest> portForwardRequest = new CompletableFuture<>();
    mockServer.expect().get().withPath("/api/v1/namespaces/test/pods/pod-in-debug-mode/portforward?ports=5005")
      .andReply(200, r -> {
        portForwardRequest.complete(r);
        return "";
      }).always();
    // When
    singleThreadExecutor.submit(() -> debugService.debug( "test", "file.name",
      Collections.singletonList(deployment), "1337", false, logger));
    verify(logger, timeout(10000L))
      .info(startsWith("Now you can start a Remote debug session by using localhost"), anyInt());
    try (final Socket ignored = new Socket(InetAddress.getLocalHost(), 1337)) {
      // The socket connection triggers the KubernetesClient request to the k8s portforward endpoint
    }
    // Then
    assertThat(portForwardRequest).succeedsWithin(10, TimeUnit.SECONDS);
  }

  @Test
  void debugWithApplicableEntitiesAndSuspend() throws Exception {
    // Given
    final Deployment deployment = withDeploymentRollout(initDeployment());
    final CompletableFuture<RecordedRequest> portForwardRequest = new CompletableFuture<>();
    mockServer.expect().get().withPath("/api/v1/namespaces/test/pods/pod-in-debug-mode/portforward?ports=5005")
      .andReply(200, r -> {
        portForwardRequest.complete(r);
        return "";
      }).always();
    // When
    singleThreadExecutor.submit(() -> debugService.debug( "test", "file.name",
      Collections.singletonList(deployment), "1337", true, logger));
    verify(logger, timeout(10000L))
      .info(startsWith("Now you can start a Remote debug session by using localhost"), anyInt());
    try (final Socket ignored = new Socket(InetAddress.getLocalHost(), 1337)) {
      // The socket connection triggers the KubernetesClient request to the k8s portforward endpoint
    }
    // Then
    assertThat(portForwardRequest).succeedsWithin(10, TimeUnit.SECONDS);
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
      .withMetadata(new ObjectMetaBuilder()
        .withLabels(initLabels())
        .build())
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

  private Deployment withDeploymentRollout(Deployment deployment) {
    mockServer.expect().put()
      .withPath("/apis/apps/v1/namespaces/test/deployments/" + deployment.getMetadata().getName())
      .andReply(200, r -> {
        final Deployment d = Serialization.unmarshal(r.getBody().inputStream(), Deployment.class);
        kubernetesClient.resource(new PodBuilder()
          .withMetadata(new ObjectMetaBuilder(d.getSpec().getTemplate().getMetadata())
            .withName("pod-in-debug-mode")
            .build())
          .withSpec(d.getSpec().getTemplate().getSpec())
          .withStatus(new PodStatusBuilder()
            .withPhase("Running")
            .build())
          .build()).create();
        return d;
      })
      .always();
    return kubernetesClient.resource(deployment).create();
  }
}
