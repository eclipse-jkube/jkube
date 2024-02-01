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
package org.eclipse.jkube.kit.config.image;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * This test has been added to ensure ImageName's upstream compatibility with OCI Registry Reference.
 * Tests ported from <a href="https://github.com/oras-project/oras-go/blob/3b1dd0e9700082fe7bc690a745263c06361417dd/registry/reference_test.go">ORAS Reference</a>
 */
class ImageNameORASReferenceTest {
  @ParameterizedTest
  @ValueSource(strings = {
      "localhost/hello-world@sha256:b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      "localhost/hello-world:v1",
      "localhost/hello-world",
      "registry.example.com/hello-world@sha256:b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      "registry.example.com/hello-world:v1",
      "registry.example.com/hello-world",
      "localhost:5000/hello-world@sha256:b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      "localhost:5000/hello-world:v1",
      "localhost:5000/hello-world",
      "127.0.0.1:5000/hello-world@sha256:b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      "127.0.0.1:5000/hello-world:v1",
      "127.0.0.1:5000/hello-world",
      "localhost/hello-world:v2@sha256:b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      "registry.example.com/hello-world:v2@sha256:b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      "localhost:5000/hello-world:v2@sha256:b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      "127.0.0.1:5000/hello-world:v2@sha256:b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
      "[::1]:5000/hello-world:v1",
      //"registry.example.com/hello-world:@sha256:b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", // https://github.com/eclipse/jkube/issues/2545
  })
  void validImageNamesCompatibleWithAll(String name) {
    // Given + When
    ImageName imageName = new ImageName(name);

    // Then
    assertThat(imageName).isNotNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "localhost/UPPERCASE/test", // Invalid repo name
      "localhost:v1/hello-world", // Invalid port
      "registry.example.com/hello-world:foobar:sha256:b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", // Invalid Digest prefix: colon instead of the at sign
      "registry.example.com/hello-world@@sha256:b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", // Invalid Digest prefix: double at sign
      "registry.example.com/hello-world @sha256:b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", // Invalid Digest prefix: space
      //"registry.example.com/foobar@sha256:b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde" // https://github.com/eclipse/jkube/issues/2543
  })
  void invalidImageNames(String name) {
    assertThatIllegalArgumentException().isThrownBy(() -> new ImageName(name));
  }
}
