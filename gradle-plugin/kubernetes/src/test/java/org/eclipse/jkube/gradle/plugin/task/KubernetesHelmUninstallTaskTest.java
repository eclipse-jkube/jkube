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
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.eclipse.jkube.kit.common.util.KubernetesMockServerUtil.prepareMockWebServerExpectationsForAggregatedDiscoveryEndpoints;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableKubernetesMockClient(crud = true)
class KubernetesHelmUninstallTaskTest {
  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();
  private KubernetesClient kubernetesClient;
  private KubernetesMockServer server;
  private TestKubernetesExtension extension;

  @BeforeEach
  void setUp() throws IOException {
    extension = new TestKubernetesExtension();
    // Remove after https://github.com/fabric8io/kubernetes-client/issues/6062 is fixed
    prepareMockWebServerExpectationsForAggregatedDiscoveryEndpoints(server);
    Helm.create().withDir(taskEnvironment.getRoot().toPath()).withName("empty-project").call();
    Path helmChartOutputDir = taskEnvironment.getRoot().toPath().resolve("build").resolve("jkube").resolve("helm");
    Files.createDirectories(helmChartOutputDir.resolve("kubernetes"));
    FileUtils.copyDirectory(taskEnvironment.getRoot().toPath().resolve("empty-project").toFile(), helmChartOutputDir.resolve("kubernetes").toFile());
    System.setProperty("jkube.kubernetesTemplate", taskEnvironment.getRoot().getAbsolutePath());
    extension.helm = HelmConfig.builder()
      .chartExtension("tgz")
      .disableOpenAPIValidation(true)
      .outputDir(helmChartOutputDir.toString()).build();
    extension.access = ClusterConfiguration.from(kubernetesClient.getConfiguration()).build();
    extension.isUseColor = false;
    when(taskEnvironment.project.getName()).thenReturn("empty-project");
    when(taskEnvironment.project.getVersion()).thenReturn("0.1.0");
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
  }

  @AfterEach
  void tearDown() {
    System.clearProperty("jkube.kubernetesTemplate");
  }
  
  @Test
  @DisplayName("when Helm Release Installed on Kubernetes Cluster, then uninstall Helm Release")
  void runTask_withHelmReleasePresentInKubernetesCluster_shouldSucceed() {
    // Given
    KubernetesHelmUninstallTask kubernetesHelmUninstallTask = new KubernetesHelmUninstallTask(KubernetesExtension.class);
    kubernetesHelmUninstallTask.init();
    kubernetesHelmUninstallTask.jKubeServiceHub.getHelmService().install(extension.helm);
    // Should be removed once https://github.com/fabric8io/kubernetes-client/issues/6220 gets fixed
    Secret secret = kubernetesClient.secrets().withName("sh.helm.release.v1.empty-project.v1").get();
    server.expect().get().withPath("/api/v1/namespaces/test/secrets?labelSelector=name%3Dempty-project%2Cowner%3Dhelm")
      .andReturn(200, new SecretListBuilder()
        .addToItems(secret)
        .build())
      .once();

    // When
    kubernetesHelmUninstallTask.runTask();
    // Then
    verify(taskEnvironment.logger).lifecycle("k8s: release \"empty-project\" uninstalled");
  }

  @Test
  @DisplayName("Helm Release not installed on Kubernetes cluster, then throw exception")
  void execute_whenReleaseNotPresent_thenThrowException() {
    // Given
    KubernetesHelmUninstallTask kubernetesHelmUninstallTask = new KubernetesHelmUninstallTask(KubernetesExtension.class);

    // When + Then
    assertThatIllegalStateException()
      .isThrownBy(kubernetesHelmUninstallTask::runTask)
      .withMessageContaining(" not found");
  }
}
