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

import net.minidev.json.parser.ParseException;
import org.eclipse.jkube.kit.common.ResourceVerify;
import org.gradle.testkit.runner.BuildResult;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;

public class KubernetesExtensionIT {

  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Test
  public void k8sConfigView_containsAFullyDeserializedConfiguration() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner
        .withITProject("extension-configuration")
        .withArguments("k8sConfigView", "--stacktrace").build();
    // Then
    final String output = result.getOutput();
    ResourceVerify.verifyResourceDescriptors(
        output.substring(output.indexOf("---"), output.indexOf("BUILD SUCCESSFUL in")),
        readFileToString(gradleRunner.resolveFile("expected", "expected-config.yml"), UTF_8),
        false);
  }
}
