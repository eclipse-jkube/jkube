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

import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageConfigurationTest {

  @Test
  void testBuilder() {
    // Given
    BuildConfiguration jkubeBuildConfiguration = BuildConfiguration.builder()
      .user("super-user")
      .build();
    // When
    final ImageConfiguration result = ImageConfiguration.builder()
      .name("1337")
      .propertyResolverPrefix("app.images.image-1")
      .build(jkubeBuildConfiguration)
      .build();
    // Then
    assertThat(result)
      .hasFieldOrPropertyWithValue("name", "1337")
      .hasFieldOrPropertyWithValue("propertyResolverPrefix", "app.images.image-1")
      .extracting(ImageConfiguration::getBuildConfiguration)
      .hasFieldOrPropertyWithValue("user", "super-user");
  }
}
