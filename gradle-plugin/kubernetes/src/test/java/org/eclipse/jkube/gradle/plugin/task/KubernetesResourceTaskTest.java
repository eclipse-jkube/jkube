/**
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

import java.io.IOException;
import java.nio.file.Paths;

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

public class KubernetesResourceTaskTest {

  @Rule
  public TaskEnvironment taskEnvironment = new TaskEnvironment();

  @Before
  public void setUp() throws IOException {
    when(taskEnvironment.project.getExtensions().getByType(KubernetesExtension.class))
        .thenReturn(new TestKubernetesExtension());
  }

  @Test
  public void runTask_withImageConfiguration_shouldGenerateResources() {
    // Given
    KubernetesResourceTask resourceTask = new KubernetesResourceTask(KubernetesExtension.class);
    // When
    resourceTask.runTask();
    // Then
    assertThat(taskEnvironment.getRoot().toPath()
        .resolve(Paths.get("build", "classes", "java", "main", "META-INF", "jkube", "kubernetes.yml")))
            .exists()
            .hasContent("---\n" +
                "apiVersion: v1\n" +
                "kind: List\n");
  }
}