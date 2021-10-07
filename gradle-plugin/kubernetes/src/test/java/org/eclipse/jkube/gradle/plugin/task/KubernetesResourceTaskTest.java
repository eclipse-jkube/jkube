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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.gradle.plugin.TestKubernetesExtension;

import org.apache.commons.io.FileUtils;
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

  @Test
  public void runTask_withMultipleFiles_shouldInterpolateApplicableFragments() throws IOException {
    // Given
    final Map<String, Object> properties = new HashMap<>();
    properties.put("some.property", "value");
    properties.put("unused", "value");
    properties.put("other.property", "value2");
    withProperties(properties);
    withResourceFragment("configmap.yml", "key: ${some.property}");
    withResourceFragment("second-configmap.yml",
        "field-1: ${some.property}\nfield-2: ${other.property}\nf3: ${not.here}\nf4: Hard");
    KubernetesResourceTask resourceTask = new KubernetesResourceTask(KubernetesExtension.class);
    // When
    resourceTask.runTask();
    // Then
    assertThat(taskEnvironment.getRoot().toPath().resolve("build").resolve("jkube"))
        .exists()
        .satisfies(p -> assertThat(p.resolve("configmap.yml")).hasContent("key: value"))
        .satisfies(p -> assertThat(p.resolve("second-configmap.yml"))
            .hasContent("field-1: value\nfield-2: value2\nf3: ${not.here}\nf4: Hard"));
  }

  private void withProperties(Map<String, ?> properties) {
    when(taskEnvironment.project.getProperties()).thenAnswer(i -> properties);
  }

  private void withResourceFragment(String name, String content) throws IOException {
    final File dir = taskEnvironment.getRoot().toPath().resolve("src").resolve("main").resolve("jkube").toFile();
    FileUtils.forceMkdir(dir);
    FileUtils.write(new File(dir, name), content, StandardCharsets.UTF_8);
  }
}