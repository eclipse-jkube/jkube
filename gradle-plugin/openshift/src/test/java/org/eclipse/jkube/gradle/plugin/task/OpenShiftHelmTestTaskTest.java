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
package org.eclipse.jkube.gradle.plugin.task;

import com.marcnuri.helm.Helm;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.common.util.AsyncUtil;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.jkube.kit.common.util.KubernetesMockServerUtil.prepareMockWebServerExpectationsForAggregatedDiscoveryEndpoints;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableKubernetesMockClient(crud = true)
class OpenShiftHelmTestTaskTest {
  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();
  private KubernetesClient kubernetesClient;
  private KubernetesMockServer server;
  private TestOpenShiftExtension extension;

  @BeforeEach
  void setUp() throws IOException {
    extension = new TestOpenShiftExtension();
    // Remove after https://github.com/fabric8io/kubernetes-client/issues/6062 is fixed
    prepareMockWebServerExpectationsForAggregatedDiscoveryEndpoints(server);
    Helm.create().withDir(taskEnvironment.getRoot().toPath()).withName("empty-project").call();
    Path helmChartOutputDir = taskEnvironment.getRoot().toPath().resolve("build").resolve("jkube").resolve("helm");
    Files.createDirectories(helmChartOutputDir.resolve("openshift"));
    FileUtils.copyDirectory(taskEnvironment.getRoot().toPath().resolve("empty-project").toFile(), helmChartOutputDir.resolve("openshift").toFile());
    System.setProperty("jkube.kubernetesTemplate", taskEnvironment.getRoot().getAbsolutePath());
    extension.helm = HelmConfig.builder()
      .disableOpenAPIValidation(true)
      .outputDir(helmChartOutputDir.toString()).build();
    extension.access = ClusterConfiguration.from(kubernetesClient.getConfiguration()).build();
    extension.isUseColor = false;
    when(taskEnvironment.project.getName()).thenReturn("empty-project");
    when(taskEnvironment.project.getVersion()).thenReturn("0.1.0");
    when(taskEnvironment.project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
  }

  @AfterEach
  void tearDown() {
    System.clearProperty("jkube.kubernetesTemplate");
  }

  @Test
  @DisplayName("when Helm Release Installed on OpenShift Cluster, then test Helm Release")
  void runTask_withHelmReleasePresentInKubernetesCluster_shouldSucceed() throws IOException {
    // Given
    // OpenAPI validation endpoints required by helm test
    server.expect().get().withPath("/openapi/v3?timeout=32s")
      .andReturn(200, IOUtils.toString(Objects.requireNonNull(OpenShiftHelmTestTaskTest.class.getResourceAsStream("/helm-test-task/kubernetes-openapi-v3-schema.json")), StandardCharsets.UTF_8))
      .always();
    server.expect().get().withPath("/openapi/v3/api/v1?timeout=32s")
      .andReturn(200, IOUtils.toString(Objects.requireNonNull(OpenShiftHelmTestTaskTest.class.getResourceAsStream("/helm-test-task/kubernetes-openapi-v3-api-v1-schema-pod.json")), StandardCharsets.UTF_8))
      .always();
    OpenShiftHelmTestTask openShiftHelmTestTask = new OpenShiftHelmTestTask(OpenShiftExtension.class);
    openShiftHelmTestTask.init();
    openShiftHelmTestTask.jKubeServiceHub.getHelmService().install(extension.helm);
    // Should be removed once https://github.com/fabric8io/kubernetes-client/issues/6220 gets fixed
    Secret secret = kubernetesClient.secrets().withName("sh.helm.release.v1.empty-project.v1").get();
    server.expect().get().withPath("/api/v1/namespaces/test/secrets?labelSelector=name%3Dempty-project%2Cowner%3Dhelm")
      .andReturn(200, new SecretListBuilder()
        .addToItems(secret)
        .build())
      .once();
    // When
    CompletableFuture<Boolean> openShiftHelmTest = AsyncUtil.async(() -> {
      openShiftHelmTestTask.runTask();
      return true;
    });

    kubernetesClient.pods().withName("empty-project-test-connection")
      .waitUntilCondition(Objects::nonNull, 5, TimeUnit.SECONDS);
    // When
    kubernetesClient.pods().withName("empty-project-test-connection").editStatus(p -> new PodBuilder(p)
      .editOrNewStatus()
      .withPhase("Succeeded")
      .endStatus()
      .build());
    // Then
    assertThat(openShiftHelmTest).succeedsWithin(5, TimeUnit.SECONDS);
    verify(taskEnvironment.logger, times(1)).lifecycle("oc: Testing Helm Chart empty-project 0.1.0");
    verify(taskEnvironment.logger, times(2)).lifecycle("oc: NAME: empty-project");
    verify(taskEnvironment.logger, times(2)).lifecycle("oc: STATUS: deployed");
    verify(taskEnvironment.logger, times(2)).lifecycle("oc: REVISION: 1");
    verify(taskEnvironment.logger, times(2)).lifecycle("oc: Phase: Succeeded");
  }

  @Test
  @DisplayName("Helm Release not installed on OpenShift cluster, then throw exception")
  void execute_whenReleaseNotPresent_thenThrowException() {
    // Given
    OpenShiftHelmTestTask openShiftHelmTestTask = new OpenShiftHelmTestTask(OpenShiftExtension.class);

    // When + Then
    assertThatIllegalStateException()
      .isThrownBy(openShiftHelmTestTask::runTask)
      .withMessageContaining(" not found");
  }
}
