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
import java.util.Collections;
import java.util.List;

import org.eclipse.jkube.kit.resource.helm.HelmConfig.HelmType;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.openshift.api.model.Template;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class HelmConfigTest {

  private ObjectMapper objectMapper;

  @Before
  public void setUp() {
    objectMapper = new ObjectMapper();
  }

  @After
  public void tearDown() {
    objectMapper = null;
  }

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
  public void deserialize() throws Exception {
    // Given
    final String serializedChart = "{" +
        "\"version\":\"1337\"," +
        "\"security\":\"NYPD\"," +
        "\"chart\":\"pie\"," +
        "\"type\":\"kUberNetes\"," +
        "\"chartExtension\":\"tar\"," +
        "\"outputDir\":\"./output\"," +
        "\"sourceDir\":\"./src\"," +
        "\"snapshotRepository\":{}," +
        "\"stableRepository\":{}," +
        "\"tarballOutputDir\":\"./tar-output\"," +
        "\"templates\":[{}]," +
        "\"description\":\"The description\"," +
        "\"home\":\"e.t.\"," +
        "\"icon\":\"Warhol\"," +
        "\"maintainers\":[{}]," +
        "\"sources\":[\"source\"]," +
        "\"engine\":\"V8\"," +
        "\"keywords\":[\"SEO\"]" +
        "}";
    // When
    final HelmConfig result = objectMapper.readValue(serializedChart, HelmConfig.class);
    // Then
    assertThat(result)
        .hasFieldOrPropertyWithValue("version", "1337")
        .hasFieldOrPropertyWithValue("security", "NYPD")
        .hasFieldOrPropertyWithValue("chart", "pie")
        .hasFieldOrPropertyWithValue("types", Collections.singletonList(HelmType.KUBERNETES))
        .hasFieldOrPropertyWithValue("chartExtension", "tar")
        .hasFieldOrPropertyWithValue("outputDir", "./output")
        .hasFieldOrPropertyWithValue("sourceDir", "./src")
        .hasFieldOrPropertyWithValue("snapshotRepository", new HelmRepository())
        .hasFieldOrPropertyWithValue("stableRepository", new HelmRepository())
        .hasFieldOrPropertyWithValue("tarballOutputDir", "./tar-output")
        .hasFieldOrPropertyWithValue("templates", Collections.singletonList(new Template()))
        .hasFieldOrPropertyWithValue("description", "The description")
        .hasFieldOrPropertyWithValue("home", "e.t.")
        .hasFieldOrPropertyWithValue("icon", "Warhol")
        .hasFieldOrPropertyWithValue("maintainers", Collections.singletonList(new Maintainer()))
        .hasFieldOrPropertyWithValue("sources", Collections.singletonList("source"))
        .hasFieldOrPropertyWithValue("engine", "V8")
        .hasFieldOrPropertyWithValue("keywords", Collections.singletonList("SEO"));
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
    helmConfig.setTypes(Collections.singletonList(HelmType.KUBERNETES));
    helmConfig.setType(HelmType.KUBERNETES.name());
    helmConfig.setAdditionalFiles(Collections.singletonList(file));
    helmConfig.setChartExtension("chartExtension");
    helmConfig.setOutputDir("outputDir");
    helmConfig.setSourceDir("sourceDir");
    helmConfig.setSnapshotRepository(new HelmRepository());
    helmConfig.setStableRepository(new HelmRepository());
    helmConfig.setTarballOutputDir("tarballOutputDir");
    helmConfig.setTemplates(Collections.singletonList(new Template()));
    helmConfig.setDescription("description");
    helmConfig.setHome("home");
    helmConfig.setIcon("icon");
    helmConfig.setMaintainers(Collections.singletonList(new Maintainer()));
    helmConfig.setSources(Collections.singletonList("source"));
    helmConfig.setEngine("engine");
    helmConfig.setKeywords(Collections.singletonList("keyword"));

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

  @Test
  public void equals() {
    // Given
    final HelmConfig helmConfig1 = HelmConfig.builder()
        .version("1337")
        .security("NYPD")
        .chart("Chart")
        .types(Collections.singletonList(HelmType.KUBERNETES))
        .additionalFiles(Collections.emptyList())
        .chartExtension("tar")
        .outputDir("./output")
        .sourceDir("./source")
        .snapshotRepository(new HelmRepository())
        .stableRepository(new HelmRepository())
        .tarballOutputDir("tarballOutputDir")
        .templates(Collections.singletonList(new Template()))
        .description("description")
        .home("e.t.")
        .icon("Warhol")
        .maintainers(Collections.singletonList(new Maintainer()))
        .sources(Arrays.asList("source-1", "source-2"))
        .engine("V8")
        .keywords(Collections.singletonList("keyword"))
        .build();
    // When
    final HelmConfig result = helmConfig1.toBuilder().build();
    // Then
    assertThat(result)
        .isNotSameAs(helmConfig1)
        .isEqualTo(helmConfig1);
  }
}
