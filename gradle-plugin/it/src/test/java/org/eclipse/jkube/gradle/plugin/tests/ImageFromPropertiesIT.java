/*
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

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ImageFromPropertiesIT {
  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @Test
  void k8sBuild_generatesConfiguredImage() throws IOException {
    // When
    final BuildResult result = gradleRunner.withITProject("image-from-properties")
      .withArguments("clean", "build", "k8sBuild", "--stacktrace")
      .build();
    // Then
    final File dockerFile = gradleRunner.resolveFile("build", "docker", "repository", "image-from-properties", "latest", "build", "Dockerfile");
    assertThat(new String(Files.readAllBytes(dockerFile.toPath())))
      .contains("FROM quay.io/quay/busybox")
      .contains("ENV JAVA_OPTIONS=-Xmx256m")
      .contains("LABEL MAINTAINER=\"JKube testing team\"")
      .contains("EXPOSE 8080")
      .contains("COPY /jkube-generated-layer-final-artifact/deployments /deployments/")
      .contains("ENTRYPOINT java -jar /app.jar");
    assertThat(result).extracting(BuildResult::getOutput).asString()
      .contains("Building container image in Kubernetes mode")
      .contains("JIB image build started");
  }
}
