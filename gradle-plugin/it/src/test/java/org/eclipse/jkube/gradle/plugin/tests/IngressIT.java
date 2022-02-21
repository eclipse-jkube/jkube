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
public class IngressIT {
  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Parameterized.Parameters(name = "k8sResource with {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "zero-config-no-host", "", "", "", "false"},
        new Object[] { "zero-config-host-enricher-config", "test.example.com", "", "", "false" },
        new Object[] { "zero-config-extensionsv1beta1-enricher-config", "", "extensions/v1beta1", "", "false"},
        new Object[] { "zero-config-extensionsv1beta1-host-enricher-config", "test.example.com", "extensions/v1beta1", "", "false"},
        new Object[] { "groovy-dsl-config", "", "", "org.eclipse.jkube.quickstart", "true"}
    );
  }

  @Parameterized.Parameter
  public String profileName;

  @Parameterized.Parameter (1)
  public String host;

  @Parameterized.Parameter (2)
  public String targetApiVersion;

  @Parameterized.Parameter (3)
  public String domain;

  @Parameterized.Parameter (4)
  public String groovyDslEnabled;

  @Test
  public void k8sResourceTask_whenRun_generatesK8sManifestWithIngress() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("ingress")
        .withArguments("k8sResource",
            "-Pjkube.enricher.jkube-ingress.host=" + host,
            "-Pjkube.enricher.jkube-ingress.targetApiVersion=" + targetApiVersion,
            "-Pjkube.domain=" + domain,
            "-Pgroovy-dsl-config=" + groovyDslEnabled,
            "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", profileName, "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Running in Kubernetes mode")
        .contains("Using resource templates from")
        .contains("Adding a default Deployment")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }
}
