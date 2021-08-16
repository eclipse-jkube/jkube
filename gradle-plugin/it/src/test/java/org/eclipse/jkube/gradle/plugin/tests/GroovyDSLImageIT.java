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
package org.eclipse.jkube.gradle.plugin.tests;

import org.gradle.testkit.runner.BuildResult;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GroovyDSLImageIT {
  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Test
  public void k8sResource_whenRun_generatesK8sManifests() throws IOException {
    // Given + When
    final BuildResult result = gradleRunner.withITProject("groovy-dsl-image").withArguments("k8sResource").build();

    // Then
    File actualKubernetesYaml = new File("src/it/groovy-dsl-image/build/META-INF/jkube/kubernetes.yml");
    String actualKubernetesYamlStr = new String(Files.readAllBytes(actualKubernetesYaml.toPath()));
    assertThat(result).extracting(BuildResult::getOutput).asString()
      .contains("Running in Kubernetes mode")
      .contains("Using resource templates from")
      .contains("Adding a default Deployment")
      .contains("Adding revision history limit to 2")
      .contains("Using first mentioned service port")
      .contains("validating");
    assertThat(actualKubernetesYamlStr)
      .contains("apiVersion: v1\nkind: List\nitems:\n")
      .contains("- apiVersion: v1\n  kind: Service\n  metadata:")
      .contains("    annotations:")
      .contains("    labels:")
      .contains("  spec:")
      .contains("ports:\n" +
                "    - name: http\n" +
                "      port: 8080\n" +
                "      protocol: TCP\n" +
                "      targetPort: 8080")
      .contains("- apiVersion: apps/v1\n  kind: Deployment\n  metadata:")
      .contains("  spec:\n" +
               "    replicas: 1\n" +
               "    revisionHistoryLimit: 2")
      .contains("    selector:\n" +
                "      matchLabels:\n" +
                "        app: groovy-dsl-image\n" +
                "        provider: jkube\n" +
                "        group: org.eclipse.jkube.integration.tests.gradle")
      .contains("image: repository/groovy-dsl-image-test:latest")
      .contains("imagePullPolicy: IfNotPresent")
      .contains("ports:\n          - containerPort: 8080\n            name: http\n            protocol: TCP");
  }
}
