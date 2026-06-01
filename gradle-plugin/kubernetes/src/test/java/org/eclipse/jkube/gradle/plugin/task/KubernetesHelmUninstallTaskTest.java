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
import io.fabric8.kubeapitest.junit.EnableKubeAPIServer;
import io.fabric8.kubeapitest.junit.KubeConfig;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.gradle.plugin.GradleUtil;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.gradle.api.provider.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableKubeAPIServer
class KubernetesHelmUninstallTaskTest {
  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();
  @KubeConfig
  static String kubeConfigYaml;
  private KubernetesClient kubernetesClient;
  private TestKubernetesExtension extension;

  @BeforeEach
  void setUp() throws IOException {
    extension = new TestKubernetesExtension();
    kubernetesClient = new KubernetesClientBuilder().withConfig(Config.fromKubeconfig(kubeConfigYaml)).build();
    kubernetesClient.apps().deployments().withTimeout(1, TimeUnit.SECONDS).delete();
    kubernetesClient.pods().withTimeout(1, TimeUnit.SECONDS).delete();
    kubernetesClient.configMaps().withTimeout(1, TimeUnit.SECONDS).delete();
    kubernetesClient.secrets().withTimeout(1, TimeUnit.SECONDS).delete();
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
    kubernetesClient.close();
    System.clearProperty("jkube.kubernetesTemplate");
  }

  @Test
  @DisplayName("when Helm Release Installed on Kubernetes Cluster, then uninstall Helm Release")
  void runTask_withHelmReleasePresentInKubernetesCluster_shouldSucceed() {
    // Given
    KubernetesHelmUninstallTask kubernetesHelmUninstallTask = new KubernetesHelmUninstallTask(KubernetesExtension.class);
    kubernetesHelmUninstallTask.kitLogger = new KitLogger.SilentLogger();
    kubernetesHelmUninstallTask.kubernetesExtension.javaProject = GradleUtil.convertGradleProject(kubernetesHelmUninstallTask.getProject());
    kubernetesHelmUninstallTask.init();
    kubernetesHelmUninstallTask.jKubeServiceHub.getHelmService().install(extension.helm);

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

  @Test
  void runTask_withSkip_shouldDoNothing() {
    // Given
    extension = new TestKubernetesExtension() {
      @Override
      public Property<Boolean> getSkip() {
        return super.getSkip().value(true);
      }
    };
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class)).thenReturn(extension);
    final KubernetesHelmUninstallTask task = new KubernetesHelmUninstallTask(KubernetesExtension.class);
    when(task.getName()).thenReturn("k8sHelmUninstall");

    // When
    task.runTask();

    // Then
    verify(taskEnvironment.logger, times(1)).lifecycle(contains("k8s: `k8sHelmUninstall` task is skipped."));
  }
}
