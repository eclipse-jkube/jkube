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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.jkube.kit.common.util.Base64Util.decodeToString;
import static org.eclipse.jkube.kit.common.util.Base64Util.encodeToString;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.jkube.kit.common.ResourceVerify;
import org.gradle.testkit.runner.BuildResult;
import org.junit.Rule;
import org.junit.Test;

import net.minidev.json.parser.ParseException;

public class FileSecretIT {
  @Rule
  public final ITGradleRunner gradleRunner = new ITGradleRunner();

  @Test
  public void k8sResource_whenRun_generatesK8sSecret() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("secret-file").withArguments("k8sResource").build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
        adjustSecretEncodingOnWindows(gradleRunner.resolveFile("expected", "kubernetes.yml")));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }

  @Test
  public void ocResource_whenRun_generatesK8sSecret() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("secret-file").withArguments("ocResource").build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        adjustSecretEncodingOnWindows(gradleRunner.resolveFile("expected", "openshift.yml")));
    assertThat(result).extracting(BuildResult::getOutput).asString()
        .contains("Using resource templates from")
        .contains("Adding revision history limit to 2")
        .contains("validating");
  }

  /**
   * On Windows, the separator used across lines is "\r\n" in contrast to *nix system where it's "\n".
   * This function expects as input the file used for comparison in the tests and, if the test(s) is(are) running on
   * Windows, then re-encodes the secret in them so that the Base64 string is the same between the expected file and the
   * generated one.
   * This is necessary because the comparison file contains the Base64 string calculated from a *nix machine.
   *
   * @param expectedFile the file used for comparison
   * @return a file which has the same content as {@code expectedFile}, but with the secret re-encoded
   */
  private File adjustSecretEncodingOnWindows(File expectedFile) throws IOException {
    if (!SystemUtils.IS_OS_WINDOWS) {
      return expectedFile;
    }

    final String propertyHoldingTheData = "application.properties: ";
    final File patchedFile = gradleRunner.resolveFile("build", expectedFile.getName());
    try (final Stream<String> lines = Files.lines(expectedFile.toPath());
        final PrintWriter pw = new PrintWriter(Files.newBufferedWriter(patchedFile.toPath()))) {
      lines.forEach(l -> {
        String lineToWrite = l;
        if (l.contains(propertyHoldingTheData)) {
          final int dataPosition = l.indexOf(propertyHoldingTheData) + propertyHoldingTheData.length();
          final String data = l.substring(dataPosition);
          lineToWrite = l.substring(0, dataPosition)
              + encodeToString(decodeToString(data).replace("\n", System.lineSeparator()).getBytes());
        }

        pw.println(lineToWrite);
      });
    }

    return patchedFile;
  }
}
