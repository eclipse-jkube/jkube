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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(Parameterized.class)
public class ServiceAccountIT {
  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Parameterized.Parameters(name = "resource task on project = {0} with {1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "serviceaccount", new String[] {}, "default"},
        new Object[] { "serviceaccount-via-groovy-dsl", new String[] {}, "default"},
        new Object[] { "serviceaccount", new String[] {"-Pjkube.enricher.jkube-serviceaccount.skipCreate=true"}, "skip-create"},
        new Object[] { "serviceaccount-via-groovy-dsl", new String[] {"-Pjkube.enricher.jkube-serviceaccount.skipCreate=true"}, "skip-create"}
    );
  }

  @Parameterized.Parameter
  public String testProjectDir;

  @Parameterized.Parameter (1)
  public String[] arguments;

  @Parameterized.Parameter (2)
  public String expectedDir;

  @Test
  public void k8sResource_whenRun_generatesServiceAccount() throws IOException, ParseException {
    // When
    List<String> gradleArgs = new ArrayList<>(Arrays.asList(arguments));
    gradleArgs.add("k8sResource");
    gradleArgs.add("--stacktrace");
    final BuildResult result = gradleRunner.withITProject(testProjectDir)
        .withArguments(gradleArgs.toArray(new String[0]))
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", expectedDir, "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }

  @Test
  public void ocResource_whenRun_generatesServiceAccount() throws IOException, ParseException {
    // When
    List<String> gradleArgs = new ArrayList<>(Arrays.asList(arguments));
    gradleArgs.add("ocResource");
    gradleArgs.add("--stacktrace");
    final BuildResult result = gradleRunner.withITProject(testProjectDir)
        .withArguments(gradleArgs.toArray(new String[0])).build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", expectedDir, "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Converting Deployment to DeploymentConfig")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }
}
