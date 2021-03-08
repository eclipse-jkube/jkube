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
package org.eclipse.jkube.kit.resource.helm;

import java.io.File;
import java.io.IOException;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.ResourceUtil;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HelmServiceTest {

  @Mocked
  KitLogger kitLogger;
  @Mocked
  HelmConfig helmConfig;

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() {
    kitLogger = null;
    helmConfig = null;
  }

  @Test(expected = IOException.class)
  public void prepareSourceDirValidWithNoYamls(@Mocked File file) throws Exception {
    // Given
    new Expectations() {{
      file.isDirectory();
      result = true;
    }};
    // When
    HelmService.prepareSourceDir(helmConfig, HelmConfig.HelmType.OPENSHIFT);
  }

  @Test
  public void createChartYaml(@Mocked File outputDir, @Mocked ResourceUtil resourceUtil) throws Exception {
    // Given
    new Expectations() {{
      helmConfig.getChart();
      result = "Chart Name";
      helmConfig.getVersion();
      result = "1337";
    }};
    // When
    HelmService.createChartYaml(helmConfig, outputDir);
    // Then
    new Verifications() {{
      Chart chart;
      ResourceUtil.save(withNotNull(), chart = withCapture(), ResourceFileType.yaml);
      assertThat(chart)
          .hasFieldOrPropertyWithValue("apiVersion", "v1")
          .hasFieldOrPropertyWithValue("name", "Chart Name")
          .hasFieldOrPropertyWithValue("version", "1337");
    }};
  }
}
