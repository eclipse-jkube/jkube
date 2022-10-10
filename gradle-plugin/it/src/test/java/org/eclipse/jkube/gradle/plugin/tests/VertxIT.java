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
public class VertxIT {
  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Parameterized.Parameters(name = "resource task with {0} generates deployment with liveness and readiness probes")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] {"DSL configured probes", new String[] { "-PconfigMode=enricherConfig" }},

        new Object[] { "Property configured probes", new String[] {
            "-Pvertx.health.liveness.port=8888",
            "-Pvertx.health.liveness.path=/health/live",
            "-Pvertx.health.liveness.scheme=HTTPS",
            "-Pvertx.health.initialDelay=3",
            "-Pvertx.health.period=3",
            "-Pvertx.health.readiness.port=8888",
            "-Pvertx.health.readiness.path=/health/ready",
            "-Pvertx.health.readiness.scheme=HTTPS" }
        }
    );
  }

  @Parameterized.Parameter
  public String testDescription;

  @Parameterized.Parameter(1)
  public String[] arguments;

  @Test
  public void k8sResource_whenRun_generatesK8sManifestsWithProbes() throws IOException, ParseException {
    // When
    List<String> gradleArgs = new ArrayList<>(Arrays.asList(arguments));
    gradleArgs.add("build");
    gradleArgs.add("k8sResource");
    gradleArgs.add("--stacktrace");
    final BuildResult result = gradleRunner.withITProject("vertx")
        .withArguments(gradleArgs.toArray(new String[0]))
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Running generator vertx")
        .contains("jkube-controller: Adding a default Deployment")
        .contains("jkube-service: Adding a default service")
        .contains("jkube-healthcheck-vertx: Adding readiness probe on port 8888")
        .contains("jkube-healthcheck-vertx: Adding liveness probe on port 8888")
        .contains("jkube-service-discovery: Using first mentioned service port '8888' ")
        .contains("jkube-revision-history: Adding revision history limit to 2");
  }

  @Test
  public void ocResource_whenRun_generatesOpenShiftManifestsWithProbes() throws IOException, ParseException {
    // When
    List<String> gradleArgs = new ArrayList<>(Arrays.asList(arguments));
    gradleArgs.add("build");
    gradleArgs.add("ocResource");
    gradleArgs.add("--stacktrace");
    final BuildResult result = gradleRunner.withITProject("vertx")
        .withArguments(gradleArgs.toArray(new String[0]))
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Running generator vertx")
        .contains("jkube-controller: Adding a default Deployment")
        .contains("jkube-service: Adding a default service")
        .contains("jkube-openshift-deploymentconfig: Converting Deployment to DeploymentConfig")
        .contains("jkube-healthcheck-vertx: Adding readiness probe on port 8888")
        .contains("jkube-healthcheck-vertx: Adding liveness probe on port 8888")
        .contains("jkube-service-discovery: Using first mentioned service port '8888' ")
        .contains("jkube-revision-history: Adding revision history limit to 2");
  }
}
