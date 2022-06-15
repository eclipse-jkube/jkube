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
package org.eclipse.jkube.kit.common;

import java.io.File;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.eclipse.jkube.kit.common.ResourceVerify.verifyResourceDescriptors;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceVerifyTest {

  @Test
  void verifyResourceDescriptors_withSameFile_shouldComplete() {
    assertThatCode(() -> verifyResourceDescriptors(file("original.yaml"), file("original.yaml")))
        .doesNotThrowAnyException();
  }

  @Test
  void verifyResourceDescriptors_withMatchingFile_shouldComplete() {
    assertThatCode(() -> verifyResourceDescriptors(file("original.yaml"), file("matching.yaml")))
        .doesNotThrowAnyException();
  }

  @Test
  void verifyResourceDescriptors_withPartiallyMatchingFile_shouldComplete() {
    assertThatCode(() -> verifyResourceDescriptors(file("original.yaml"), file("partially-matching.yaml")))
        .doesNotThrowAnyException();
  }

  @Test
  void verifyResourceDescriptors_withMatchingFileAndStrict_shouldComplete() {
    assertThatCode(() -> verifyResourceDescriptors(file("original.yaml"), file("matching.yaml"), true))
        .doesNotThrowAnyException();
  }

  @Test
  void verifyResourceDescriptors_withPartiallyMatchingFileAndStrict_shouldFail() {
    final File original = file("original.yaml");
    final File partiallyMatching = file("partially-matching.yaml");
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class,
        () -> verifyResourceDescriptors(original, partiallyMatching, true));
    assertThat(result).hasMessageContaining("JSONArray size mismatch for JSON entry 'items'");
  }

  private static File file(String path) {
    return new File(ResourceVerifyTest.class.getResource("/resource-verify/" + path).getFile());
  }
}