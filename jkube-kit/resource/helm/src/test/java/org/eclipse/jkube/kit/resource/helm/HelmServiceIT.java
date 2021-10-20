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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ResourceUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.openshift.api.model.Template;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.eclipse.jkube.kit.common.assertj.ArchiveAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public class HelmServiceIT {

  @Test
  public void generateHelmChartsTest() throws Exception {
    // Given
    JavaProject javaProject = JavaProject.builder()
      .properties(new Properties())
      .maintainers(Collections.emptyList())
      .baseDirectory(new File("."))
      .buildDirectory(new File("target"))
      .build();
    File manifest = new File("target/classes/META-INF/jkube/kubernetes.yml");
    File templateDir = new File("target/jkube/helm");
    HelmConfig helmConfig = new HelmConfig();
    helmConfig.setChart("ITChart");
    helmConfig.setVersion("1337");
    helmConfig.setSourceDir(new File(HelmServiceIT.class.getResource("/it/sources").toURI()).getAbsolutePath());
    helmConfig.setOutputDir("target/helm-it");
    helmConfig.setTarballOutputDir("target/helm-it");
    helmConfig.setChartExtension("tar");
    helmConfig.setAdditionalFiles(Collections.singletonList(
        new File(HelmServiceIT.class.getResource("/it/sources/additional-file.txt").toURI())
    ));
    helmConfig.setTemplates(Collections.singletonList(
        ResourceUtil.load(new File(HelmServiceIT.class.getResource("/it/sources/global-template.yml").toURI()), Template.class)
    ));
    final AtomicInteger generatedChartCount = new AtomicInteger(0);
    List<GeneratedChartListener> generatedChartListeners = Collections.singletonList(
      (helmConfig1, type, chartFile) -> generatedChartCount.incrementAndGet());
    final KitLogger logger = new KitLogger.StdoutLogger();
    // When
    helmConfig = HelmServiceUtil.initHelmConfig(HelmConfig.HelmType.KUBERNETES, javaProject, manifest, templateDir, helmConfig, generatedChartListeners);
    HelmService.generateHelmCharts(logger, helmConfig);
    helmConfig = HelmServiceUtil.initHelmConfig(HelmConfig.HelmType.OPENSHIFT, javaProject, manifest, templateDir, helmConfig, generatedChartListeners);
    HelmService.generateHelmCharts(logger, helmConfig);
    // Then
    assertThat(new File("target/helm-it/kubernetes/Chart.yaml")).exists().isNotEmpty();
    assertThat(new File("target/helm-it/kubernetes/values.yaml")).exists().isNotEmpty();
    assertThat(new File("target/helm-it/kubernetes/additional-file.txt")).exists().isNotEmpty();
    assertThat(new File("target/helm-it/kubernetes/templates/kubernetes.yaml")).exists().isNotEmpty();
    assertThat(new File("target/helm-it/openshift/Chart.yaml")).exists().isNotEmpty();
    assertThat(new File("target/helm-it/openshift/values.yaml")).exists().isNotEmpty();
    assertThat(new File("target/helm-it/openshift/additional-file.txt")).exists().isNotEmpty();
    assertThat(new File("target/helm-it/openshift/templates/test-pod.yaml")).exists().isNotEmpty();
    assertThat(new File("target/helm-it/openshift/templates/openshift.yaml")).exists().isNotEmpty();
    ArchiveAssertions.assertThat(new File("target/helm-it/ITChart-1337-helm.tar"))
        .exists().isNotEmpty().isUncompressed().fileTree().containsExactlyInAnyOrder(
            "ITChart/additional-file.txt",
            "ITChart/templates/",
            "ITChart/templates/kubernetes.yaml",
            "ITChart/Chart.yaml",
            "ITChart/values.yaml");
    ArchiveAssertions.assertThat(new File("target/helm-it/ITChart-1337-helmshift.tar"))
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

  private static void assertYamls() throws Exception {
    final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    final Path expectations = new File(HelmServiceIT.class.getResource("/it/expected").toURI()).toPath();
    final Path generatedYamls = new File("target/helm-it").toPath();
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
