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
import org.eclipse.jkube.gradle.plugin.OpenShiftExtension;
import org.eclipse.jkube.gradle.plugin.TestOpenShiftExtension;
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

class OpenShiftHelmLintTaskTest {

  @RegisterExtension
  private final TaskEnvironmentExtension taskEnvironment = new TaskEnvironmentExtension();

  @BeforeEach
  void setUp() throws IOException {
    System.setProperty("jkube.kubernetesTemplate", taskEnvironment.getRoot().getAbsolutePath());
    final TestOpenShiftExtension extension = new TestOpenShiftExtension();
    extension.helm = HelmConfig.builder().chartExtension("tgz").build();
    extension.isUseColor = false;
    when(taskEnvironment.project.getName()).thenReturn("empty-project");
    when(taskEnvironment.project.getVersion()).thenReturn("0.1.0");
    when(taskEnvironment.project.getExtensions().getByType(OpenShiftExtension.class)).thenReturn(extension);
  }

  @AfterEach
  void tearDown() {
    System.clearProperty("jkube.kubernetesTemplate");
  }

  @Test
  void runTask_withMissingHelmPackage_shouldThrowException() {
    OpenShiftHelmLintTask openShiftHelmLintTask = new OpenShiftHelmLintTask(OpenShiftExtension.class);
    assertThatThrownBy(openShiftHelmLintTask::runTask)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Linting failed");
    verify(taskEnvironment.logger).lifecycle("oc: Linting empty-project 0.1.0");
    verify(taskEnvironment.logger).lifecycle(startsWith("oc: Using packaged file:"));
  }

  @Test
  void runTask_withHelmPackage_shouldSucceed() {
    OpenShiftHelmLintTask openShiftHelmLintTask = new OpenShiftHelmLintTask(OpenShiftExtension.class);
    Helm.create().withDir(taskEnvironment.getRoot().toPath()).withName("empty-project").call()
      .packageIt().withDestination(taskEnvironment.getRoot().toPath().resolve("build").resolve("jkube").resolve("helm").resolve("empty-project").resolve("openshift")).call();
    openShiftHelmLintTask.runTask();
    verify(taskEnvironment.logger).lifecycle("oc: Linting empty-project 0.1.0");
    verify(taskEnvironment.logger).lifecycle(startsWith("oc: Using packaged file:"));
    verify(taskEnvironment.logger).lifecycle("oc: [INFO] Chart.yaml: icon is recommended");
    verify(taskEnvironment.logger).lifecycle("oc: Linting successful");
  }
}
