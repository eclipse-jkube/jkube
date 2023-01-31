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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.assertj.ArchiveAssertions;
import org.eclipse.jkube.kit.common.util.ResourceUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.openshift.api.model.ParameterBuilder;
import io.fabric8.openshift.api.model.Template;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HelmServiceIT {

  private ObjectMapper mapper;
  private HelmService helmService;
  private HelmConfig helmConfig;
  private File helmOutputDir;
  private static File helmK8sOutputDir;
  private static File helmOpenShiftOutputDir;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws Exception {
    mapper = new ObjectMapper(new YAMLFactory());
    helmService = new HelmService(new JKubeConfiguration(), new KitLogger.SilentLogger());
    helmOutputDir = Files.createDirectory(temporaryFolder.resolve("helm-output")).toFile();
    helmK8sOutputDir = Files.createDirectory(helmOutputDir.toPath().resolve("kubernetes")).toFile();
    helmOpenShiftOutputDir = Files.createDirectory(helmOutputDir.toPath().resolve("openshift")).toFile();
    helmConfig = new HelmConfig();
    helmConfig.setSourceDir(new File(HelmServiceIT.class.getResource("/it/sources").toURI()).getAbsolutePath());
    helmConfig.setOutputDir(helmOutputDir.getAbsolutePath());
    helmConfig.setTarballOutputDir(helmOutputDir.getAbsolutePath());
    helmConfig.setChartExtension("tar");
  }

  @Test
  void generateHelmCharts_whenKubernetesHelmTypeProvided_thenGeneratesChartForKubernetes() throws Exception {
    generateHelmChartsTest(HelmConfig.HelmType.KUBERNETES, helmK8sOutputDir,
        "ITChart/additional-file.txt",
        "ITChart/templates/",
        "ITChart/templates/kubernetes.yaml",
        "ITChart/Chart.yaml",
        "ITChart/values.yaml");
  }

  @Test
  void generateHelmCharts_whenOpenShiftHelmTypeProvided_thenGeneratesChartForOpenShift() throws Exception {
    generateHelmChartsTest(HelmConfig.HelmType.OPENSHIFT, helmOpenShiftOutputDir,
        "ITChart/additional-file.txt",
        "ITChart/templates/",
        "ITChart/templates/openshift.yaml",
        "ITChart/templates/test-pod.yaml",
        "ITChart/Chart.yaml",
        "ITChart/values.yaml");
  }

  void generateHelmChartsTest(HelmConfig.HelmType helmType, File tarballOutputDir, String... tarballContents) throws Exception {
    // Given
    helmConfig.setChart("ITChart");
    helmConfig.setVersion("1.33.7");
    helmConfig.setTypes(Collections.singletonList(helmType));
    helmConfig.setTarballOutputDir(tarballOutputDir.getAbsolutePath());
    helmConfig.setAdditionalFiles(Collections.singletonList(
        new File(HelmServiceIT.class.getResource("/it/sources/additional-file.txt").toURI())
    ));
    helmConfig.setParameterTemplates(Collections.singletonList(
        ResourceUtil.load(new File(HelmServiceIT.class.getResource("/it/sources/global-template.yml").toURI()), Template.class)
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
    assertThat(new File(helmOutputDir, String.format("%s/Chart.yaml", helmType.getOutputDir()))).exists().isNotEmpty();
    assertThat(new File(helmOutputDir, String.format("%s/values.yaml", helmType.getOutputDir()))).exists().isNotEmpty();
    assertThat(new File(helmOutputDir, String.format("%s/additional-file.txt", helmType.getOutputDir()))).exists().isNotEmpty();
    assertThat(new File(helmOutputDir, String.format("%s/templates/%s.yaml", helmType.getOutputDir(), helmType.getOutputDir()))).exists().isNotEmpty();
    ArchiveAssertions.assertThat(new File(tarballOutputDir, "ITChart-1.33.7.tar"))
        .exists().isNotEmpty().isUncompressed().fileTree().containsExactlyInAnyOrder(tarballContents);
    assertYamls(tarballOutputDir, helmType);
    assertThat(generatedChartCount).hasValue(1);
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

  @SuppressWarnings("unchecked")
  private void assertYamls(File outputDir, HelmConfig.HelmType helmType) throws Exception {
    final Path expectations = new File(HelmServiceIT.class.getResource("/it/expected/" + helmType.getOutputDir()).toURI()).toPath();
    final Path generatedYamls = outputDir.toPath();
    for (Path expected : Files.walk(expectations).filter(Files::isRegularFile).collect(Collectors.toList())) {
      final Map<String, ?> expectedContent = mapper.readValue(replacePlaceholders(expected), Map.class);
      final Map<String, ?> actualContent =
          mapper.readValue(replacePlaceholders(generatedYamls.resolve(expectations.relativize(expected))), Map.class);
      assertThat(actualContent).isEqualTo(expectedContent);
    }
  }

  private static String replacePlaceholders(final Path yamlWithJsonPlaceholders) throws IOException {
    return FileUtils.readFileToString(yamlWithJsonPlaceholders.toFile(), StandardCharsets.UTF_8)
        .replace("\"{{\"{{\"}}", "").replace("{{\"}}\"}}", "")
        .replace("{", "(").replace("}", ")");
  }
}
