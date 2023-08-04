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
package org.eclipse.jkube.gradle.plugin.tests;

import net.minidev.json.parser.ParseException;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.ResourceVerify;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

class KubernetesExtensionIT {

  @RegisterExtension
  final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @Test
  void k8sConfigView_containsAFullyDeserializedConfiguration() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner
        .withITProject("extension-configuration")
        .withArguments("k8sConfigView", "--stacktrace").build();
    // Then
    final String output = result.getOutput();
    ResourceVerify.verifyResourceDescriptors(
        output.substring(output.indexOf("---"), output.lastIndexOf("---")),
        FileUtils.readFileToString(gradleRunner.resolveFile("expected", "expected-config.yml"), StandardCharsets.UTF_8),
        false);
  }
}
