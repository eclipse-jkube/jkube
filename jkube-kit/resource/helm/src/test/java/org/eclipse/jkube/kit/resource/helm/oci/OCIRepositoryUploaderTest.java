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
package org.eclipse.jkube.kit.resource.helm.oci;

import org.eclipse.jkube.kit.resource.helm.HelmRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class OCIRepositoryUploaderTest {

  @Test
  void withMissingChart_throwsException(@TempDir Path tempDir) throws IOException {
    // Given
    final File chartFile = Files.createDirectory(tempDir.resolve("helm"))
      .resolve("missing-chart-0.0.1.tar.gz").toFile();
    final HelmRepository repository = HelmRepository.builder().build();
    // When + Then
    assertThatIllegalStateException()
        .isThrownBy(() -> new OCIRepositoryUploader().uploadSingle(chartFile, repository))
        .withMessageStartingWith("Could not find Chart.yaml file in ")
        .withMessageEndingWith("helm");
  }
}
