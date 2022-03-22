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

import mockit.Expectations;
import org.eclipse.jkube.kit.build.service.docker.helper.ConfigHelper;
import org.eclipse.jkube.kit.config.image.RunImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import mockit.Mocked;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ImageConfigurationTest {

  @Test
  public void initAndValidateWithBuildAndRun(
      @Mocked ConfigHelper.NameFormatter nameFormatter, @Mocked  BuildConfiguration buildConfiguration,
      @Mocked RunImageConfiguration runImageConfiguration) {

    // Given
    final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .build(buildConfiguration)
        .run(runImageConfiguration)
        .build();
    // @formatter:off
    new Expectations() {{
      buildConfiguration.initAndValidate(); result = "1.337";
      runImageConfiguration.initAndValidate(); result = "13.37";
    }};
    // @formatter:on
    // When
    final String result = ConfigHelper.initAndValidate(nameFormatter, imageConfiguration);
    // Then
    assertThat(result).isEqualTo("13.37");
  }
}
