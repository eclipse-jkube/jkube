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
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KubernetesHelmLintTaskTest {

  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  @BeforeEach
  void setUp() throws IOException {
    System.setProperty("jkube.kubernetesTemplate", taskEnvironment.getRoot().getAbsolutePath());
    final TestKubernetesExtension extension = new TestKubernetesExtension();
    extension.helm = HelmConfig.builder().chartExtension("tgz").build();
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
  void runTask_withMissingHelmPackage_shouldThrowException() {
    KubernetesHelmLintTask kubernetesHelmLintTask = new KubernetesHelmLintTask(KubernetesExtension.class);
    assertThatThrownBy(kubernetesHelmLintTask::runTask)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Linting failed");
    verify(taskEnvironment.logger).lifecycle("k8s: Linting empty-project 0.1.0");
    verify(taskEnvironment.logger).lifecycle(startsWith("k8s: Using packaged file:"));
  }

  @Test
  void runTask_withHelmPackage_shouldSucceed() {
    KubernetesHelmLintTask kubernetesHelmLintTask = new KubernetesHelmLintTask(KubernetesExtension.class);
    Helm.create().withDir(taskEnvironment.getRoot().toPath()).withName("empty-project").call()
      .packageIt().withDestination(taskEnvironment.getRoot().toPath().resolve("build").resolve("jkube").resolve("helm").resolve("empty-project").resolve("kubernetes")).call();
    kubernetesHelmLintTask.runTask();
    verify(taskEnvironment.logger).lifecycle("k8s: Linting empty-project 0.1.0");
    verify(taskEnvironment.logger).lifecycle(startsWith("k8s: Using packaged file:"));
    verify(taskEnvironment.logger).lifecycle("k8s: [INFO] Chart.yaml: icon is recommended");
    verify(taskEnvironment.logger).lifecycle("k8s: Linting successful");
  }
}
