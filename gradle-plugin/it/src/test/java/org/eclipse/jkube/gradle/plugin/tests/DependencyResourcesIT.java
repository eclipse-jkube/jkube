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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.jkube.kit.common.ResourceVerify;
import org.gradle.testkit.runner.BuildResult;
import org.junit.Rule;
import org.junit.Test;

import net.minidev.json.parser.ParseException;

public class DependencyResourcesIT {

  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Test
  public void k8sResource_whenRun_generatesK8sManifestsIncludingDependencies() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("dependency-resources")
        .withArguments(addCleanArgument("jar", "k8sResource", "--stacktrace"))
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(
        gradleRunner.resolveFile("dependent", "build", "classes", "java", "main",
            "META-INF", "jkube", "kubernetes.yml"),
        gradleRunner.resolveFile("expected", "kubernetes-defaults.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }

  @Test
  public void k8sResource_whenRunWithReplicas_generatesK8sManifestsIncludingDependencies() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("dependency-resources")
        .withArguments(addCleanArgument("clean", "jar", "k8sResource", "-Pjkube.replicas=1337", "--stacktrace"))
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(
        gradleRunner.resolveFile("dependent", "build", "classes", "java", "main",
            "META-INF", "jkube", "kubernetes.yml"),
        gradleRunner.resolveFile("expected", "kubernetes-with-replica-override.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }

  private static String[] addCleanArgument(String... arguments) {
    Builder<String> builder = Stream.builder();
    /*
     * For some weird reason, depending on the order of execution of the tests, the first running test locks the entire
     * build folder even after it has finished running and, as a result, the next test can't clean such folder and it
     * fails.
     * Considering that the tests operate on different resources, it should be safe to skip the clean step completely.
     */
    if (!SystemUtils.IS_OS_WINDOWS) {
      builder.add("clean");
    }
    Arrays.stream(arguments).forEach(builder);

    return builder.build().toArray(String[]::new);
  }
}
