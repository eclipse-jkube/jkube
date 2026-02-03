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
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.gradle.plugin.GradleUtil;
import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.common.util.AsyncUtil;
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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableKubeAPIServer
class OpenShiftHelmTestTaskTest {
  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();
  @KubeConfig
  static String kubeConfigYaml;
  private KubernetesClient kubernetesClient;
  private TestOpenShiftExtension extension;

  @BeforeEach
  void setUp() throws IOException {
    extension = new TestOpenShiftExtension();
    kubernetesClient = new KubernetesClientBuilder().withConfig(Config.fromKubeconfig(kubeConfigYaml)).build();
    kubernetesClient.apps().deployments().withTimeout(1, TimeUnit.SECONDS).delete();
    kubernetesClient.pods().withTimeout(1, TimeUnit.SECONDS).delete();
    kubernetesClient.configMaps().withTimeout(1, TimeUnit.SECONDS).delete();
    kubernetesClient.secrets().withTimeout(1, TimeUnit.SECONDS).delete();
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
    kubernetesClient.close();
    System.clearProperty("jkube.kubernetesTemplate");
  }

  @Test
  @DisplayName("when Helm Release Installed on OpenShift Cluster, then test Helm Release")
  void runTask_withHelmReleasePresentInKubernetesCluster_shouldSucceed() {
    // Given
    OpenShiftHelmTestTask openShiftHelmTestTask = new OpenShiftHelmTestTask(OpenShiftExtension.class);
    openShiftHelmTestTask.kitLogger = new KitLogger.SilentLogger();
    openShiftHelmTestTask.kubernetesExtension.javaProject = GradleUtil.convertGradleProject(openShiftHelmTestTask.getProject());
    openShiftHelmTestTask.init();
    openShiftHelmTestTask.jKubeServiceHub.getHelmService().install(extension.helm);
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
    verify(taskEnvironment.logger, times(1)).lifecycle("oc: NAME: empty-project");
    verify(taskEnvironment.logger, times(1)).lifecycle("oc: STATUS: deployed");
    verify(taskEnvironment.logger, times(1)).lifecycle("oc: REVISION: 1");
    verify(taskEnvironment.logger, times(1)).lifecycle("oc: Phase: Succeeded");
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

  @Test
  void runTask_withSkip_shouldDoNothing() {
    // Given
    extension = new TestOpenShiftExtension() {
      @Override
      public Property<Boolean> getSkip() {
        return super.getSkip().value(true);
      }
    };
    when(taskEnvironment.project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
    final OpenShiftHelmTestTask task = new OpenShiftHelmTestTask(OpenShiftExtension.class);
    when(task.getName()).thenReturn("ocHelmTest");

    // When
    task.runTask();

    // Then
    verify(taskEnvironment.logger, times(1)).lifecycle(contains("oc: `ocHelmTest` task is skipped."));
  }
}
