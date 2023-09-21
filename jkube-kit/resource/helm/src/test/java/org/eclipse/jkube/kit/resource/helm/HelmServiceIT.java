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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.assertj.ArchiveAssertions;

import io.fabric8.openshift.api.model.ParameterBuilder;
import io.fabric8.openshift.api.model.Template;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.resource.ResourceServiceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HelmServiceIT {

  private HelmService helmService;
  private HelmConfig helmConfig;
  private File helmOutputDir;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws Exception {
    helmService = new HelmService(
      JKubeConfiguration.builder().project(JavaProject.builder().properties(new Properties()).build()).build(),
      new ResourceServiceConfig(),
      new KitLogger.SilentLogger());
    helmOutputDir = Files.createDirectory(temporaryFolder.resolve("helm-output")).toFile();
    helmConfig = new HelmConfig();
    helmConfig.setSourceDir(new File(HelmServiceIT.class.getResource("/it/sources").toURI()).getAbsolutePath());
    helmConfig.setOutputDir(helmOutputDir.getAbsolutePath());
    helmConfig.setTarballOutputDir(helmOutputDir.getAbsolutePath());
    helmConfig.setChartExtension("tar");
  }

  @Test
  void generateHelmChartsTest() throws Exception {
    // Given
    helmConfig.setApiVersion("v1");
    helmConfig.setChart("ITChart");
    helmConfig.setVersion("1.33.7");
    helmConfig.setTypes(Arrays.asList(HelmConfig.HelmType.OPENSHIFT, HelmConfig.HelmType.KUBERNETES));
    helmConfig.setAdditionalFiles(Collections.singletonList(
        new File(HelmServiceIT.class.getResource("/it/sources/additional-file.txt").toURI())
    ));
    helmConfig.setParameterTemplates(Collections.singletonList(
        Serialization.unmarshal(HelmServiceIT.class.getResource("/it/sources/global-template.yml"), Template.class)
    ));
    helmConfig.setParameters(Arrays.asList(
        new ParameterBuilder().withName("annotation_from_config").withValue("{{ .Chart.Name | upper }}").build(),
        new ParameterBuilder().withName("annotation.from.config.dotted").withValue("{{ .Chart.Name }}").build()));
    final AtomicInteger generatedChartCount = new AtomicInteger(0);
    helmConfig.setGeneratedChartListeners(Collections.singletonList(
        (helmConfig1, type, chartFile) -> generatedChartCount.incrementAndGet()));
    // When
    helmService.generateHelmCharts(helmConfig);
    // Then
    assertThat(new File(helmOutputDir, "kubernetes/Chart.yaml")).exists().isNotEmpty();
    assertThat(new File(helmOutputDir, "kubernetes/values.yaml")).exists().isNotEmpty();
    assertThat(new File(helmOutputDir, "kubernetes/additional-file.txt")).exists().isNotEmpty();
    assertThat(new File(helmOutputDir, "kubernetes/templates/kubernetes.yaml")).exists().isNotEmpty();
    assertThat(new File(helmOutputDir, "openshift/Chart.yaml")).exists().isNotEmpty();
    assertThat(new File(helmOutputDir, "openshift/values.yaml")).exists().isNotEmpty();
    assertThat(new File(helmOutputDir, "openshift/additional-file.txt")).exists().isNotEmpty();
    assertThat(new File(helmOutputDir, "openshift/templates/test-pod.yaml")).exists().isNotEmpty();
    assertThat(new File(helmOutputDir, "openshift/templates/openshift.yaml")).exists().isNotEmpty();
    ArchiveAssertions.assertThat(new File(helmOutputDir, "kubernetes/ITChart-1.33.7.tar"))
        .exists().isNotEmpty().isUncompressed().fileTree().containsExactlyInAnyOrder(
            "ITChart/additional-file.txt",
            "ITChart/templates/",
            "ITChart/templates/kubernetes.yaml",
            "ITChart/Chart.yaml",
            "ITChart/values.yaml");
    ArchiveAssertions.assertThat(new File(helmOutputDir, "openshift/ITChart-1.33.7.tar"))
        .exists().isNotEmpty().isUncompressed().fileTree().containsExactlyInAnyOrder(
            "ITChart/additional-file.txt",
            "ITChart/templates/",
            "ITChart/templates/openshift.yaml",
            "ITChart/templates/test-pod.yaml",
            "ITChart/Chart.yaml",
            "ITChart/values.yaml");
    assertYamls();
    assertThat(generatedChartCount).hasValue(2);
  }

  @Test
  void generateHelmChartsTest_withInvalidParameters_throwsException() {
    // Given
    helmConfig.setTypes(Collections.singletonList(HelmConfig.HelmType.KUBERNETES));
    helmConfig.setParameters(Collections.singletonList(new ParameterBuilder()
        .withValue("{{ .Chart.Name | upper }}").build()));
    // When
    final IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () ->
        helmService.generateHelmCharts(helmConfig));
    // Then
    assertThat(result).hasMessageStartingWith("Helm parameters must be declared with a valid name:");
  }

  @Test
  void generateHelmChartsTest_withParameterCasePreserved() throws Exception {
    // Given
    helmConfig.setTypes(Collections.singletonList(HelmConfig.HelmType.KUBERNETES));
    helmConfig.setPreserveParameterCase(true);
    helmConfig.setParameters(Collections.singletonList(new ParameterBuilder().withName("testCamelCase").withValue("testValue").build()));
    // When
    helmService.generateHelmCharts(helmConfig);
    // Then
    File valuesFile = new File(helmOutputDir, "kubernetes/values.yaml");
    assertThat(new File(helmOutputDir, "kubernetes/Chart.yaml")).exists().isNotEmpty();
    assertThat(valuesFile).exists().isNotEmpty();
    List<String> values = Files.readAllLines(valuesFile.toPath());
    // Contents should be
    // ---
    // testCamelCase: testValue
    assertThat(values).hasSize(2);
    String parameterLine = values.get(1);
    assertThat(parameterLine).isEqualTo("testCamelCase: testValue");
  }

  @SuppressWarnings("unchecked")
  private void assertYamls() throws Exception {
    final Path expectations = new File(HelmServiceIT.class.getResource("/it/expected").toURI()).toPath();
    final Path generatedYamls = helmOutputDir.toPath();
    for (Path expected : Files.walk(expectations).filter(Files::isRegularFile).collect(Collectors.toList())) {
      final Map<String, ?> expectedContent = Serialization.unmarshal(replacePlaceholders(expected), Map.class);
      final Map<String, ?> actualContent = Serialization.unmarshal(
        replacePlaceholders(generatedYamls.resolve(expectations.relativize(expected))), Map.class);
      assertThat(actualContent).isEqualTo(expectedContent);
    }
  }

  private static String replacePlaceholders(final Path yamlWithJsonPlaceholders) throws IOException {
    return FileUtils.readFileToString(yamlWithJsonPlaceholders.toFile(), StandardCharsets.UTF_8)
        .replace("\"{{\"{{\"}}", "").replace("{{\"}}\"}}", "")
        .replace("{", "(").replace("}", ")");
  }
}
