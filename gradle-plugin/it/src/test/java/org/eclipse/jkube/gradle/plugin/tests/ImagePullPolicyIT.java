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
public class ImagePullPolicyIT {
  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Parameterized.Parameters(name = "resource task with {1}  ")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "property" , new String[] { "-Pjkube.imagePullPolicy=Always-property" }},
        new Object[] { "resourceconfig" , new String[] { "-PimagePullPolicyMode=resourceconfig" }},
        new Object[] { "enricherconfig" , new String[] { "-PimagePullPolicyMode=enricherconfig" }},
        new Object[] { "fragment", new String[] { "-Pjkube.resourceDir=./fragments" }},
        new Object[] { "property-and-resourceconfig", new String[] {"-Pjkube.imagePullPolicy=Always-property", "-PimagePullPolicyMode=property-and-resourceconfig" }},
        new Object[] { "property-and-enricherconfig", new String[] {"-Pjkube.imagePullPolicy=Always-property", "-PimagePullPolicyMode=property-and-enricherconfig"}},
        new Object[] { "property-and-fragment", new String[] {"-Pjkube.imagePullPolicy=Always-property", "-Pjkube.resourceDir=./fragments", "-PimagePullPolicyMode=property", }}
    );
  }

  @Parameterized.Parameter
  public String expectedDir;

  @Parameterized.Parameter (1)
  public String[] arguments;

  @Test
  public void k8sResource_whenRun_generatesK8sManifestsWithProjectLabels() throws IOException, ParseException {
    // When
    List<String> gradleArgs = new ArrayList<>(Arrays.asList(arguments));
    gradleArgs.add("k8sResource");
    gradleArgs.add("--stacktrace");
    final BuildResult result = gradleRunner.withITProject("imagepullpolicy")
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
  public void ocResource_whenRun_generatesOpenShiftManifestsWithProjectLabels() throws IOException, ParseException {
    // When
    List<String> gradleArgs = new ArrayList<>(Arrays.asList(arguments));
    gradleArgs.add("ocResource");
    gradleArgs.add("--stacktrace");
    final BuildResult result = gradleRunner.withITProject("imagepullpolicy")
        .withArguments(gradleArgs.toArray(new String[0]))
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", expectedDir, "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }
}
