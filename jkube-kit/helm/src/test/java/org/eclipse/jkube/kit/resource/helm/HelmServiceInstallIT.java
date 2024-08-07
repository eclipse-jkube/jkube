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

import com.marcnuri.helm.Helm;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.api.model.Template;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
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

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.eclipse.jkube.kit.common.util.KubernetesMockServerUtil.prepareMockWebServerExpectationsForAggregatedDiscoveryEndpoints;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("HelmService.install")
@EnableKubernetesMockClient(crud = true)
class HelmServiceInstallIT {
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
    helmService = new HelmService(JKubeConfiguration.builder()
      .project(JavaProject.builder()
        .buildDirectory(tempDir.resolve("target").toFile())
        .build())
      .clusterConfiguration(ClusterConfiguration.from(kubernetesClient.getConfiguration()).build())
      .build(), new ResourceServiceConfig(), kitLogger);
  }

  @Test
  @DisplayName("when valid chart provided, then log installation details after install")
  void validChart_thenLogInstalledChartDetails() throws IOException {
    // Given
    helmService.generateHelmCharts(helmConfig);
    // When
    helmService.install(helmConfig);
    // Then
    verify(kitLogger, times(1)).info("[[W]]NAME : %s", "test-project");
    verify(kitLogger, times(1)).info("[[W]]NAMESPACE : %s", "");
    verify(kitLogger, times(1)).info("[[W]]STATUS : %s", "deployed");
    verify(kitLogger, times(1)).info("[[W]]REVISION : %s", "1");
  }

  @Test
  @DisplayName("dependency update flag enabled, then download chart")
  void whenDependencyUpdateEnabled_thenDownloadDependency() throws IOException {
    // Given
    givenHelmDependencyPresent();
    helmConfig = helmConfig.toBuilder()
      .installDependencyUpdate(true)
      .build();
    helmService.generateHelmCharts(helmConfig);

    // When
    helmService.install(helmConfig);

    // Then
    verify(kitLogger, times(1)).info("[[W]]%s", "Saving 1 charts");
    verify(kitLogger, times(1)).info("[[W]]%s", "Deleting outdated charts");
  }

  @Test
  @DisplayName("dependency update flag disabled, then throw exception")
  void whenDependencyUpdateDisabled_thenDoNotDownloadDependency() throws IOException {
    // Given
    givenHelmDependencyPresent();
    helmConfig = helmConfig.toBuilder()
      .installDependencyUpdate(false)
      .build();
    helmService.generateHelmCharts(helmConfig);

    // When + Then
    assertThatIllegalStateException()
      .isThrownBy(() -> helmService.install(helmConfig))
      .withMessage("An error occurred while checking for chart dependencies. You may need to run `helm dependency build` to fetch missing dependencies: found in Chart.yaml, but missing in charts/ directory: the-dependency");
  }

  private void givenHelmDependencyPresent() {
    Helm.create().withName("the-dependency").withDir(tempDir).call();
    helmConfig = helmConfig.toBuilder()
      .dependencies(Collections.singletonList(HelmDependency.builder()
        .name("the-dependency")
        .version("0.1.0")
        .repository("file://../../the-dependency")
        .build()))
      .build();
  }

  @Test
  @DisplayName("when invalid chart provided, then throw exception")
  void whenInvalidChartLocation_thenThrowException() {
    assertThatIllegalStateException()
      .isThrownBy(() -> helmService.install(helmConfig))
      .withMessageContaining(" not found");
  }

  @Test
  @DisplayName("when helm chart release name invalid, then throw exception")
  void whenHelmChartNameInvalid_thenThrowException() throws IOException {
    // Given
    helmConfig = helmConfig.toBuilder().releaseName("INVALID").build();
    helmService.generateHelmCharts(helmConfig);
    // When + Then
    assertThatIllegalStateException()
      .isThrownBy(() -> helmService.install(helmConfig))
      .withMessageContaining("release name \"INVALID\": invalid release name");
  }

  @Test
  @DisplayName("when dependency referenced doesn't exist, then throw exception")
  void whenDependencyReferencedDoesNotExist_thenThrowException() throws IOException {
    // Given
    helmConfig = helmConfig.toBuilder()
      .installDependencyUpdate(true)
      .dependencies(Collections.singletonList(HelmDependency.builder()
        .name("the-dependency")
        .version("0.1.0")
        .repository("file://../../the-dependency")
        .build()))
      .build();
    helmService.generateHelmCharts(helmConfig);
    // When + Then
    assertThatIllegalStateException()
      .isThrownBy(() -> helmService.install(helmConfig))
      .withMessage(String.format("An error occurred while updating chart dependencies: directory %s%sthe-dependency not found", tempDir,File.separator));
  }
}
