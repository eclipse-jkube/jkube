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

import io.fabric8.kubeapitest.junit.EnableKubeAPIServer;
import io.fabric8.kubeapitest.junit.KubeConfig;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.api.model.Template;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.junit.jupiter.api.AfterEach;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("HelmService.uninstall")
@EnableKubeAPIServer
class HelmServiceUninstallIT {

  @KubeConfig
  static String kubeConfigYaml;
  @TempDir
  private Path tempDir;
  private KubernetesClient kubernetesClient;
  private HelmConfig helmConfig;
  private HelmService helmService;
  private KitLogger kitLogger;

  @BeforeEach
  void setUp() throws URISyntaxException, IOException {
    kubernetesClient = new KubernetesClientBuilder().withConfig(Config.fromKubeconfig(kubeConfigYaml)).build();
    kubernetesClient.apps().deployments().withTimeout(1, TimeUnit.SECONDS).delete();
    kubernetesClient.pods().withTimeout(1, TimeUnit.SECONDS).delete();
    kubernetesClient.configMaps().withTimeout(1, TimeUnit.SECONDS).delete();
    kubernetesClient.secrets().withTimeout(1, TimeUnit.SECONDS).delete();
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
    helmService = new HelmService(JKubeConfiguration.builder()
      .project(JavaProject.builder()
        .buildDirectory(tempDir.resolve("target").toFile())
        .build())
      .clusterConfiguration(ClusterConfiguration.from(kubernetesClient.getConfiguration()).build())
      .build(), new ResourceServiceConfig(), kitLogger);
  }

  @AfterEach
  void tearDown() {
    kubernetesClient.close();
  }

  @Test
  @DisplayName("uninstall invoked, then log uninstallation details after uninstall")
  void uninstall_thenLogUninstalledChartDetails() throws IOException {
    // Given
    helmService.generateHelmCharts(helmConfig);
    helmService.install(helmConfig);
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
