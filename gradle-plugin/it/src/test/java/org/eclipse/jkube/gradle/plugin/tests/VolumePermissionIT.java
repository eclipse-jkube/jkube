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
public class VolumePermissionIT {
  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Parameterized.Parameters(name = "{0} : jkube.enricher.jkube-volume-permission.defaultStorageClass={1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "default", ""},
        new Object[] { "custom-storageclass", "cheese"}
    );
  }

  @Parameterized.Parameter
  public String expectedDirectory;

  @Parameterized.Parameter (1)
  public String defaultStorageClassEnricherConfig;

  @Test
  public void k8sResourceTask_whenRun_generatesK8sManifestWithPersistentVolume() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("volume-permission")
        .withArguments("-Pjkube.enricher.jkube-volume-permission.defaultStorageClass=" + defaultStorageClassEnricherConfig,
            "k8sResource")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        gradleRunner.resolveFile("expected", expectedDirectory, "kubernetes.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }

  @Test
  public void ocResourceTask_whenRun_generatesOpenShiftManifestWithPersistentVolume() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("volume-permission")
        .withArguments("-Pjkube.enricher.jkube-volume-permission.defaultStorageClass=" + defaultStorageClassEnricherConfig,
            "ocResource")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", expectedDirectory, "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }
}
