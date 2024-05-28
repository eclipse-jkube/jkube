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
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KubernetesHelmDependencyUpdateTaskTest {
  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  @BeforeEach
  void setUp() throws IOException {
    final TestKubernetesExtension extension = new TestKubernetesExtension();
    Helm.create().withDir(taskEnvironment.getRoot().toPath()).withName("empty-project").call();
    Path helmChartOutputDir = taskEnvironment.getRoot().toPath().resolve("build").resolve("jkube").resolve("helm");
    Files.createDirectories(helmChartOutputDir.resolve("kubernetes"));
    FileUtils.copyDirectory(taskEnvironment.getRoot().toPath().resolve("empty-project").toFile(), helmChartOutputDir.resolve("kubernetes").toFile());
    Files.write(helmChartOutputDir.resolve("kubernetes").resolve("Chart.yaml"),
        ("\ndependencies:\n" +
            "  - name: the-dependency\n" +
            "    version: 0.1.0\n" +
            "    repository: file://../../../../the-dependency\n").getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.APPEND);
    System.setProperty("jkube.kubernetesTemplate", taskEnvironment.getRoot().getAbsolutePath());
    extension.helm = HelmConfig.builder().chartExtension("tgz").outputDir(helmChartOutputDir.toString()).build();
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
  void runTask_withInvalidHelmDependency_shouldThrowException() {
    // Given
    KubernetesHelmDependencyUpdateTask kubernetesHelmDependencyUpdateTask = new KubernetesHelmDependencyUpdateTask(KubernetesExtension.class);
    // When + Then
    assertThatThrownBy(kubernetesHelmDependencyUpdateTask::runTask)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("the-dependency not found");
  }

  @Test
  void runTask_withHelmDependencyPresent_shouldSucceed() {
    // Given
    KubernetesHelmDependencyUpdateTask kubernetesHelmDependencyUpdateTask = new KubernetesHelmDependencyUpdateTask(KubernetesExtension.class);
    Helm.create().withName("the-dependency").withDir(taskEnvironment.getRoot().toPath()).call();

    // When
    kubernetesHelmDependencyUpdateTask.runTask();
    // Then
    verify(taskEnvironment.logger).lifecycle("k8s: Running Helm Dependency Upgrade empty-project 0.1.0");
    verify(taskEnvironment.logger).lifecycle("k8s: Saving 1 charts");
    verify(taskEnvironment.logger).lifecycle("k8s: Deleting outdated charts");
  }
}
