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
package org.eclipse.jkube.kit.build.service.docker;

import org.eclipse.jkube.kit.config.image.build.ImagePullPolicy;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ImagePullManagerTest {

  @ParameterizedTest(name = "With ''{0}'' imagePullPolicy and ''{1}'' autoPull mode imagePullPolicy should be ''{2}''")
  @MethodSource("createImagePullManagerTestData")
  void createImagePullManager(String imagePullPolicy, String autoPull, ImagePullPolicy expectedImagePullPolicy) {
    // Given & When
    ImagePullManager imagePullManager = ImagePullManager.createImagePullManager(imagePullPolicy, autoPull, new Properties());
    // Then
    assertThat(imagePullManager)
        .hasFieldOrPropertyWithValue("imagePullPolicy", expectedImagePullPolicy)
        .extracting("cacheStore").isNotNull();
  }

  public static Stream<Arguments> createImagePullManagerTestData() {
    return Stream.of(
            Arguments.of("Always", null, ImagePullPolicy.Always),
            Arguments.of(null, "always", ImagePullPolicy.Always),
            Arguments.of(null, "off", ImagePullPolicy.Never),
            Arguments.of(null, "always", ImagePullPolicy.Always),
            Arguments.of(null, null, ImagePullPolicy.IfNotPresent)
    );
  }
}