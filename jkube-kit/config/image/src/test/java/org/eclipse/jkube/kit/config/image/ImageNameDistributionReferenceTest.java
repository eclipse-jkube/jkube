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
 * This test has been added to ensure ImageName's upstream compatibility with Docker Distribution Reference.
 * Tests ported from <a href="https://github.com/distribution/reference/blob/8507c7fcf0da9f570540c958ea7b972c30eeaeca/reference_test.go">Distribution Reference</a>
 */
class ImageNameDistributionReferenceTest {

  @ParameterizedTest
  @ValueSource(strings = {
      "test.com/foo",
      "test:8080/foo",
      "docker.io/library/foo",
      "test_com",
      "test.com:tag",
      "test.com:5000",
      "test.com/repo:tag",
      "test:5000/repo",
      "test:5000/repo:tag",
      "lowercase:Uppercase",
      "test:5000/repo@sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
      "test:5000/repo:tag@sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
      "sub-dom1.foo.com/bar/baz/quux",
      "sub-dom1.foo.com/bar/baz/quux:some-long-tag",
      "b.gcr.io/test.example.com/my-app:test.example.com",
      "xn--n3h.com/myimage:xn--n3h.com",
      "xn--7o8h.com/myimage:xn--7o8h.com@sha512:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
      "foo_bar.com:8080",
      "foo/foo_bar.com:8080",
      "192.168.1.1",
      "192.168.1.1:tag",
      "192.168.1.1:5000",
      "192.168.1.1/repo",
      "192.168.1.1:5000/repo",
      "192.168.1.1:5000/repo:5050",
      "a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a:tag-puts-this-over-max",
      "docker/docker",
      "library/debian",
      "debian",
      "localhost/library/debian",
      "localhost/debian",
      "docker.io/docker/docker",
      "docker.io/library/debian",
      "docker.io/debian",
      "index.docker.io/docker/docker",
      "index.docker.io/library/debian",
      "index.docker.io/debian",
      "127.0.0.1:5000/docker/docker",
      "127.0.0.1:5000/library/debian",
      "127.0.0.1:5000/debian",
      "192.168.0.1",
      "192.168.0.1:80",
      "192.168.0.1:8/debian",
      "192.168.0.2:25000/debian",
      "docker.io/1a3f5e7d9c1b3a5f7e9d1c3b5a7f9e1d3c5b7a9f1e3d5d7c9b1a3f5e7d9c1b3a",
      "[2001:db8::1]/repo",
      "[2001:db8:1:2:3:4:5:6]/repo:tag",
      "[2001:db8::1]:5000/repo",
      "[2001:db8::1]:5000/repo:tag",
      "[2001:db8::1]:5000/repo@sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
      "[2001:db8::1]:5000/repo:tag@sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
      "[2001:db8::]:5000/repo",
      "[::1]:5000/repo",
  })
  void validNames(String name) {
    // Given
    ImageName imageName = new ImageName(name);

    // When + Then
    assertThat(imageName).isNotNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "",
      ":justtag",
      "@sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
      "a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a:tag",
      //"repo@sha256:ffffffffffffffffffffffffffffffffff", // https://github.com/eclipse/jkube/issues/2543
      "validname@invaliddigest:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
      "Uppercase:tag",
      "test:5000/Uppercase/lowercase:tag",
      "aa/asdf$$^/aa",
      //"[fe80::1%eth0]:5000/repo", // https://github.com/eclipse/jkube/issues/2541
      //"[fe80::1%@invalidzone]:5000/repo", // https://github.com/eclipse/jkube/issues/2541
  })
  void invalidNames(String name) {
    assertThatIllegalArgumentException().isThrownBy(() -> new ImageName(name));
  }
}
