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
import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.gradle.api.provider.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableKubeAPIServer
class OpenShiftHelmInstallTaskTest {
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
    Files.write(helmChartOutputDir.resolve("openshift").resolve("Chart.yaml"),
      ("\ndependencies:\n" +
        "  - name: the-dependency\n" +
        "    version: 0.1.0\n" +
        "    repository: file://../../../../the-dependency\n").getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.APPEND);
    System.setProperty("jkube.kubernetesTemplate", taskEnvironment.getRoot().getAbsolutePath());
    extension.helm = HelmConfig.builder()
      .chartExtension("tgz")
      .installDependencyUpdate(true)
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
  void runTask_withHelmDependencyPresent_shouldSucceed() {
    // Given
    OpenShiftHelmInstallTask openShiftHelmInstallTask = new OpenShiftHelmInstallTask(OpenShiftExtension.class);
    Helm.create().withName("the-dependency").withDir(taskEnvironment.getRoot().toPath()).call();

    // When
    openShiftHelmInstallTask.runTask();
    // Then
    verify(taskEnvironment.logger).lifecycle("oc: NAME: empty-project");
    verify(taskEnvironment.logger).lifecycle("oc: NAMESPACE: ");
    verify(taskEnvironment.logger).lifecycle("oc: STATUS: deployed");
    verify(taskEnvironment.logger).lifecycle("oc: REVISION: 1");
    verify(taskEnvironment.logger).lifecycle("oc: Saving 1 charts");
    verify(taskEnvironment.logger).lifecycle("oc: Deleting outdated charts");
  }

  @Test
  void runTask_withHelmDependencyAbsent_shouldThrowException() {
    // Given
    OpenShiftHelmInstallTask openShiftHelmInstallTask = new OpenShiftHelmInstallTask(OpenShiftExtension.class);
    // When + Then
    assertThatIllegalStateException()
      .isThrownBy(openShiftHelmInstallTask::runTask)
      .withMessageContaining("the-dependency not found");
  }

  @Test
  void runTask_withInstallDependencyUpdateDisabled_shouldThrowException() {
    // Given
    extension.helm = extension.helm.toBuilder()
      .installDependencyUpdate(false)
      .build();
    when(taskEnvironment.project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
    OpenShiftHelmInstallTask openShiftHelmInstallTask = new OpenShiftHelmInstallTask(OpenShiftExtension.class);
    // When + Then
    assertThatIllegalStateException()
      .isThrownBy(openShiftHelmInstallTask::runTask)
      .withMessage("An error occurred while checking for chart dependencies. " +
        "You may need to run `helm dependency build` to fetch missing dependencies: found in Chart.yaml, but missing in charts/ directory: the-dependency");
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
    final OpenShiftHelmInstallTask task = new OpenShiftHelmInstallTask(OpenShiftExtension.class);
    when(task.getName()).thenReturn("ocHelmInstall");

    // When
    task.runTask();

    // Then
    verify(taskEnvironment.logger, times(1)).lifecycle(contains("oc: `ocHelmInstall` task is skipped."));
  }
}
