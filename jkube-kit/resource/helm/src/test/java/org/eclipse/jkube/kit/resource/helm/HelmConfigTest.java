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
import java.util.Arrays;
import java.util.List;

import org.eclipse.jkube.kit.resource.helm.HelmConfig.HelmType;

import io.fabric8.openshift.api.model.Template;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class HelmConfigTest {

  @Test
  public void helmTypeParseStringNullTest() {
    // When
    final List<HelmConfig.HelmType> result = HelmConfig.HelmType.parseString(null);
    // Then
    assertThat(result).isEmpty();
  }

  @Test
  public void helmTypeParseStringEmptyTest() {
    // When
    final List<HelmConfig.HelmType> result = HelmConfig.HelmType.parseString(" ");
    // Then
    assertThat(result).isEmpty();
  }

  @Test
  public void helmTypeParseStringEmptiesTest() {
    // When
    final List<HelmConfig.HelmType> result = HelmConfig.HelmType.parseString(",,  ,   ,, ");
    // Then
    assertThat(result).isEmpty();
  }

  @Test
  public void helmTypeParseStringKubernetesTest() {
    // When
    final List<HelmConfig.HelmType> result = HelmConfig.HelmType.parseString("kuBerNetes");
    // Then

    assertThat(result).containsOnly(HelmConfig.HelmType.KUBERNETES);
  }

  @Test(expected = IllegalArgumentException.class)
  public void helmTypeParseStringKubernetesInvalidTest() {
    // When
    HelmConfig.HelmType.parseString("OpenShift,Kuberentes");
    // Then > throw Exception
    fail();
  }

  @Test
  public void createHelmConfig() throws IOException {
    // Given
    File file = File.createTempFile("test", ".tmp");
    file.deleteOnExit();

    HelmConfig helmConfig = new HelmConfig();
    helmConfig.setVersion("version");
    helmConfig.setSecurity("security");
    helmConfig.setChart("chart");
    helmConfig.setTypes(Arrays.asList(HelmType.KUBERNETES));
    helmConfig.setType(HelmType.KUBERNETES.name());
    helmConfig.setAdditionalFiles(Arrays.asList(file));
    helmConfig.setChartExtension("chartExtension");
    helmConfig.setOutputDir("outputDir");
    helmConfig.setSourceDir("sourceDir");
    helmConfig.setSnapshotRepository(new HelmRepository());
    helmConfig.setStableRepository(new HelmRepository());
    helmConfig.setTarballOutputDir("tarballOutputDir");
    helmConfig.setTemplates(Arrays.asList(new Template()));
    helmConfig.setDescription("description");
    helmConfig.setHome("home");
    helmConfig.setIcon("icon");
    helmConfig.setMaintainers(Arrays.asList(new Maintainer()));
    helmConfig.setSources(Arrays.asList("source"));
    helmConfig.setEngine("engine");
    helmConfig.setKeywords(Arrays.asList("keyword"));

    helmConfig.setGeneratedChartListeners(Arrays.asList((helmConfig1, type, chartFile) -> {
    }));
    // Then
    assertThat(helmConfig.getVersion()).isEqualTo("version");
    assertThat(helmConfig.getSecurity()).isEqualTo("security");
    assertThat(helmConfig.getChart()).isEqualTo("chart");
    assertThat(helmConfig.getTypes().get(0)).isEqualTo(HelmType.KUBERNETES);
    assertThat(helmConfig.getAdditionalFiles().get(0)).exists();
    assertThat(helmConfig.getChartExtension()).isEqualTo("chartExtension");
    assertThat(helmConfig.getOutputDir()).isEqualTo("outputDir");
    assertThat(helmConfig.getSourceDir()).isEqualTo("sourceDir");
    assertThat(helmConfig.getSnapshotRepository()).isNotNull();
    assertThat(helmConfig.getStableRepository()).isNotNull();
    assertThat(helmConfig.getTarballOutputDir()).isEqualTo("tarballOutputDir");
    assertThat(helmConfig.getTemplates()).isNotEmpty();
    assertThat(helmConfig.getDescription()).isEqualTo("description");
    assertThat(helmConfig.getHome()).isEqualTo("home");
    assertThat(helmConfig.getIcon()).isEqualTo("icon");
    assertThat(helmConfig.getMaintainers()).isNotEmpty();
    assertThat(helmConfig.getSources().get(0)).isEqualTo("source");
    assertThat(helmConfig.getEngine()).isEqualTo("engine");
    assertThat(helmConfig.getKeywords().get(0)).isEqualTo("keyword");
  }
}
