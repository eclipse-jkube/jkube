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
public class ProjectLabelIT {
  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Parameterized.Parameters(name = "k8sResource {0} configured project-label ")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "default" , new String[] {}},
        new Object[] { "custom" , new String[] {"-Pjkube.enricher.jkube-project-label.useProjectLabel=true", "-Pjkube.enricher.jkube-project-label.provider=custom-provider"}},
        new Object[] { "app" , new String[] {"-Pjkube.enricher.jkube-project-label.app=custom-app" }},
        new Object[] { "group", new String[] {"-Pjkube.enricher.jkube-project-label.group=org.example"}},
        new Object[] { "version", new String[] {"-Pjkube.enricher.jkube-project-label.version=0.1.0"}}
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
    final BuildResult result = gradleRunner.withITProject("project-label")
        .withArguments(gradleArgs.toArray(new String[0]))
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", expectedDir, "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Running in Kubernetes mode")
        .contains("Using resource templates from")
        .contains("Adding a default Deployment")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }

  @Test
  public void ocResource_whenRun_generatesOpenShiftManifestsWithProjectLabels() throws IOException, ParseException {
    // When
    List<String> gradleArgs = new ArrayList<>(Arrays.asList(arguments));
    gradleArgs.add("ocResource");
    gradleArgs.add("--stacktrace");
    final BuildResult result = gradleRunner.withITProject("project-label")
        .withArguments(gradleArgs.toArray(new String[0]))
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", expectedDir, "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Running in OpenShift mode")
        .contains("Using resource templates from")
        .contains("Adding a default Deployment")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }
}
