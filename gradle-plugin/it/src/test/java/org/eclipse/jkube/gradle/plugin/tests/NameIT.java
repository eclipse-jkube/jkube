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
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(Parameterized.class)
public class NameIT {
  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Parameterized.Parameters(name = "k8sResource {0} configured name = {1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "default", "" },
        new Object[] { "custom-name", "configured-name"}
    );
  }

  @Parameterized.Parameter
  public String expectedDir;

  @Parameterized.Parameter (1)
  public String nameFromEnricherConfig;

  @Test
  public void k8sResource_whenRun_generatesK8sManifestsWithDefaultName() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("name")
        .withArguments("-Pjkube.enricher.jkube-name.name=" + nameFromEnricherConfig, "k8sResource", "--stacktrace")
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
  public void ocResource_whenRun_generatesOpenShiftManifestsWithDefaultName() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("name")
        .withArguments("-Pjkube.enricher.jkube-name.name=" + nameFromEnricherConfig, "ocResource", "--stacktrace")
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
