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

import static org.assertj.core.api.Assertions.assertThat;

public class KubernetesExtensionIT {

  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Test
  public void k8sConfigView_containsAFullyDeserializedConfiguration() {
    // When
    final BuildResult result = gradleRunner
        .withITProject("extension-configuration").withArguments("k8sConfigView", "--stacktrace").build();
    // Then
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("offline: true")
        .contains("buildStrategy: \"jib\"")
        .contains("controllerName: \"test\"")
        .contains("  configMap:\n" +
            "    name: \"configMap-name\"\n" +
            "    entries:\n" +
            "    - name: \"test\"\n" +
            "      value: \"value\"")
        .contains("enricher:\n  excludes:\n  - \"jkube-expose\"")
        .contains("- name: \"registry/extension-configuration:0.0.1-SNAPSHOT\"")
        .contains("- name: \"registry/image:tag\"")
        .contains("    from: \"busybox\"");
  }
}
