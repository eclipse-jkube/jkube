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
package org.eclipse.jkube.kit.resource.helm;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class HelmUploaderManagerTest {
  private static Stream<Arguments> testData() {
    return Stream.of(
        Arguments.of("ARTIFACTORY", ArtifactoryHelmRepositoryUploader.class),
        Arguments.of("NEXUS", NexusHelmRepositoryUploader.class),
        Arguments.of("CHARTMUSEUM", ChartMuseumHelmRepositoryUploader.class),
        Arguments.of("OCI", OCIRepositoryUploader.class)
    );
  }

  @ParameterizedTest
  @MethodSource("testData")
  void getHelmUploader_whenValidHelmTypeProvided_thenReturnAppropriateUploader(HelmRepository.HelmRepoType type, Class<? extends HelmUploader> helmUploaderType) {
    // Given
    HelmUploaderManager helmUploaderManager = new HelmUploaderManager();

    // When
    HelmUploader helmUploader = helmUploaderManager.getHelmUploader(type);

    // Then
    assertThat(helmUploader)
        .isInstanceOf(helmUploaderType);
  }
}
