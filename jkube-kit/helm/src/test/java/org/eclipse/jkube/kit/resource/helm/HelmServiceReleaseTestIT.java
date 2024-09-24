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
package org.eclipse.jkube.kit.resource.helm;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.api.model.Template;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.common.util.AsyncUtil;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.eclipse.jkube.kit.common.util.KubernetesMockServerUtil.prepareMockWebServerExpectationsForAggregatedDiscoveryEndpoints;
import static org.eclipse.jkube.kit.common.util.KubernetesMockServerUtil.prepareMockWebServerExpectationsForOpenApiV3Endpoints;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("HelmService.test")
@EnableKubernetesMockClient(crud = true)
class HelmServiceReleaseTestIT {
  @TempDir
  private Path tempDir;
  private HelmConfig helmConfig;
  private HelmService helmService;
  private KitLogger kitLogger;
  private KubernetesClient kubernetesClient;
  private KubernetesMockServer server;

  @BeforeEach
  void setUp() throws URISyntaxException, IOException {
    kitLogger = spy(new KitLogger.SilentLogger());
    Template helmParameterTemplates = Serialization.unmarshal(HelmServiceInstallIT.class.getResource("/it/sources/global-template.yml"), Template.class);
    Path outputDir = tempDir.resolve("output");
    helmConfig = HelmConfig.builder()
      .chart("helm-test")
      .version("0.1.0")
      .chartExtension("tgz")
      .types(Collections.singletonList(HelmConfig.HelmType.KUBERNETES))
      .tarballOutputDir(outputDir.toFile().getAbsolutePath())
      .outputDir(outputDir.toString())
      .parameterTemplates(Collections.singletonList(helmParameterTemplates))
      .sourceDir(new File(Objects.requireNonNull(HelmServiceInstallIT.class.getResource("/it/sources")).toURI()).getAbsolutePath())
      .releaseName("test-project")
      .disableOpenAPIValidation(true)
      .parameters(Arrays.asList(
        HelmParameter.builder().name("annotation_from_config").value("{{ .Chart.Name | upper }}").build(),
        HelmParameter.builder().name("annotation.from.config.dotted").value("{{ .Chart.Name }}").build(),
        HelmParameter.builder().name("deployment.replicas").value(1).build()))
      .build();
    // Remove after https://github.com/fabric8io/kubernetes-client/issues/6062 is fixed
    prepareMockWebServerExpectationsForAggregatedDiscoveryEndpoints(server);
    prepareMockWebServerExpectationsForOpenApiV3Endpoints(server);
    helmService = new HelmService(JKubeConfiguration.builder()
      .project(JavaProject.builder()
        .buildDirectory(tempDir.resolve("target").toFile())
        .build())
      .clusterConfiguration(ClusterConfiguration.from(kubernetesClient.getConfiguration()).build())
      .build(), ResourceServiceConfig.builder()
      .resourceDirs(Collections.singletonList(
          new File(Objects.requireNonNull(getClass().getResource("/it/source-helm-test/fragmentDir")).getFile())))
        .build(), kitLogger);
  }

  @Test
  @DisplayName("when valid chart but test timeout too low, then throw exception")
  void tooLowTimeout_thenThrowException() throws IOException {
    // Given
    givenHelmChartGeneratedAndInstalled();
    helmConfig.setTimeout(1);

    // When
    // Then
    assertThatIllegalStateException()
      .isThrownBy(() -> helmService.test(helmConfig))
      .withMessageContaining("timed out waiting for the condition");
  }

  @Test
  @DisplayName("when valid chart release provided, then log test details after test completion")
  void validChartRelease_thenLogChartTestDetails() throws IOException, ExecutionException, InterruptedException, TimeoutException {
    // Given
    givenHelmChartGeneratedAndInstalled();

    // When
    CompletableFuture<Boolean> helmTestCompletableFuture = AsyncUtil.async(() -> {
      helmService.test(helmConfig);
      return true;
    });

    // Then
    Pod testHelmPod = AsyncUtil.await(() -> kubernetesClient.pods().withName("test-project-test-connection").get())
      .apply(Objects::nonNull)
      .get(500, TimeUnit.MILLISECONDS);
    testHelmPod.setStatus(new PodStatusBuilder().withPhase("Succeeded").build());
    kubernetesClient.pods().resource(testHelmPod).updateStatus();
    helmTestCompletableFuture.get(5, TimeUnit.SECONDS);
    verify(kitLogger, times(1)).info("Testing Helm Chart %s %s", "helm-test", "0.1.0");
    verify(kitLogger, times(2)).info("[[W]]NAME : %s", "test-project");
    verify(kitLogger, times(2)).info("[[W]]NAMESPACE : %s", "");
    verify(kitLogger, times(2)).info("[[W]]STATUS : %s", "deployed");
    verify(kitLogger, times(2)).info("[[W]]REVISION : %s", "1");
    verify(kitLogger, times(1)).info("[[W]]Phase : Succeeded");
  }

  @Test
  @DisplayName("when unknown chart release provided, then throw exception")
  void invalidChartRelease_thenLogChartTestDetails() {
    // Given
    helmConfig.setReleaseName("i-was-never-created");
    // When
    assertThatIllegalStateException()
      .isThrownBy(() -> helmService.test(helmConfig))
      .withMessageContaining("not found");
  }

  private void givenHelmChartGeneratedAndInstalled() throws IOException {
    helmService.generateHelmCharts(helmConfig);
    helmService.install(helmConfig);
    Secret secret = kubernetesClient.secrets().withName("sh.helm.release.v1.test-project.v1").get();
    server.expect().get().withPath("/api/v1/namespaces/test/secrets?labelSelector=name%3Dtest-project%2Cowner%3Dhelm")
      .andReturn(200, new SecretListBuilder()
        .addToItems(secret)
        .build())
      .once();
  }
}
