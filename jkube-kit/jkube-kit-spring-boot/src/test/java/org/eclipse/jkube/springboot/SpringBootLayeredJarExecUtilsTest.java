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
package org.eclipse.jkube.springboot;

import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class SpringBootLayeredJarExecUtilsTest {
  @TempDir
  private File temporaryFolder;

  private KitLogger kitLogger;

  @BeforeEach
  void setup() {
    kitLogger = new KitLogger.SilentLogger();
  }

  @Test
  void listLayers_whenJarInvalid_thenThrowException() {
    // Given
    File layeredJar = new File(temporaryFolder, "sample.jar");

    // When + Then
    assertThatIllegalStateException()
        .isThrownBy(() -> SpringBootLayeredJarExecUtils.listLayers(kitLogger, layeredJar))
        .withMessage("Failure in getting spring boot jar layers information");
  }

  @Test
  void extractLayers_whenJarInvalid_thenThrowException() {
    // Given
    File layeredJar = new File(temporaryFolder, "sample.jar");

    // When + Then
    assertThatIllegalStateException()
        .isThrownBy(() -> SpringBootLayeredJarExecUtils.extractLayers(kitLogger, temporaryFolder, layeredJar))
        .withMessage("Failure in extracting spring boot jar layers");
  }
}
