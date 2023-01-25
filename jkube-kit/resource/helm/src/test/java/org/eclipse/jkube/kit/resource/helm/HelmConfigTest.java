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
import java.util.stream.Stream;

import io.fabric8.openshift.api.model.ParameterBuilder;
import org.eclipse.jkube.kit.common.Maintainer;
import org.eclipse.jkube.kit.resource.helm.HelmConfig.HelmType;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.openshift.api.model.Template;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HelmConfigTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  @AfterEach
  void tearDown() {
    objectMapper = null;
  }

  @DisplayName("Helm type string parse tests")
  @ParameterizedTest(name = "''{0}'' string should be empty")
  @MethodSource("parseStringTestData")
  void parseString(String testDesc, String types){
    // When
    final List<HelmConfig.HelmType> result = HelmConfig.HelmType.parseString(types);
    // Then
    assertThat(result).isEmpty();
  }

  public static Stream<Arguments> parseStringTestData() {
    return Stream.of(
            Arguments.of("null", null),
            Arguments.of("blank string", " "),
            Arguments.of("empty strings", ",,  ,   ,, ")
    );
  }

  @Test
  void helmTypeParseStringKubernetesTest() {
    // When
    final List<HelmConfig.HelmType> result = HelmConfig.HelmType.parseString("kuBerNetes");
    // Then

    assertThat(result).containsOnly(HelmConfig.HelmType.KUBERNETES);
  }

  @Test
  void helmTypeParseStringKubernetesInvalidTest() {
    // When & Then
    assertThrows(IllegalArgumentException.class, () -> HelmConfig.HelmType.parseString("OpenShift,Kuberentes"));
  }

  @Test
  void deserialize() throws Exception {
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
        "\"parameterTemplates\":[{}]," +
        "\"parameters\": [{\"name\":\"key\"}]," +
        "\"description\":\"The description\"," +
        "\"home\":\"e.t.\"," +
        "\"icon\":\"Warhol\"," +
        "\"maintainers\":[{}]," +
        "\"sources\":[\"source\"]," +
        "\"engine\":\"V8\"," +
        "\"keywords\":[\"SEO\"]," +
        "\"dependencies\":[{" +
        "\"name\": \"ngnix\"," +
        "\"version\": \"1.2.3\"," +
        "\"repository\": \"https://example.com/charts\"" +
        "}]" +
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
        .hasFieldOrPropertyWithValue("parameterTemplates", Collections.singletonList(new Template()))
        .hasFieldOrPropertyWithValue("parameters", Collections.singletonList(new ParameterBuilder().withName("key").build()))
        .hasFieldOrPropertyWithValue("description", "The description")
        .hasFieldOrPropertyWithValue("home", "e.t.")
        .hasFieldOrPropertyWithValue("icon", "Warhol")
        .hasFieldOrPropertyWithValue("maintainers", Collections.singletonList(new Maintainer()))
        .hasFieldOrPropertyWithValue("sources", Collections.singletonList("source"))
        .hasFieldOrPropertyWithValue("engine", "V8")
        .hasFieldOrPropertyWithValue("keywords", Collections.singletonList("SEO"))
        .hasFieldOrPropertyWithValue("dependencies", Collections.singletonList(new HelmDependency()
        .toBuilder().name("ngnix").version("1.2.3").repository("https://example.com/charts").build()));
  }

  @Test
  void createHelmConfig() throws IOException {
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
    helmConfig.setParameterTemplates(Collections.singletonList(new Template()));
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
    assertThat(helmConfig.getParameterTemplates()).isNotEmpty();
    assertThat(helmConfig.getDescription()).isEqualTo("description");
    assertThat(helmConfig.getHome()).isEqualTo("home");
    assertThat(helmConfig.getIcon()).isEqualTo("icon");
    assertThat(helmConfig.getMaintainers()).isNotEmpty();
    assertThat(helmConfig.getSources().get(0)).isEqualTo("source");
    assertThat(helmConfig.getEngine()).isEqualTo("engine");
    assertThat(helmConfig.getKeywords().get(0)).isEqualTo("keyword");
  }

  @Test
  void equals() {
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
        .parameterTemplates(Collections.singletonList(new Template()))
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
