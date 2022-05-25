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
public class MultiEnvironmentsIT {
  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Parameterized.Parameters(name = "environment {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "common" , "common"},
        new Object[] { "dev" , "dev"},
        new Object[] { "prod" , "prod"},
        new Object[] { "common,dev", "common-and-dev"},
        new Object[] { "common,prod", "common-and-prod"}
    );
  }

  @Parameterized.Parameter
  public String environment;

  @Parameterized.Parameter (1)
  public String expectedDirectory;

  @Test
  public void k8sResource_whenRun_generatesK8sManifestsContainingConfigMap() throws IOException, ParseException {
    // When
    gradleRunner.withITProject("multi-environments")
        .withArguments("build", "-Pjkube.environment=" + environment, "k8sResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", expectedDirectory, "kubernetes.yml"));
  }

  @Test
  public void ocResource_whenRun_generatesOpenShiftManifestsContainingConfigMap() throws IOException, ParseException {
    // When
    gradleRunner.withITProject("multi-environments")
        .withArguments("build", "-Pjkube.environment=" + environment, "ocResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", expectedDirectory, "openshift.yml"));
  }
}
