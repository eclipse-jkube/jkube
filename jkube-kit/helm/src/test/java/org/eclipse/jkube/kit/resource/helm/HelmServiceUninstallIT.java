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

@DisplayName("HelmService.uninstall")
@EnableKubernetesMockClient(crud = true)
class HelmServiceUninstallIT {
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
    Template helmParameterTemplates = Serialization.unmarshal(HelmServiceUninstallIT.class.getResource("/it/sources/global-template.yml"), Template.class);
    Path outputDir = tempDir.resolve("output");
    helmConfig = HelmConfig.builder()
      .chart("helm-test")
      .version("0.1.0")
      .chartExtension("tgz")
      .types(Collections.singletonList(HelmConfig.HelmType.KUBERNETES))
      .tarballOutputDir(outputDir.toFile().getAbsolutePath())
      .outputDir(outputDir.toString())
      .parameterTemplates(Collections.singletonList(helmParameterTemplates))
      .sourceDir(new File(Objects.requireNonNull(HelmServiceUninstallIT.class.getResource("/it/sources")).toURI()).getAbsolutePath())
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
  @DisplayName("uninstall invoked, then log uninstallation details after uninstall")
  void uninstall_thenLogUninstalledChartDetails() throws IOException {
    // Given
    helmService.generateHelmCharts(helmConfig);
    helmService.install(helmConfig);
    Secret secret = kubernetesClient.secrets().withName("sh.helm.release.v1.test-project.v1").get();
    server.expect().get().withPath("/api/v1/namespaces/test/secrets?labelSelector=name%3Dtest-project%2Cowner%3Dhelm")
      .andReturn(200, new SecretListBuilder()
        .addToItems(secret)
        .build())
      .once();
    // When
    helmService.uninstall(helmConfig);
    // Then
    verify(kitLogger, times(1)).info("[[W]]%s", "release \"test-project\" uninstalled");
  }

  @Test
  @DisplayName("when chart not present, then uninstall fails")
  void chartAbsent_thenLogChartUninstallFailure() {
    assertThatIllegalStateException()
      .isThrownBy(() -> helmService.uninstall(helmConfig))
      .withMessageContaining(" not found");
  }
}
