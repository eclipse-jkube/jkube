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
package org.eclipse.jkube.kit.build.service.docker;


import org.eclipse.jkube.kit.build.service.docker.helper.ConfigHelper;
import org.eclipse.jkube.kit.config.image.RunImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageConfigurationTest {

  @Test
  void initAndValidateWithBuildAndRun() {
     ConfigHelper.NameFormatter nameFormatter = mock(ConfigHelper.NameFormatter.class);
     BuildConfiguration buildConfiguration = mock(BuildConfiguration.class);
     RunImageConfiguration runImageConfiguration = mock(RunImageConfiguration.class);
    // Given
    final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .build(buildConfiguration)
        .run(runImageConfiguration)
        .build();
    when(buildConfiguration.initAndValidate()).thenReturn("1.337");
    when(runImageConfiguration.initAndValidate()).thenReturn("13.37");
    // When
    final String result = ConfigHelper.initAndValidate(nameFormatter, imageConfiguration);
    // Then
    assertThat(result).isEqualTo("13.37");
  }
}
