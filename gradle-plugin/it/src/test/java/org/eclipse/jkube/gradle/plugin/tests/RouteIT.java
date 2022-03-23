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
public class RouteIT {
  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Parameterized.Parameters(name = "ocResource {0} with jkube.openshift.generateRoute={1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "routeDisabled", "false", 2},
        new Object[] { "routeEnabled", "true", 3}
    );
  }

  @Parameterized.Parameter
  public String expectedDir;

  @Parameterized.Parameter (1)
  public String routeEnabled;

  @Parameterized.Parameter (2)
  public int expectedGeneratedFiles;

  @Test
  public void ocResource_whenRunWithRouteFlag_generatesOpenShiftManifests() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("route")
        .withArguments("clean", "-Pjkube.openshift.generateRoute=" + routeEnabled, "ocResource", "--stacktrace")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", expectedDir, "openshift.yml"));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Running in OpenShift mode")
        .contains("Using resource templates from")
        .contains("Adding a default Deployment")
        .contains("Adding revision history limit to ")
        .contains("validating");
    assertThat(gradleRunner.resolveDefaultOpenShiftResourceDir().listFiles())
        .hasSize(expectedGeneratedFiles);
  }
}
