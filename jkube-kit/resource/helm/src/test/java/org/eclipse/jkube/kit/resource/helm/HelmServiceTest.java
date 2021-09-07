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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.resource.helm.HelmConfig.HelmType;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

  @Test
  public void createChartYamlWithDependencies(@Mocked File outputDir, @Mocked ResourceUtil resourceUtil) throws Exception {
    // Given
    HelmDependency helmDependency = new HelmDependency()
        .toBuilder()
        .name("nginx")
        .version("1.2.3.")
        .repository("repository")
        .build();
    new Expectations() {{
      helmConfig.getChart();
      result = "Chart Name";
      helmConfig.getVersion();
      result = "1337";
      helmConfig.getDependencies();
      result = Arrays.asList(helmDependency);
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
          .hasFieldOrPropertyWithValue("version", "1337")
          .hasFieldOrPropertyWithValue("dependencies",
              Collections.singletonList(helmDependency));
    }};
  }

  @Test
  public void uploadChart(
      @Mocked HelmUploader helmUploader,
      @Mocked HelmRepository helmRepository)
      throws IOException, BadUploadException {
    //Given
    new Expectations() {{
      helmConfig.getTypes();
      result = Arrays.asList(HelmType.KUBERNETES);
      helmConfig.getChart();
      result = "chartName";
      helmConfig.getVersion();
      result = "1337";
      helmConfig.getChartExtension();
      result = "tar.gz";
      helmConfig.getOutputDir();
      result = "target";
      helmConfig.getTarballOutputDir();
      result = "target";
      helmRepository.getName();
      result = "Artifactory";
      helmUploader.uploadSingle(withInstanceOf(File.class), helmRepository);
    }};
    // When
    HelmService.uploadHelmChart(kitLogger, helmConfig, helmRepository);
    // Then
    new Verifications() {{
      String fileName = "chartName-1337-helm.tar.gz";
      File file;
      helmUploader.uploadSingle(file = withCapture(), helmRepository);
      assertThat(file)
          .hasName(fileName);
    }};
  }
}
