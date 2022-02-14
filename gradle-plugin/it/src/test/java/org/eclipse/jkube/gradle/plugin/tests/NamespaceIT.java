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
public class NamespaceIT {
  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Parameterized.Parameters(name = "{0} : jkube.namespace = {1}, jkube.enricher.jkube-namespace.namespace={2}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "default", "", "" },
        new Object[] { "set-namespace", "namespace-to-operate", ""},
        new Object[] { "create-namespace", "", "namespace-to-create-name" },
        new Object[] { "create-and-set-namespace", "namespace-to-create-and-operate", "namespace-to-create-and-operate"},
        new Object[] { "create-and-set-different-namespace", "namespace-to-operate", "namespace-to-create-name" }
    );
  }

  @Parameterized.Parameter
  public String expectedDirectory;

  @Parameterized.Parameter (1)
  public String namespaceViaJKubeNamespaceProperty;

  @Parameterized.Parameter (2)
  public String namespaceViaNamespaceEnricherConfig;


  @Test
  public void k8sResourceTask_whenRunWithConfiguredWithNamespace_generatesK8sManifestWithExpectedNamespace() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("namespace")
        .withArguments("-Pjkube.enricher.jkube-namespace.namespace=" + namespaceViaNamespaceEnricherConfig,
            "-Pjkube.namespace=" + namespaceViaJKubeNamespaceProperty,
            "k8sResource")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", expectedDirectory, "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Running in Kubernetes mode")
        .contains("Using resource templates from")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }

  @Test
  public void ocResourceTask_whenRunWithConfiguredWithNamespace_generatesOpenShiftManifestWithExpectedNamespace() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("namespace")
        .withArguments("-Pjkube.enricher.jkube-namespace.namespace=" + namespaceViaNamespaceEnricherConfig,
            "-Pjkube.namespace=" + namespaceViaJKubeNamespaceProperty,
            "ocResource")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", expectedDirectory, "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Running in OpenShift mode")
        .contains("Using resource templates from")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }
}
